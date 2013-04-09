package edu.nd.nina.snap.agm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Hashtable;
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
import edu.nd.nina.math.LogisticRegressionFit;
import edu.nd.nina.math.LogisticRegressionPrediction;
import edu.nd.nina.math.Randoms;
import edu.nd.nina.structs.Pair;
import edu.nd.nina.structs.Triple;

public class AGMUtil {

	/**
	 * estimate number of communities using AGM
	 * 
	 * @param g
	 * @param initComs
	 * @param maxIter
	 * @param randomSeed
	 * @param regGap
	 * @param pNumCom
	 * @param outfileprefix
	 * @return
	 */
	public static Integer findComsByAGM(
			final UndirectedGraph<Integer, DefaultEdge> g,
			final Integer initComs, final Integer maxIter,
			final Integer randomSeed, final Double regGap, final Float pNumCom,
			final String outfileprefix) {

		Randoms rnd = new Randoms(randomSeed);
		int lambdaIter = 100;
		if (g.vertexSet().size() < 200) {
			lambdaIter = 1;
		}
		if (g.vertexSet().size() < 200 && g.edgeSet().size() > 2000) {
			lambdaIter = 100;
		}

		// Find coms with large C
		AGMFit aGMFitM = new AGMFit(g, initComs, randomSeed);
		if (pNumCom > 0.0) {
			aGMFitM.setPNumCom(pNumCom);
		}
		aGMFitM.runMCMC(maxIter, lambdaIter, outfileprefix);

		int TE = g.edgeSet().size();
		Vector<Float> RegV = new Vector<Float>();
		RegV.add((float) (0.3 * TE));
		for (int r = 0; r < 25; r++) {
			RegV.add((float) (RegV.lastElement() * regGap));
		}
		Vector<Pair<Float, Float>> RegComsV = new Vector<Pair<Float, Float>>();
		Vector<Pair<Float, Float>> RegLV = new Vector<Pair<Float, Float>>();
		Vector<Pair<Float, Float>> RegBICV = new Vector<Pair<Float, Float>>();
		Vector<Float> LV = new Vector<Float>();
		Vector<Float> BICV = new Vector<Float>();
		// record likelihood and number of communities with nonzero P_c
		for (int r = 0; r < RegV.size(); r++) {			
			double RegCoef = RegV.get(r);
			aGMFitM.setRegCoef(RegCoef);
			aGMFitM.MLEGradAscentGivenCAG(0.01, 1000);
			aGMFitM.setRegCoef(0.0);

			Vector<Vector<Integer>> EstCmtyVV = aGMFitM.getCmtyVV(0.99);
			int NumLowQ = EstCmtyVV.size();
			RegComsV.add(new Pair<Float, Float>((float) RegCoef,
					(float) NumLowQ));

			if (EstCmtyVV.size() > 0) {
				AGMFit aFTemp = new AGMFit(g, EstCmtyVV, rnd);
				aFTemp.MLEGradAscentGivenCAG(0.001, 1000);
				double CurL = aFTemp.likelihood();
				LV.add((float) CurL);
				BICV.add((float) (-2.0 * CurL + (double) EstCmtyVV.size()
						* Math.log((double) g.vertexSet().size()
								* (g.vertexSet().size() - 1) / 2.0)));
			} else {
				break;
			}
			System.out.print(".");			
		}
		System.out.println();
		// if likelihood does not exist or does not change at all, report the
		// smallest number of communities or 2
		if (LV.size() == 0) {
			return 2;
		} else if (LV.get(0) == LV.lastElement()) {
			return (int) Math.max(2.0, RegComsV.get(LV.size() - 1).p2);
		}

		// normalize likelihood and BIC to 0~100
		int MaxL = 100;
		{
			Vector<Float> ValueV = LV;
			Vector<Pair<Float, Float>> RegValueV = RegLV;
			double MinValue = Float.MAX_VALUE, MaxValue = Float.MIN_VALUE;
			for (int l = 0; l < ValueV.size(); l++) {
				if (ValueV.get(l) < MinValue) {
					MinValue = ValueV.get(l);
				}
				if (ValueV.get(l) > MaxValue) {
					MaxValue = ValueV.get(l);
				}
			}
			while (ValueV.size() < RegV.size()) {
				ValueV.add((float) MinValue);
			}
			double RangeVal = MaxValue - MinValue;
			for (int l = 0; l < ValueV.size(); l++) {
				RegValueV
						.add(new Pair<Float, Float>(
								RegV.get(l),
								(float) (((double) MaxL)
										* (ValueV.get(l) - MinValue) / RangeVal)));
			}
		}

		{
			Vector<Float> ValueV = BICV;
			Vector<Pair<Float, Float>> RegValueV = RegBICV;
			double MinValue = Float.MAX_VALUE, MaxValue = Float.MIN_VALUE;
			for (int l = 0; l < ValueV.size(); l++) {
				if (ValueV.get(l) < MinValue) {
					MinValue = ValueV.get(l);
				}
				if (ValueV.get(l) > MaxValue) {
					MaxValue = ValueV.get(l);
				}
			}
			while (ValueV.size() < RegV.size()) {
				ValueV.add((float) MinValue);
			}
			double RangeVal = MaxValue - MinValue;
			for (int l = 0; l < ValueV.size(); l++) {
				RegValueV
						.add(new Pair<Float, Float>(
								RegV.get(l),
								(float) (((double) MaxL)
										* (ValueV.get(l) - MinValue) / RangeVal)));
			}
		}

		// fit logistic regression to normalized likelihood.
		Vector<Vector<Float>> XV = new Vector<Vector<Float>>(RegLV.size());
		Vector<Float> YV = new Vector<Float>(RegLV.size());
		for (int l = 0; l < RegLV.size(); l++) {
			Vector<Float> x = new Vector<Float>();
			x.add((float) Math.log(RegLV.get(l).p1));
			XV.add(x);
			YV.add((float) (RegLV.get(l).p2 / (double) MaxL));
		}
		Vector<Pair<Float, Float>> LRVScaled = new Vector<Pair<Float, Float>>();
		Vector<Pair<Float, Float>> LRV = new Vector<Pair<Float, Float>>();
		LogisticRegressionFit LRFit = new LogisticRegressionFit();
		LogisticRegressionPrediction LRMd = LRFit.CalcLogRegNewton(XV, YV,
				outfileprefix);
		for (int l = 0; l < RegLV.size(); l++) {
			LRV.add(new Pair<Float, Float>(RegV.get(l), (float) LRMd.GetCfy(XV
					.get(l))));
			LRVScaled.add(new Pair<Float, Float>(RegV.get(l), ((float) MaxL)
					* LRV.lastElement().p2));
		}

		// estimate # communities from fitted logistic regression
		int NumComs = 0, IdxRegDrop = 0;
		double LRThres = 1.1, RegDrop; // 1 / (1 + exp(1.1)) = 0.25
		double LeftReg = 0.0, RightReg = 0.0;
		Vector<Float> Theta = LRMd.GetTheta();

		RegDrop = (-Theta.get(1) - LRThres) / Theta.get(0);
		if (RegDrop <= XV.get(0).get(0)) {
			NumComs = RegComsV.get(0).p2.intValue();
		} else if (RegDrop >= XV.lastElement().get(0)) {
			NumComs = RegComsV.lastElement().p2.intValue();
		} else { // interpolate for RegDrop
			for (int i = 0; i < XV.size(); i++) {
				if (XV.get(i).get(0) > RegDrop) {
					IdxRegDrop = i;
					break;
				}
			}

			if (IdxRegDrop == 0) {
				System.err.printf(
						"Error!! RegDrop:%f, Theta[0]:%f, Theta[1]:%f\n",
						RegDrop, Theta.get(0), Theta.get(1));
				for (int l = 0; l < RegLV.size(); l++) {
					System.out.printf("X[%d]:%f, Y[%d]:%f\n", l,
							XV.get(l).get(0), l, YV.get(l));
				}
			}
			assert (IdxRegDrop > 0);
			LeftReg = RegDrop - XV.get(IdxRegDrop - 1).get(0);
			RightReg = XV.get(IdxRegDrop).get(0) - RegDrop;
			NumComs = (int) Math.round((RightReg
					* RegComsV.get(IdxRegDrop - 1).p2 + LeftReg
					* RegComsV.get(IdxRegDrop).p2)
					/ (LeftReg + RightReg));

		}
		// printf("Interpolation coeff: %f, %f, index at drop:%d (%f), Left-Right Vals: %f, %f\n",
		// LeftReg, RightReg, IdxRegDrop, RegDrop, RegComsV[IdxRegDrop -
		// 1].Val2, RegComsV[IdxRegDrop].Val2);
		System.out.printf("Num Coms:%d\n", NumComs);
		if (NumComs < 2) {
			NumComs = 2;
		}

		if (outfileprefix.length() > 0) {
			ImageTerminal png = new ImageTerminal();
			File file = new File("." + System.getProperty("file.separator")
					+ "data" + System.getProperty("file.separator")
					+ outfileprefix + System.getProperty("file.separator")
					+ outfileprefix + "_l.png");

			file.getParentFile().mkdirs();

			JavaPlot GP1 = new JavaPlot();
			GP1.setTerminal(png);
			double[][] zz = new double[RegComsV.size()][2];
			for (int i = 0; i<RegComsV.size(); i++){
				zz[i][0] = RegComsV.get(i).p1;
				zz[i][1] = RegComsV.get(i).p2;
			}
			GP1.addPlot(zz);	
			
			zz = new double[RegLV.size()][2];
			for (int i = 0; i<RegLV.size(); i++){
				zz[i][0] = RegLV.get(i).p1;
				zz[i][1] = RegLV.get(i).p2;
			}
			GP1.addPlot(zz);
			
			zz = new double[RegBICV.size()][2];
			for (int i = 0; i<RegBICV.size(); i++){ 
				zz[i][0] = RegBICV.get(i).p1;
				zz[i][1] = RegBICV.get(i).p2;
			}
			GP1.addPlot(zz);
			
			zz = new double[LRVScaled.size()][2];
			for (int i = 0; i<LRVScaled.size(); i++){
				zz[i][0] = LRVScaled.get(i).p1;
				zz[i][1] = LRVScaled.get(i).p2;
			}
			GP1.addPlot(zz);			
			GP1.getAxis("x").setLogScale(true);
			
			((AbstractPlot) GP1.getPlots().get(0)).setTitle("C");
			((AbstractPlot) GP1.getPlots().get(1)).setTitle("likelihood");
			((AbstractPlot) GP1.getPlots().get(2)).setTitle("BIC");
			((AbstractPlot) GP1.getPlots().get(3)).setTitle("Sigmoid (scaled)");
			((AbstractPlot) GP1.getPlots().get(0)).getPlotStyle().setStyle(
					Style.LINESPOINTS);
			((AbstractPlot) GP1.getPlots().get(1)).getPlotStyle().setStyle(
					Style.LINESPOINTS);
			((AbstractPlot) GP1.getPlots().get(2)).getPlotStyle().setStyle(
					Style.LINESPOINTS);
			((AbstractPlot) GP1.getPlots().get(3)).getPlotStyle().setStyle(
					Style.LINESPOINTS);
			
			String titleStr = String.format("N:%d, E:%d ", g.vertexSet().size(),
					TE);
			GP1.setTitle(titleStr);
			GP1.plot();
			
			try {
		        ImageIO.write(png.getImage(), "png", file);
		    } catch (IOException ex) {
		        System.err.print(ex);
		    }
		}

		return NumComs;

	}

