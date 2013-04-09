package edu.nd.nina.snap.agm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.imageio.ImageIO;

import com.panayotis.gnuplot.JavaPlot;
import com.panayotis.gnuplot.plot.AbstractPlot;
import com.panayotis.gnuplot.style.PlotStyle;
import com.panayotis.gnuplot.style.Style;
import com.panayotis.gnuplot.terminal.ImageTerminal;
import com.panayotis.gnuplot.terminal.PostscriptTerminal;

import edu.nd.nina.UndirectedGraph;
import edu.nd.nina.graph.DefaultEdge;
import edu.nd.nina.math.LinearAlgebra;
import edu.nd.nina.math.Randoms;
import edu.nd.nina.structs.Pair;

public class AGMFit {
	/** Graph to fit. */
	UndirectedGraph<Integer, DefaultEdge> g;
	/** Community ID -> Member Node ID Sets. */
	Vector<Set<Integer>> cIDNSetV;
	/** Edge -> Shared Community ID Set. */
	Hashtable<Pair<Integer, Integer>, Set<Integer>> edgeComVH;
	/** Node ID -> Communities IDs the node belongs to. */
	Hashtable<Integer, Set<Integer>> nIDComVH;
	/** The number of edges in each community. */
	Vector<Integer> comEdgesV;
	/**
	 * Probability of edge when two nodes share no community (epsilon in the
	 * paper).
	 */
	Float pNoCom;
	/**
	 * Parametrization of P_c (edge probability in community c), P_c = 1 -
	 * exp(-lambda).
	 */
	Vector<Float> lambdaV;
	/***/
	Randoms rnd;
	/** <Node ID, Community ID> pairs (for sampling MCMC moves). */
	Hashtable<Pair<Integer, Integer>, Float> nIDCIDPrH;
	/** <Node ID, Community ID> pairs (for sampling MCMC moves). */
	Hashtable<Pair<Integer, Integer>, Integer> nIDCIDPrS;
	/** Minimum value of regularization parameter lambda (default = 1e-5). */
	Float minLambda;
	/** Maximum value of regularization parameter lambda (default = 10). */
	Float maxLambda;
	/**
	 * Regularization parameter when we fit for P_c (for finding # communities).
	 */
	Float regCoef;
	/**
	 * ID of the Epsilon-community (in case we fit P_c of the epsilon
	 * community). We do not fit
	 */
	Integer baseCID;

	AGMFit() { }
	

	
	AGMFit(final UndirectedGraph<Integer, DefaultEdge> _g, final Integer _initComs, final Integer _rndSeed){
		g = _g;
		pNoCom = 0.0f;
		rnd = new Randoms(_rndSeed);
		minLambda = 0.00001f;
		maxLambda = 10.0f; 
		regCoef = 0f;
		baseCID = -1;		
		neighborComInit(_initComs); 
		//RandomInitCmtyVV(InitComs);  
	}
	
	public AGMFit(UndirectedGraph<Integer, DefaultEdge> _g,
			Vector<Vector<Integer>> _cmtyVVPt, Randoms _rnd) {
		g = _g;
		pNoCom = 0.0f;
		rnd = _rnd;
		minLambda = 0.00001f;
		maxLambda = 10.0f; 
		regCoef = 0f;
		baseCID = -1;		
		setCmtyVV(_cmtyVVPt);
	}

	private void setCmtyVV(Vector<Vector<Integer>> cmtyVV) {
		cIDNSetV = new Vector<Set<Integer>>(cmtyVV.size());
		for (int i = 0; i < cmtyVV.size(); i++)
			cIDNSetV.add(new HashSet<Integer>());

		for (int c = 0; c < cIDNSetV.size(); c++) {
			cIDNSetV.get(c).addAll(cmtyVV.get(c));
			// check whether the member nodes exist in the graph
			for (int j = 0; j < cmtyVV.get(c).size(); j++) {
				assert (g.containsVertex(cmtyVV.get(c).get(j)));
			}
		}
		initNodeData();
		setDefaultPNoCom();
	}

	private void neighborComInit(Integer _initComs) {

		cIDNSetV = new Vector<Set<Integer>>(_initComs);
		for(int i=0; i<_initComs; i++) cIDNSetV.add(new HashSet<Integer>());
		final int edges = g.edgeSet().size();
		Vector<Pair<Float, Integer>> nIdPhiV = new Vector<Pair<Float, Integer>>(
				g.vertexSet().size());
		Set<Integer> invalidNIDS = new HashSet<Integer>(g.vertexSet().size());
		Vector<Integer> chosenNIDV = new Vector<Integer>(_initComs); // FOR
																		// DEBUG
		Long exeTime = System.currentTimeMillis();
		// compute conductance of neighborhood community

		for (Integer n : g.vertexSet()) {
			Set<Integer> nBCmty;
			double phi;
			if (g.degreeOf(n) < 5) { // do not include nodes with too few degree
				phi = 1.0;
			} else {
				nBCmty = AGMUtil.getNbhCom(g, n);
				assert (nBCmty.size() == g.degreeOf(n) + 1);
				phi = AGMUtil.getConductance(g, nBCmty, edges);
			}
			nIdPhiV.add(new Pair<Float, Integer>((float) phi, n));
		}
		Collections.sort(nIdPhiV);
		Collections.reverse(nIdPhiV);
		
		
		System.out.println("conductance computation completed "+(System.currentTimeMillis() - exeTime));
		System.out.flush();
		//choose nodes with local minimum in conductance

		int curCID = 0;
		for (int ui = 0; ui < nIdPhiV.size(); ui++) {
			int UID = nIdPhiV.get(ui).p2;
		    if (invalidNIDS.contains(UID)) { continue; }
		    chosenNIDV.add(UID); //FOR DEBUG
		    //add the node and its neighbors to the current community
		    cIDNSetV.get(curCID).add(UID);
		    
		    for (DefaultEdge e : g.edgesOf(UID)) {
				if (g.getEdgeSource(e) == UID) {
					cIDNSetV.get(curCID).add(g.getEdgeTarget(e));
					invalidNIDS.add(g.getEdgeTarget(e));
				} else {
					cIDNSetV.get(curCID).add(g.getEdgeSource(e));
					invalidNIDS.add(g.getEdgeTarget(e));
				}
			}

		    curCID++;
		    
		    if (curCID >= _initComs) { break;  }
		  }
		  if (_initComs > curCID) {
		    System.out.println((_initComs - curCID) + " communities needed to fill randomly");
		  }
		  //assign a member to zero-member community (if any)
		  for (int c = 0; c < cIDNSetV.size(); c++) {
		    if (cIDNSetV.get(c).size() == 0) {
		      int comSz = 10;
		      for (int u = 0; u < comSz; u++) {
		        int UID = g.randomVertex(rnd);
		        cIDNSetV.get(c).add(UID);
		      }
		    }
		  }
		  initNodeData();
		  setDefaultPNoCom();
	}