	public static Set<Integer> getNbhCom(
			UndirectedGraph<Integer, DefaultEdge> g, Integer n) {
		Set<Integer> nBCmty = new HashSet<Integer>(g.degreeOf(n) + 1);
		nBCmty.add(n);
		for (DefaultEdge e : g.edgesOf(n)) {
			if (g.getEdgeSource(e) == n) {
				nBCmty.add(g.getEdgeTarget(e));
			} else {
				nBCmty.add(g.getEdgeSource(e));
			}
		}
		return nBCmty;
	}

	public static double getConductance(
			UndirectedGraph<Integer, DefaultEdge> g, Set<Integer> cmtyS,
			int edges) {
		final int edges2 = edges >= 0 ? 2 * edges : g.edgeSet().size();
		int vol = 0, cut = 0;
		double phi = 0.0;
		for (Integer n : cmtyS) {
			if (!g.containsVertex(n)) {
				continue;
			}

			for (DefaultEdge e : g.edgesOf(n)) {
				if (g.getEdgeSource(e) == n) {
					if (!cmtyS.contains(g.getEdgeTarget(e))) {
						cut += 1;
					}
				} else {
					if (!cmtyS.contains(g.getEdgeSource(e))) {
						cut += 1;
					}
				}
			}
			vol += g.degreeOf(n);
		}
		// get conductance
		if (vol != edges2) {
			if (2 * vol > edges2) {
				phi = cut / ((double) (edges2 - vol));
			} else if (vol == 0) {
				phi = 0.0;
			} else {
				phi = cut / ((double) vol);
			}
		} else {
			if (vol == edges2) {
				phi = 1.0;
			}
		}
		return phi;
	}

	public static Hashtable<Integer, Set<Integer>> getNodeMembership(
			Hashtable<Integer, Set<Integer>> nIDComVH, final Vector<Set<Integer>> cmtyVV) {
		for (int i = 0; i < cmtyVV.size(); i++) {
			int CID = i;
			for (Integer NID : cmtyVV.get(i)) {
				if (nIDComVH.containsKey(NID)) {
					Set<Integer> x = nIDComVH.get(NID);
					x.add(CID);
				} else {
					Set<Integer> x = new HashSet<Integer>();
					x.add(CID);
					nIDComVH.put(NID, x);
				}

			}
		}
		return nIDComVH;
	}

	public static Hashtable<Integer, Vector<Integer>> getNodeMembership(final Vector<Vector<Integer>> CmtyVV) {
		Hashtable<Integer, Vector<Integer>> NIDComVH = new Hashtable<Integer, Vector<Integer>>(); 
		for (int CID=0; CID<CmtyVV.size(); CID++) {
			for (Integer NID : CmtyVV.get(CID)) {
				if (!NIDComVH.contains(NID)) {
					Vector<Integer> v = new Vector<Integer>();
					v.add(CID);
					NIDComVH.put(NID, v);
				} else {
					Vector<Integer> v = NIDComVH.get(NID);
					v.add(CID);
					NIDComVH.put(NID, v);
				}
			}
		}
		return NIDComVH;
	}

	