	//AGMFit(final UndirectedGraph<Integer, DefaultEdge> g, const TVec<TIntV>& CmtyVVPt, const TRnd& RndPt): G(GraphPt), PNoCom(0.0), Rnd(RndPt), MinLambda(0.00001), MaxLambda(10.0), RegCoef(0), BaseCID(-1) { SetCmtyVV(CmtyVVPt); }

	/** Set epsilon by the default value.*/
	private void setDefaultPNoCom() {
		pNoCom = (float) (1.0d / (double) g.vertexSet().size() / (double) g.vertexSet().size());
	}


	private void initNodeData() {
		nIDComVH = new Hashtable<Integer, Set<Integer>>();
		for(int i=0; i<g.vertexSet().size(); i++){
			Set<Integer> x = new HashSet<Integer>();
			nIDComVH.put(i, x);
		}
		AGMUtil.getNodeMembership(nIDComVH, cIDNSetV);
		getEdgeJointCom();
		lambdaV = new Vector<Float>(cIDNSetV.size());
		for(int i=0; i<cIDNSetV.size(); i++){
			lambdaV.add(0f);
		}
		for (int c = 0; c < cIDNSetV.size(); c++) {
			int MaxE = (cIDNSetV.get(c).size()) * (cIDNSetV.get(c).size() - 1)
					/ 2;
			if (MaxE < 2) {
				lambdaV.set(c, maxLambda);
			} else {
				lambdaV.set(
						c,
						(float) -Math.log((double) (MaxE - comEdgesV.get(c))
								/ (float) MaxE));
			}
			if (lambdaV.get(c) > maxLambda) {
				lambdaV.set(c, maxLambda);
			}
			if (lambdaV.get(c) < minLambda) {
				lambdaV.set(c, minLambda);
			}
		}
		nIDCIDPrS = new Hashtable<Pair<Integer, Integer>, Integer>();
		for (int c = 0; c < cIDNSetV.size(); c++) {
			for (Integer SI : cIDNSetV.get(c)) {
				nIDCIDPrS.put(new Pair<Integer, Integer>(SI, c), 0);
			}
		}
	}


	/**
	 * For each (u, v) in edges, precompute C_uv (the set of communities u and v
	 * share).
	 */
	private void getEdgeJointCom() {
		comEdgesV = new Vector<Integer>();
		for (int i = 0; i < cIDNSetV.size(); i++) {
			comEdgesV.add(0);
		}
		edgeComVH = new Hashtable<Pair<Integer, Integer>, Set<Integer>>();
		for (DefaultEdge e : g.edgeSet()) {
			Integer srcNID = g.getEdgeSource(e);
			Integer dstNID = g.getEdgeTarget(e);

			if (srcNID >= dstNID) {
				continue;
			}

			assert (nIDComVH.containsKey(srcNID));
			assert (nIDComVH.containsKey(dstNID));
			Set<Integer> jointCom = AGMUtil.getIntersection(
					nIDComVH.get(srcNID), nIDComVH.get(dstNID));
			//assert(!jointCom.isEmpty());
			edgeComVH.put(new Pair<Integer, Integer>(srcNID, dstNID), jointCom);
			for (Integer k : jointCom) {
				comEdgesV.set(k,
						comEdgesV.get(k) + 1);
			}
		}

		assert (edgeComVH.size() == g.edgeSet().size());
	}
	
	public void setPNumCom(final Float epsilon) {
		if (baseCID == -1 && epsilon > 0.0) {
			pNoCom = epsilon;
		}
	}
	
	private void runMCMC(int maxIter, int i) {
		runMCMC(maxIter, i, "");
	}

	
	public void runMCMC(Integer maxIter, int evalLambdaIter, String plotFPrf) {
		Long IterTm = 0l, TotalTm = 0l;
		double prevL = likelihood();
		double[] deltaL = { 0d };
		double bestL = prevL;
		System.out.printf("initial likelihood = %f\n", prevL);
		Vector<Pair<Integer, Float>> IterTrueLV = new Vector<Pair<Integer, Float>>();
		Vector<Pair<Integer, Float>> IterJoinV = new Vector<Pair<Integer, Float>>();
		Vector<Pair<Integer, Float>> IterLeaveV = new Vector<Pair<Integer, Float>>();
		Vector<Pair<Integer, Float>> IterAcceptV = new Vector<Pair<Integer, Float>>();
		Vector<Pair<Integer, Float>> IterSwitchV = new Vector<Pair<Integer, Float>>();
		Vector<Pair<Integer, Float>> IterLBV = new Vector<Pair<Integer, Float>>();
		Vector<Pair<Integer, Integer>> IterTotMemV;
		Vector<Integer> IterV;
		Vector<Float> BestLV = new Vector<Float>();
		Vector<Set<Integer>> BestCmtySetV = new Vector<Set<Integer>>();
		int SwitchCnt = 0, LeaveCnt = 0, JoinCnt = 0, AcceptCnt = 0, ProbBinSz;
		Long PlotTm = System.currentTimeMillis();
		ProbBinSz = Math.max(1000, g.vertexSet().size() / 10); // bin to compute
																// probabilities
		IterLBV.add(new Pair<Integer, Float>(1, (float) bestL));

		for (int iter = 0; iter < maxIter; iter++) {
			IterTm++;
			int[] NID = { -1 };
			int[] JoinCID = { -1 }, LeaveCID = { -1 };
			sampleTransition(NID, JoinCID, LeaveCID, deltaL); // sample a move
			double OptL = prevL;
			// if it is accepted
			if (deltaL[0] > 0 || rnd.GetUniDev() < Math.exp(deltaL[0])) {
				IterTm++;
				if (LeaveCID[0] > -1 && LeaveCID[0] != baseCID) {
					leaveCom(NID[0], LeaveCID[0]);

				}
				if (JoinCID[0] > -1 && JoinCID[0] != baseCID) {
					joinCom(NID[0], JoinCID[0]);
				}
				if (LeaveCID[0] > -1 && JoinCID[0] > -1
						&& JoinCID[0] != baseCID && LeaveCID[0] != baseCID) {
					SwitchCnt++;
				} else if (LeaveCID[0] > -1 && LeaveCID[0] != baseCID) {
					LeaveCnt++;
				} else if (JoinCID[0] > -1 && JoinCID[0] != baseCID) {
					JoinCnt++;
				}
				AcceptCnt++;
				if ((iter + 1) % evalLambdaIter == 0) {
					IterTm++;
					MLEGradAscentGivenCAG(0.01, 3);
					OptL = likelihood();
				} else {
					OptL = prevL + deltaL[0];
				}
				if (bestL <= OptL && cIDNSetV.size() > 0) {
					BestCmtySetV = cIDNSetV;
					BestLV = lambdaV;
					bestL = OptL;
				}
			}
			if (iter > 0 && (iter % ProbBinSz == 0) && plotFPrf.length() > 0) {
				IterLBV.add(new Pair<Integer, Float>(iter, (float) OptL));
				IterSwitchV.add(new Pair<Integer, Float>(iter,
						(float) SwitchCnt / (float) AcceptCnt));
				IterLeaveV.add(new Pair<Integer, Float>(iter, (float) LeaveCnt
						/ (float) AcceptCnt));
				IterJoinV.add(new Pair<Integer, Float>(iter, (float) JoinCnt
						/ (float) AcceptCnt));
				IterAcceptV.add(new Pair<Integer, Float>(iter,
						(float) AcceptCnt / (float) ProbBinSz));
				SwitchCnt = JoinCnt = LeaveCnt = AcceptCnt = 0;
			}
			prevL = OptL;
			if ((iter + 1) % 10000 == 0) {
				System.out.printf("\r%d iterations completed [%.2f]", iter,
						(double) iter / (double) maxIter);
			}
		}

		// plot the likelihood and acceptance probabilities if the plot file
		// name is given
		if (plotFPrf.length() > 0) {

			ImageTerminal png = new ImageTerminal();
			File file = new File("." + System.getProperty("file.separator")
					+ "data" + System.getProperty("file.separator") + plotFPrf
					+ System.getProperty("file.separator") + plotFPrf
					+ "_likelihood.png");

			file.getParentFile().mkdirs();

			JavaPlot GP1 = new JavaPlot();
			GP1.setTerminal(png);
			double[][] zz = new double[IterLBV.size()][2];
			for(int i = 0; i<IterLBV.size(); i++){
				zz[i][0] = IterLBV.get(i).p1;
				zz[i][1] = IterLBV.get(i).p2;
			}
			GP1.addPlot(zz);
			GP1.getAxis("x").setLabel("iterations");

			// GP1.setGNUPlotPath(plotFPrf + ".likelihood.tab");
			((AbstractPlot) GP1.getPlots().get(0)).getPlotStyle().setStyle(
					Style.LINESPOINTS);

			((AbstractPlot) GP1.getPlots().get(0)).setTitle("likelihood");
			String titleStr = String.format(" N:%d E:%d", g.vertexSet().size(),
					g.edgeSet().size());
			GP1.setTitle(plotFPrf + ".likelihood" + titleStr);
			GP1.plot();
			
			try {
				ImageIO.write(png.getImage(), "png", file);
			} catch (IOException ex) {
				System.err.print(ex);
			}

			JavaPlot GP2 = new JavaPlot();
			png = new ImageTerminal();
			file = new File("." + System.getProperty("file.separator") + "data"
					+ System.getProperty("file.separator") + plotFPrf
					+ System.getProperty("file.separator") + plotFPrf
					+ "_transition.png");

			file.getParentFile().mkdirs();

			GP2.setTerminal(png);
			zz = new double[IterSwitchV.size()][2];
			for (int i = 0; i < IterSwitchV.size(); i++) {
				zz[i][0] = IterSwitchV.get(i).p1;
				zz[i][1] = IterSwitchV.get(i).p2;
			}
			GP2.addPlot(zz);
			
			zz = new double[IterLeaveV.size()][2];
			for(int i = 0; i<IterLeaveV.size(); i++){
				zz[i][0] = IterLeaveV.get(i).p1;
				zz[i][1] = IterLeaveV.get(i).p2;
			}
			GP2.addPlot(zz);
			

			zz = new double[IterJoinV.size()][2];
			for(int i = 0; i<IterJoinV.size(); i++){
				zz[i][0] = IterJoinV.get(i).p1;
				zz[i][1] = IterJoinV.get(i).p2;
			}
			GP2.addPlot(zz);			
			
			zz = new double[IterAcceptV.size()][2];
			for(int i = 0; i<IterAcceptV.size(); i++){
				zz[i][0] = IterAcceptV.get(i).p1;
				zz[i][1] = IterAcceptV.get(i).p2;
			}
			GP2.addPlot(zz);
			GP2.getAxis("x").setLabel("Iterations");
			((AbstractPlot) GP2.getPlots().get(0)).setTitle("Switch");
			((AbstractPlot) GP2.getPlots().get(1)).setTitle("Leave");
			((AbstractPlot) GP2.getPlots().get(2)).setTitle("Join");
			((AbstractPlot) GP2.getPlots().get(3)).setTitle("Accept");
			
			
			((AbstractPlot) GP2.getPlots().get(0)).getPlotStyle().setStyle(
					Style.LINESPOINTS);
			((AbstractPlot) GP2.getPlots().get(1)).getPlotStyle().setStyle(
					Style.LINESPOINTS);
			((AbstractPlot) GP2.getPlots().get(2)).getPlotStyle().setStyle(
					Style.LINESPOINTS);
			((AbstractPlot) GP2.getPlots().get(3)).getPlotStyle().setStyle(
					Style.LINESPOINTS);
			GP2.setTitle(plotFPrf + ".transition");

			// GP2.setGNUPlotPath(plotFPrf + "transition_prob.tab");
			GP2.plot();

			try {
				ImageIO.write(png.getImage(), "png", file);
			} catch (IOException ex) {
		        System.err.print(ex);
		    }
		}

		cIDNSetV = BestCmtySetV;
		lambdaV = BestLV;

		initNodeData();
		MLEGradAscentGivenCAG(0.001, 100);
		System.out.printf("\nMCMC completed (best likelihood: %.2f) [%s]\n",
				bestL, System.currentTimeMillis() - TotalTm);
	}
	