	public static Set<Integer> getIntersection(Set<Integer> set1,
			Set<Integer> set2) {
		Set<Integer> ret = new HashSet<Integer>();
		for (Integer i : set1) {
			if (set2.contains(i)) {
				ret.add(i);
			}
		}
		return ret;
	}

	/**
	 * dump bipartite community affiliation into a text file with node names
	 * 
	 * @param OutFNm
	 * @param CmtyVV
	 * @param NIDNmH
	 */
	static void dumpCmtyVV(final String OutFNm, Vector<Vector<Integer>> CmtyVV,
			Hashtable<Integer, String> NIDNmH) {
		PrintWriter f;
		try {
			f = new PrintWriter(OutFNm);

			for (int c = 0; c < CmtyVV.size(); c++) {
				for (int u = 0; u < CmtyVV.get(c).size(); u++) {
					if (NIDNmH.containsKey(CmtyVV.get(c).get(u))) {
						f.printf("%s\t", NIDNmH.get(CmtyVV.get(c).get(u)));
					} else {
						f.printf("%d\t", (int) CmtyVV.get(c).get(u));
					}
				}
				f.printf("\n");
			}
			f.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * save graph into a gexf file which Gephi can read
	 * 
	 * @param OutFNm
	 * @param g
	 * @param CmtyVVAtr
	 * @param MaxSz
	 * @param MinSz
	 * @param NIDNameH
	 * @param NIDColorH
	 */
	public static void saveGephi(
			final String OutFNm,
			final UndirectedGraph<Integer, DefaultEdge> g,
			final Vector<Vector<Integer>> CmtyVVAtr,
			final double MaxSz,
			final double MinSz,
			final Hashtable<Integer, String> NIDNameH,
			final Hashtable<Integer, Triple<Integer, Integer, Integer>> NIDColorH) {

		Hashtable<Integer, Vector<Integer>> NIDComVHAtr = AGMUtil.getNodeMembership(CmtyVVAtr);

		PrintWriter f = null;
		try {
			f = new PrintWriter(OutFNm);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		f.printf("<?xml version='1.0' encoding='UTF-8'?>\n");
		f.printf("<gexf xmlns='http://www.gexf.net/1.2draft' xmlns:viz='http://www.gexf.net/1.1draft/viz' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='http://www.gexf.net/1.2draft http://www.gexf.net/1.2draft/gexf.xsd' version='1.2'>\n");
		f.printf("\t<graph mode='static' defaultedgetype='undirected'>\n");
		if (CmtyVVAtr.size() > 0) {
			f.printf("\t<attributes class='node'>\n");
			for (int c = 0; c < CmtyVVAtr.size(); c++) {
				f.printf("\t\t<attribute id='%d' title='c%d' type='boolean'>",
						c, c);
				f.printf("\t\t<default>false</default>\n");
				f.printf("\t\t</attribute>\n");
			}
			f.printf("\t</attributes>\n");
		}
		f.printf("\t\t<nodes>\n");
		for (Integer NID : g.vertexSet()) {

			String Label = NIDNameH.containsKey(NID) ? NIDNameH.get(NID) : "";
			Triple<Integer, Integer, Integer> Color = NIDColorH.containsKey(NID) ? NIDColorH
					.get(NID) : new Triple<Integer, Integer, Integer>(120, 120,
					120);

			double Size = MinSz;
			double SizeStep = (MaxSz - MinSz) / (double) CmtyVVAtr.size();
			if (NIDComVHAtr.containsKey(NID)) {
				Size = MinSz + SizeStep
						* (double) NIDComVHAtr.get(NID).size();
			}
			double Alpha = 1.0;
			f.printf("\t\t\t<node id='%d' label='%s'>\n", NID, Label);
			f.printf("\t\t\t\t<viz:color r='%d' g='%d' b='%d' a='%.1f'/>\n",
					Color.v1, Color.v2, Color.v3, Alpha);
			f.printf("\t\t\t\t<viz:size value='%.3f'/>\n", Size);
			// specify attributes
			if (NIDComVHAtr.containsKey(NID)) {
				f.printf("\t\t\t\t<attvalues>\n");
				for (int c = 0; c < NIDComVHAtr.get(NID).size(); c++) {
					int CID = NIDComVHAtr.get(NID).get(c);
					f.printf("\t\t\t\t\t<attvalue for='%d' value='true'/>\n",
							CID);
				}
				f.printf("\t\t\t\t</attvalues>\n");
			}

			f.printf("\t\t\t</node>\n");
		}
		f.printf("\t\t</nodes>\n");
		// plot edges
		int EID = 0;
		f.printf("\t\t<edges>\n");
		for (DefaultEdge EI : g.edgeSet()) {
			f.printf("\t\t\t<edge id='%d' source='%d' target='%d'/>\n", EID++,
					g.getEdgeSource(EI), g.getEdgeTarget(EI));
		}
		f.printf("\t\t</edges>\n");
		f.printf("\t</graph>\n");
		f.printf("</gexf>\n");
		f.close();
	}

	public static void saveGephi(final String OutFNm,
			final UndirectedGraph<Integer, DefaultEdge> g,
			final Vector<Vector<Integer>> CmtyVVAtr, final double MaxSz,
			final double MinSz) {
		Hashtable<Integer, String> TmpH = new Hashtable<Integer, String>();
		saveGephi(OutFNm, g, CmtyVVAtr, MaxSz, MinSz, TmpH);
	}

	static void saveGephi(final String OutFNm,
			final UndirectedGraph<Integer, DefaultEdge> g,
			final Vector<Vector<Integer>> CmtyVVAtr, final double MaxSz,
			final double MinSz, final Hashtable<Integer, String> NIDNameH) {
		Hashtable<Integer, Triple<Integer, Integer, Integer>> TmpH = new Hashtable<Integer, Triple<Integer, Integer, Integer>>();
		saveGephi(OutFNm, g, CmtyVVAtr, MaxSz, MinSz, NIDNameH, TmpH);
	}

}