	/**
	 * Gradient descent for p_c while fixing community affiliation graph (CAG).
	 * 
	 * @param Thres
	 * @param MaxIter
	 * @return 
	 */
	int MLEGradAscentGivenCAG(final double Thres, final int MaxIter) {
		return MLEGradAscentGivenCAG(Thres, MaxIter, new String());
	}

	private int MLEGradAscentGivenCAG(final double Thres, final int MaxIter,
			final String PlotNm) {
		int Edges = g.edgeSet().size();
		Long ExeTm = System.currentTimeMillis();
		Vector<Float>[] GradV = new Vector[1];
		int iter = 0;
		Vector<Pair<Integer, Float>> IterLV = new Vector<Pair<Integer, Float>>(), IterGradNormV = new Vector<Pair<Integer, Float>>();
		double GradCutOff = 1000;
		for (iter = 0; iter < MaxIter; iter++) {
			gradLogLForLambda(GradV); // if gradient is going out of the
										// boundary, cut off
			for (int i = 0; i < lambdaV.size(); i++) {
				if (GradV[0].get(i) < -GradCutOff) {
					GradV[0].set(i, (float) -GradCutOff);
				}
				if (GradV[0].get(i) > GradCutOff) {
					GradV[0].set(i, (float) GradCutOff);
				}
				if (lambdaV.get(i) <= minLambda && GradV[0].get(i) < 0) {
					GradV[0].set(i, 0.0f);
				}
				if (lambdaV.get(i) >= maxLambda && GradV[0].get(i) > 0) {
					GradV[0].set(i, 0.0f);
				}
			}
			double Alpha = 0.15, Beta = 0.2;
			if (Edges > 1024 * 100) {
				Alpha = 0.00015;
				Beta = 0.3;
			}
			double LearnRate = getStepSizeByLineSearchForLambda(GradV[0], GradV[0],
					Alpha, Beta);
			if (LinearAlgebra.norm(GradV[0]) < Thres) {
				break;
			}
			for (int i = 0; i < lambdaV.size(); i++) {
				double Change = LearnRate * GradV[0].get(i);
				lambdaV.set(i, (float) (lambdaV.get(i) + Change));
				if (lambdaV.get(i) < minLambda) {
					lambdaV.set(i, minLambda);
				}
				if (lambdaV.get(i) > maxLambda) {
					lambdaV.set(i, maxLambda);
				}
			}
			if (!PlotNm.isEmpty()) {
				double L = likelihood();
				IterLV.add(new Pair<Integer, Float>(iter, (float) L));
				IterGradNormV.add(new Pair<Integer, Float>(iter,
						(float) LinearAlgebra.norm(GradV[0])));
			}
		}
		if (!PlotNm.isEmpty()) {
			// TGnuPlot::PlotValV(IterLV, PlotNm + ".likelihood_Q");
			// TGnuPlot::PlotValV(IterGradNormV, PlotNm + ".gradnorm_Q");
			System.out.printf(
					"MLE for Lambda completed with %d iterations(%s)\n", iter,
					System.currentTimeMillis() - ExeTm);
		}
		return iter;
	}

	/**
	 * Step size search for updating P_c (which is parametarized by lambda).
	 * 
	 * @param DeltaV
	 * @param GradV
	 * @param Alpha
	 * @param Beta
	 * @return
	 */
	private double getStepSizeByLineSearchForLambda(final Vector<Float> DeltaV,
			final Vector<Float> GradV, final double Alpha, final double Beta) {
		double StepSize = 1.0;
		double InitLikelihood = likelihood();
		assert (lambdaV.size() == DeltaV.size());
		Vector<Float> NewLambdaV = new Vector<Float>(lambdaV.size());
		for(int i=0; i<lambdaV.size(); i++) NewLambdaV.add(0f);
		for (int iter = 0;; iter++) {
			for (int i = 0; i < lambdaV.size(); i++) {
				NewLambdaV.set(i,
						(float) (lambdaV.get(i) + StepSize * DeltaV.get(i)));
				if (NewLambdaV.get(i) < minLambda) {
					NewLambdaV.set(i, minLambda);
				}
				if (NewLambdaV.get(i) > maxLambda) {
					NewLambdaV.set(i, maxLambda);
				}
			}
			if (likelihood(NewLambdaV) < InitLikelihood + Alpha * StepSize
					* LinearAlgebra.dotProduct(GradV, DeltaV)) {
				StepSize *= Beta;
			} else {
				break;
			}
		}
		return StepSize;
	}


	/**
	 * Gradient of likelihood for P_c.
	 * 
	 * @param gradV
	 */
	private void gradLogLForLambda(Vector<Float>[] gradV) {
		gradV[0] = new Vector<Float>(lambdaV.size());
		for(int i=0; i<lambdaV.size(); i++){
			gradV[0].add(0f);
		}
		Vector<Float> SumEdgeProbsV = new Vector<Float>(lambdaV.size());
		for(int i=0; i<lambdaV.size(); i++){
			SumEdgeProbsV.add(0f);
		}
		for (Set<Integer> JointCom : edgeComVH.values()) {			
			double LambdaSum = selectLambdaSum(JointCom);
			double Puv = 1 - Math.exp(-LambdaSum);
			if (JointCom.size() == 0) {
				Puv = pNoCom;
			}
			for (Integer si : JointCom) {
				SumEdgeProbsV.set(si,
						(float) (SumEdgeProbsV.get(si) + (1 - Puv) / Puv));
			}
		}
		for (int k = 0; k < lambdaV.size(); k++) {
			int MaxEk = cIDNSetV.get(k).size() * (cIDNSetV.get(k).size() - 1)
					/ 2;
			int NotEdgesInCom = MaxEk - comEdgesV.get(k);
			gradV[0].set(k,
					(float) (SumEdgeProbsV.get(k) - (double) NotEdgesInCom));
			if (lambdaV.get(k) > 0.0 && regCoef > 0.0) { // if regularization
															// exists
				gradV[0].set(k, gradV[0].get(k) - regCoef);
			}
		}
	}

	


	/**
	 * After MCMC, NID leaves community CID.
	 *  
	 * @param NID
	 * @param CID
	 */
	void leaveCom(final int NID, final int CID) {
		for (DefaultEdge e : g.edgesOf(NID)) {
			int VID;
			if (g.getEdgeSource(e) == NID) {
				VID = g.getEdgeTarget(e);
			} else {
				VID = g.getEdgeSource(e);
			}
			if (nIDComVH.get(VID).contains(CID)) {
				Pair<Integer, Integer> SrcDstNIDPr = new Pair<Integer, Integer>(
						Math.min(NID, VID), Math.max(NID, VID));
				edgeComVH.get(SrcDstNIDPr).remove(CID);
				comEdgesV.set(CID, comEdgesV.get(CID) - 1);
			}
		}
		cIDNSetV.get(CID).remove(NID);
		nIDComVH.get(NID).remove(CID);
		nIDCIDPrS.remove(new Pair<Integer, Integer>(NID, CID));
	}

	/**
	 * After MCMC, NID joins community CID.
	 * 
	 * @param NID
	 * @param JoinCID
	 */
	void joinCom(final int NID, final int JoinCID) {
		for (DefaultEdge e : g.edgesOf(NID)) {
			int VID;
			if (g.getEdgeSource(e) == NID) {
				VID = g.getEdgeTarget(e);
			} else {
				VID = g.getEdgeSource(e);
			}
			if (nIDComVH.get(VID).contains(JoinCID)) {
				Pair<Integer, Integer> SrcDstNIDPr = new Pair<Integer, Integer>(
						Math.min(NID, VID), Math.max(NID, VID));
				edgeComVH.get(SrcDstNIDPr).add(JoinCID);
				comEdgesV.set(JoinCID, comEdgesV.get(JoinCID) + 1);
			}
		}
		cIDNSetV.get(JoinCID).add(NID);
		nIDComVH.get(NID).add(JoinCID);
		nIDCIDPrS.put(new Pair<Integer, Integer>(NID, JoinCID), 0);
	}

	/**
	 * Sample transition: Choose among (join, leave, switch), and then sample
	 * (NID, CID).
	 * 
	 * @param nID
	 * @param joinCID
	 * @param leaveCID
	 * @param deltaL
	 */
	private void sampleTransition(int[] nID, int[] joinCID, int[] leaveCID,
			double[] deltaL) {
		int Option = rnd.GetUniDevInt(3); // 0:Join 1:Leave 2:Switch
		// if there is only one node membership, only join is possible.
		if (nIDCIDPrS.size() <= 1) {
			Option = 0;
		}
		int TryCnt = 0;
		int MaxTryCnt = g.vertexSet().size();
		deltaL[0] = Float.MIN_VALUE;
		if (Option == 0) {
			do {
				joinCID[0] = rnd.GetUniDevInt(cIDNSetV.size());
				nID[0] = g.randomVertex(rnd);
			} while (TryCnt++ < MaxTryCnt
					&& nIDCIDPrS.containsKey(new Pair<Integer, Integer>(nID[0],
							joinCID[0])));
			if (TryCnt < MaxTryCnt) { // if successfully find a move
				deltaL[0] = seekJoin(nID[0], joinCID[0]);
			}
		} else if (Option == 1) {
			do {
				Pair<Integer, Integer> NIDCIDPr = null;
				int stop = rnd.next(nIDCIDPrS.size()), i = 0;
				for (Pair<Integer, Integer> x : nIDCIDPrS.keySet()) {
					NIDCIDPr = x;
					i++;
					if (i >= stop)
						break;
				}
				nID[0] = NIDCIDPr.p1;
				leaveCID[0] = NIDCIDPr.p2;
			} while (TryCnt++ < MaxTryCnt && leaveCID[0] == baseCID);
			if (TryCnt < MaxTryCnt) {// if successfully find a move
				deltaL[0] = seekLeave(nID[0], leaveCID[0]);
			}
		} else {
			do {
				Pair<Integer, Integer> NIDCIDPr = null;
				int stop = rnd.next(nIDCIDPrS.size()), i = 0;
				for (Pair<Integer, Integer> x : nIDCIDPrS.keySet()) {
					NIDCIDPr = x;
					i++;
					if (i >= stop)
						break;
				}
				nID[0] = NIDCIDPr.p1;
				leaveCID[0] = NIDCIDPr.p2;
			} while (TryCnt++ < MaxTryCnt
					&& (nIDComVH.get(nID[0]).size() == cIDNSetV.size() || leaveCID[0] == baseCID));
			do {
				joinCID[0] = rnd.GetUniDevInt(cIDNSetV.size());
			} while (TryCnt++ < g.vertexSet().size()
					&& nIDCIDPrS.containsKey(new Pair<Integer, Integer>(nID[0],
							joinCID[0])));
			if (TryCnt < MaxTryCnt) {// if successfully find a move
				deltaL[0] = seekSwitch(nID[0], leaveCID[0], joinCID[0]);
			}
		}
	}

	/**
	 * Compute the change in likelihood (Delta) if node UID switches from CurCID
	 * to NewCID.
	 * 
	 * @param UID
	 * @param curCID
	 * @param newCID
	 * @return
	 */
	private double seekSwitch(int UID, int curCID, int newCID) {
		assert (!cIDNSetV.get(newCID).contains(UID));
		assert (cIDNSetV.get(curCID).contains(UID));
		double Delta = seekJoin(UID, newCID) + seekLeave(UID, curCID);
		assert(!Double.isNaN(Delta));
		// correct only for intersection between new com and current com

		for (DefaultEdge e : g.edgesOf(UID)) {
			int VID;
			if (g.getEdgeSource(e) == UID) {
				VID = g.getEdgeTarget(e);
			} else {
				VID = g.getEdgeSource(e);
			}
			if (!nIDComVH.get(VID).contains(curCID)
					|| !nIDComVH.get(VID).contains(newCID)) {
				continue;
			}
			Pair<Integer, Integer> SrcDstNIDPr = new Pair<Integer, Integer>(
					Math.min(UID, VID), Math.max(UID, VID));
			Set<Integer> JointCom = edgeComVH.get(SrcDstNIDPr);
			if(JointCom.isEmpty()) {
				System.out.println(SrcDstNIDPr);
			}
			double CurPuv, NewPuvAfterJoin, NewPuvAfterLeave, NewPuvAfterSwitch;
			double LambdaSum = selectLambdaSum(JointCom);
			CurPuv = 1 - Math.exp(-LambdaSum);
			NewPuvAfterLeave = 1 - Math.exp(-LambdaSum + lambdaV.get(curCID));
			NewPuvAfterJoin = 1 - Math.exp(-LambdaSum - lambdaV.get(newCID));
			NewPuvAfterSwitch = 1 - Math.exp(-LambdaSum - lambdaV.get(newCID)
					+ lambdaV.get(curCID));
			if (JointCom.size() == 1 || NewPuvAfterLeave == 0.0) {
				NewPuvAfterLeave = pNoCom;
			}
			assert(!Double.isNaN(Delta));
			Delta += (Math.log(NewPuvAfterSwitch) + Math.log(CurPuv)
					- Math.log(NewPuvAfterLeave) - Math.log(NewPuvAfterJoin));
			if (Double.isNaN(Delta)) {
				System.out.printf("NS:%f C:%f NL:%f NJ:%f pNoCom:%f",
						NewPuvAfterSwitch, CurPuv, NewPuvAfterLeave,
						NewPuvAfterJoin, pNoCom);
			}
			assert (!Double.isNaN(Delta));
		}
		return Delta;
	}


	/**
	 * Compute the change in likelihood (Delta) if node UID leaves community
	 * CID.
	 * 
	 * @param UID
	 * @param CID
	 * @return
	 */
	double seekLeave(int UID, int CID) {
		assert (cIDNSetV.get(CID).contains(UID));
		
		assert (g.containsVertex(UID));
		double Delta = 0.0;
		int NbhsInC = 0;
		for (DefaultEdge e : g.edgesOf(UID)) {
			int VID;
			if (g.getEdgeSource(e) == UID) {
				VID = g.getEdgeTarget(e);
			} else {
				VID = g.getEdgeSource(e);
			}
			if (!nIDComVH.get(VID).contains(CID)) {
				continue;
			}
			Pair<Integer, Integer> SrcDstNIDPr = new Pair<Integer, Integer>(
					Math.min(UID, VID), Math.max(UID, VID));
			Set<Integer> JointCom = edgeComVH.get(SrcDstNIDPr);

			double CurPuv, NewPuv, LambdaSum = selectLambdaSum(JointCom);
			CurPuv = 1 - Math.exp(-LambdaSum);
			NewPuv = 1 - Math.exp(-LambdaSum + lambdaV.get(CID));
			assert (JointCom.size() > 0);
			if (JointCom.size() == 1) {
				NewPuv = pNoCom;
			}
			Delta += (Math.log(NewPuv) - Math.log(CurPuv));
			assert (!Double.isNaN(Delta));
			NbhsInC++;
		}
		Delta += lambdaV.get(CID) * (cIDNSetV.get(CID).size() - 1 - NbhsInC);

		return Delta;
	}


	/**
	 * // Compute the change in likelihood (Delta) if node UID joins community
	 * CID.
	 * 
	 * @param UID
	 * @param CID
	 * @return
	 */
	private double seekJoin(int UID, int CID) {
		assert (!cIDNSetV.get(CID).contains(UID));
		double Delta = 0.0;
		int NbhsInC = 0;
		for (DefaultEdge e : g.edgesOf(UID)) {
			int VID;
			if (g.getEdgeSource(e) == UID) {
				VID = g.getEdgeTarget(e);
			} else {
				VID = g.getEdgeSource(e);
			}

			if (!nIDComVH.get(VID).contains(CID)) {
				continue;
			}
			Pair<Integer, Integer> SrcDstNIDPr = new Pair<Integer, Integer>(
					Math.min(UID, VID), Math.max(UID, VID));
			Set<Integer> jointCom = edgeComVH.get(SrcDstNIDPr);
			double CurPuv, NewPuv, LambdaSum = selectLambdaSum(jointCom);
			CurPuv = 1 - Math.exp(-LambdaSum);
			if (jointCom.size() == 0) {
				CurPuv = pNoCom;
			}
			NewPuv = 1 - Math.exp(-LambdaSum - lambdaV.get(CID));
			Delta += (Math.log(NewPuv) - Math.log(CurPuv));
			assert (!Double.isNaN(Delta));
			NbhsInC++;
		}
		Delta -= lambdaV.get(CID) * (cIDNSetV.get(CID).size() - NbhsInC);
		assert(!Double.isNaN(Delta));
		return Delta;
	}


	double likelihood() {
		return likelihood(lambdaV);
	}

	private double likelihood(final Vector<Float> newLambdaV) {
		double tmp1 = 0, tmp2 = 0;
		return likelihood(newLambdaV, tmp1, tmp2);
	}

	private double likelihood(Vector<Float> newLambdaV, double lEdges,
			double lNoEdges) {
		assert (cIDNSetV.size() == newLambdaV.size());
		assert (comEdgesV.size() == cIDNSetV.size());
		lEdges = 0.0;
		lNoEdges = 0.0;
		for (Set<Integer> jointCom : edgeComVH.values()) {			
			double lambdaSum = selectLambdaSum(newLambdaV, jointCom);
			double puv = 1 - Math.exp(-lambdaSum);
			if (jointCom.size() == 0) {
				puv = pNoCom;
			}
			assert (!Double.isNaN(Math.log(puv)));
			lEdges += Math.log(puv);
		}
		for (int k = 0; k < newLambdaV.size(); k++) {
			int maxEk = cIDNSetV.get(k).size() * (cIDNSetV.get(k).size() - 1)
					/ 2;
			int notEdgesInCom = maxEk - comEdgesV.get(k);
			if (notEdgesInCom > 0) {
				if (lNoEdges >= -Double.MAX_VALUE + (double) notEdgesInCom
						* newLambdaV.get(k)) {
					lNoEdges -= (double) notEdgesInCom * newLambdaV.get(k);
				}
			}
		}
		double lReg = 0.0;
		if (regCoef > 0.0) {
			lReg = -regCoef * LinearAlgebra.sumVec(newLambdaV);
		}
		return lEdges + lNoEdges + lReg;
	}

	/**
	 * Compute sum of \c lambda_c (which is log (1 - \c p_c)) over \c C_uv (
	 * <tt>ComK</tt>). The function is used to compute edge probability \c P_uv.
	 * 
	 * @param newLambdaV
	 * @param jointCom
	 * @return
	 */
	private double selectLambdaSum(Vector<Float> newLambdaV, Set<Integer> comK) {
		double result = 0.0;
		for (Integer si : comK) {
			assert (newLambdaV.get(si) >= 0);
			result += newLambdaV.get(si);
		}
		return result;
	}
	
	private double selectLambdaSum(Set<Integer> jointCom) {
		return selectLambdaSum(lambdaV, jointCom);
	}

	public void setRegCoef(double _regCoef) {
		regCoef = (float) _regCoef;
	}
	
	/**
	 * Get communities whose p_c is higher than 1 - QMax.
	 * 
	 * @param CmtyVV
	 * @param QMax
	 */
	public Vector<Vector<Integer>> getCmtyVV(final double QMax) {
		Vector<Float> TmpQV = new Vector<Float>();
		return getCmtyVV(TmpQV, QMax);
	}

	public Vector<Vector<Integer>> getCmtyVV(Vector<Float> QV,
			final double QMax) {
		Vector<Vector<Integer>> CmtyVV = new Vector<Vector<Integer>>();
		for (int i = 0; i < cIDNSetV.size(); i++){
			QV.add(0f);
			CmtyVV.add(new Vector<Integer>());
		}

		Hashtable<Integer, Float> CIDLambdaH = new Hashtable<Integer, Float>(
				cIDNSetV.size());
		for (int c = 0; c < cIDNSetV.size(); c++) {
			CIDLambdaH.put(c, lambdaV.get(c));
		}
		sortByValue(CIDLambdaH);
		for (Integer CID : CIDLambdaH.keySet()) {

			assert (lambdaV.get(CID) >= minLambda);
			double Q = Math.exp(-(double) lambdaV.get(CID));
			if (Q > QMax) {
				continue;
			}
			Vector<Integer> CmtyV = new Vector<Integer>();
			CmtyV.addAll(cIDNSetV.get(CID));

			if (CmtyV.size() == 0) {
				continue;
			}
			if (CID == baseCID) { // if the community is the base
									// community(epsilon community), discard
				assert (CmtyV.size() == g.vertexSet().size());
			} else {
				CmtyVV.add(CmtyV);
				QV.add((float) Q);
			}
		}
		return CmtyVV;
	}

	public static void sortByValue(Hashtable<?, Float> t) {

		// Transfer as List and sort it
		ArrayList<Map.Entry<?, Float>> l = new ArrayList<Map.Entry<?, Float>>(
				t.entrySet());
		Collections.sort(l, new Comparator<Map.Entry<?, Float>>() {

			public int compare(Map.Entry<?, Float> o1,
					Map.Entry<?, Float> o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		});
	}

	private void printSummary() {
		Hashtable<Integer, Float> CIDLambdaH = new Hashtable<Integer, Float>(
				cIDNSetV.size());
		for (int c = 0; c < cIDNSetV.size(); c++) {
			CIDLambdaH.put(c, lambdaV.get(c));
		}
		sortByValue(CIDLambdaH);
		int Coms = 0;
		for (int i = 0; i < lambdaV.size(); i++) {
			int CID = CIDLambdaH.get(i).intValue();
			if (lambdaV.get(CID) <= 0.0001) {
				continue;
			}
			System.out.printf(
					"P_c : %.3f Com Sz: %d, Total Edges inside: %d \n",
					1.0 - Math.exp(-lambdaV.get(CID)),
					cIDNSetV.get(CID).size(), (int) comEdgesV.get(CID));
			Coms++;
		}
		System.out
				.printf("%d Communities, Total Memberships = %d, Likelihood = %.2f, Epsilon = %f\n",
						Coms, nIDCIDPrS.size(), likelihood(), pNoCom);
	}

	public static void main(String args[]){
		/**Output file name prefix*/
		final String 	outFilePrefix = "demo";
		/**Input edgelist file name. DEMO: AGM with 2 communities*/
		//private static final String 	inFileName = "football.edgelist";
		final String 	inFileName = "DEMO";
		/**Input file name for node names (Node ID, Node label)*/
		final String 	labelFileName = "football.labels";
		/**Random seed for AGM*/
		final Integer 	randomSeed = 1;
		/**Edge probability between the nodes that do not share any community (default (0.0): set it to be 1 / N^2)*/
		final Float 	epsilon = 0f;
		/**Number of communities (0: determine it by AGM)*/
		final Integer 	numCommunites = 0;
		
		Long exeTime = System.currentTimeMillis();
		UndirectedGraph<Integer, DefaultEdge> g = null;
		
		Vector<Vector<Integer>> cmtyVV = new Vector<Vector<Integer>>();
		Hashtable<Integer, String> nIDName = new Hashtable<Integer, String>();
		
		if(inFileName.equals("DEMO")){
			Vector<Vector<Integer>> trueCmtyVV = new Vector<Vector<Integer>>();
			Randoms rnd = new Randoms(randomSeed);

			//generate community bipartite affiliation
			final Integer aBegin = 0, aEnd = 64, bBegin = 25, bEnd = 100;

		    trueCmtyVV.add(new Vector<Integer>());
		    trueCmtyVV.add(new Vector<Integer>());
		    for (int u = aBegin; u < aEnd; u++) {
		    	trueCmtyVV.get(0).add(u);
		    }
		    for (int u = bBegin; u < bEnd; u++) {
		    	trueCmtyVV.get(1).add(u);
		    }
		    
		    g = AGM.generateAGM(trueCmtyVV, 0.0, 0.5, rnd);
		}else if(!labelFileName.isEmpty()){

//		    G = TSnap::LoadEdgeList<PUNGraph>(InFNm);
//		    TSsParser Ss(LabelFNm, ssfTabSep);
//		    while (Ss.Next()) {
//		      if (Ss.Len() > 0) { NIDNameH.AddDat(Ss.GetInt(0), Ss.GetFld(1)); }
//		    }	
		}else{
//		    G = TAGMUtil::LoadEdgeListStr<PUNGraph>(InFNm, NIDNameH);			
		}


		System.out.println("Graph: "+g.vertexSet().size()+" Nodes "+g.edgeSet().size()+" Edges");

	  int maxIter = 10 * g.vertexSet().size() *g.vertexSet().size();
	  if (maxIter < 0) { maxIter = Integer.MAX_VALUE; }
	  int numComs = numCommunites;
	  if (numComs < 2) {
	    int initComs;
	    if (g.vertexSet().size() > 1000) {
	      initComs = g.vertexSet().size() / 5;
	      numComs = AGMUtil.findComsByAGM(g, initComs, maxIter, randomSeed, 1.5, epsilon, outFilePrefix);
	    } else {
	      initComs = g.vertexSet().size() / 5;
	      numComs = AGMUtil.findComsByAGM(g, initComs, maxIter, randomSeed, 1.2, epsilon, outFilePrefix);
	    }
	  }
	  AGMFit aGMFit = new AGMFit(g, numComs, randomSeed);
	  if (epsilon > 0) { aGMFit.setPNumCom(epsilon);  }
	  aGMFit.runMCMC(maxIter, 10, outFilePrefix);
	  cmtyVV = aGMFit.getCmtyVV(0.9999);

	  AGMUtil.dumpCmtyVV(outFilePrefix + "cmtyvv.txt", cmtyVV, nIDName);
	  AGMUtil.saveGephi(outFilePrefix + "graph.gexf", g, cmtyVV, 1.5, 1.5, nIDName);
	  aGMFit.printSummary();



	  System.out.printf("\nrun time: %s (%s)\n", System.currentTimeMillis() - exeTime, System.currentTimeMillis());

	  
	}




}
