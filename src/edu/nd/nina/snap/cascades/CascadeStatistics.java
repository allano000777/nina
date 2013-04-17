package edu.nd.nina.snap.cascades;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.imageio.ImageIO;

import com.panayotis.gnuplot.JavaPlot;
import com.panayotis.gnuplot.style.Style;
import com.panayotis.gnuplot.terminal.ImageTerminal;

import edu.nd.nina.DirectedGraph;
import edu.nd.nina.alg.BreadthFirstSearch;
import edu.nd.nina.alg.ConnectivityInspector;
import edu.nd.nina.graph.DefaultEdge;
import edu.nd.nina.graph.DirectedSubgraph;
import edu.nd.nina.math.Moment;
import edu.nd.nina.math.Randoms;
import edu.nd.nina.structs.Pair;
import edu.nd.nina.util.Plot;

/**
 * Structural properties of the cascades (propagation trees)
 * 
 * @author weninger
 * 
 */
public class CascadeStatistics {

	Hashtable<Float, Moment> NCascInf, NCascNet; // number of cascades
	Hashtable<Float, Moment> MxSzInf, MxSzNet; // size of the largest cascade
	Hashtable<Float, Moment> AvgSzInf, AvgSzNet; // average cascade size (number
													// of nodes)
	Hashtable<Float, Moment> NIsoInf, NIsoNet; // number of isolated nodes in
												// the cascade
	Hashtable<Float, Moment> NLfInf, NLfNet; // number of leaves in a cascade
	Hashtable<Float, Moment> NRtInf, NRtNet; // number of roots in a cascade
	Hashtable<Float, Moment> OutDegInf, OutDegNet; // average out-degree of a
													// cascade
	Hashtable<Float, Moment> InDegInf, InDegNet; // average in-degree of a
													// cascade
	// requires the root node (largest connected component)
	Hashtable<Float, Moment> DepthInf, DepthNet; // average depth (avg. distance
													// from leaves to the root)
	Hashtable<Float, Moment> MxWidInf, MxWidNet; // cascade width (max number of
													// nodes at any depth d)
	Hashtable<Float, Moment> MxLevInf, MxLevNet; // level of max width (depth of
													// max width)
	Hashtable<Float, Moment> IncLevInf, IncLevNet; // number of levels with
													// increasing width

	CascadeStatistics() {
		NCascInf = new Hashtable<Float, Moment>();
		NCascNet = new Hashtable<Float, Moment>();
		MxSzInf = new Hashtable<Float, Moment>();
		MxSzNet = new Hashtable<Float, Moment>();
		AvgSzInf = new Hashtable<Float, Moment>();
		NCascNet = new Hashtable<Float, Moment>();
		AvgSzNet = new Hashtable<Float, Moment>();
		
		InDegInf = new Hashtable<Float, Moment>();
		InDegNet = new Hashtable<Float, Moment>();
		OutDegInf = new Hashtable<Float, Moment>();
		OutDegNet = new Hashtable<Float, Moment>();
		NLfInf = new Hashtable<Float, Moment>();
		NLfNet = new Hashtable<Float, Moment>();
		NRtInf = new Hashtable<Float, Moment>();
		NRtNet = new Hashtable<Float, Moment>();
		NIsoInf = new Hashtable<Float, Moment>();
		NIsoNet = new Hashtable<Float, Moment>();
		
		DepthInf = new Hashtable<Float, Moment>();
		MxWidInf = new Hashtable<Float, Moment>();
		MxLevInf = new Hashtable<Float, Moment>();
		IncLevInf = new Hashtable<Float, Moment>();
		
		DepthNet = new Hashtable<Float, Moment>();
		MxWidNet = new Hashtable<Float, Moment>();
		MxLevNet = new Hashtable<Float, Moment>();
		IncLevNet = new Hashtable<Float, Moment>();
	}

	void plotAll(final String OutFNm, final String Desc) {
		plotAll(OutFNm, Desc, true);
	}

	void plotAll(final String OutFNm, final String Desc, final boolean DivByM) {

		String MStr = DivByM ? " / M (number of observed nodes)" : "";
		JavaPlot GP = new JavaPlot();
		ImageTerminal png = new ImageTerminal();
		File file = new File("." + System.getProperty("file.separator")
				+ "data" + System.getProperty("file.separator") + OutFNm
				+ System.getProperty("file.separator")
				+ String.format("ncasc-%s", OutFNm) + ".png");

		file.getParentFile().mkdirs();

		GP.setTerminal(png);
		GP.setTitle(Desc);
		GP.getAxis("x").setLabel("Fraction of observed data (P)");
		GP.getAxis("y").setLabel("Number of connected components" + MStr);

		Plot.addPlot(GP, NCascInf, Style.LINESPOINTS, "Influence cascade",
				"lw 2", true, false, false, false, false, false, false);
		Plot.addPlot(GP, NCascNet, Style.LINESPOINTS, "Network cascade",
				"lw 2", true, false, false, false, false, false, false);
		GP.plot();
		try {
			ImageIO.write(png.getImage(), "png", file);
		} catch (IOException ex) {
			System.err.print(ex);
		}

		GP = new JavaPlot();
		png = new ImageTerminal();
		file = new File("." + System.getProperty("file.separator") + "data"
				+ System.getProperty("file.separator") + OutFNm
				+ System.getProperty("file.separator")
				+ String.format("mxSz-%s", OutFNm) + ".png");

		file.getParentFile().mkdirs();

		GP.setTerminal(png);
		GP.setTitle(Desc);
		GP.getAxis("x").setLabel("Fraction of observed data (P)");
		GP.getAxis("y").setLabel("Size of largest connected component" + MStr);

		Plot.addPlot(GP, MxSzInf, Style.LINESPOINTS, "Influence cascade",
				"lw 2", true, false, false, false, false, false, false);
		Plot.addPlot(GP, MxSzNet, Style.LINESPOINTS, "Network cascade", "lw 2",
				true, false, false, false, false, false, false);
		GP.plot();
		try {
			ImageIO.write(png.getImage(), "png", file);
		} catch (IOException ex) {
			System.err.print(ex);
		}

		GP = new JavaPlot();
		png = new ImageTerminal();
		file = new File("." + System.getProperty("file.separator") + "data"
				+ System.getProperty("file.separator") + OutFNm
				+ System.getProperty("file.separator")
				+ String.format("avgSz-%s", OutFNm) + ".png");

		file.getParentFile().mkdirs();

		GP.setTerminal(png);
		GP.setTitle(Desc);
		GP.getAxis("x").setLabel("Fraction of observed data (P)");
		GP.getAxis("y").setLabel("Average connected component size" + MStr);

		Plot.addPlot(GP, AvgSzInf, Style.LINESPOINTS, "Influence cascade",
				"lw 2", true, false, false, false, false, false, false);
		Plot.addPlot(GP, AvgSzNet, Style.LINESPOINTS, "Network cascade",
				"lw 2", true, false, false, false, false, false, false);
		GP.plot();
		try {
			ImageIO.write(png.getImage(), "png", file);
		} catch (IOException ex) {
			System.err.print(ex);
		}

		GP = new JavaPlot();
		png = new ImageTerminal();
		file = new File("." + System.getProperty("file.separator") + "data"
				+ System.getProperty("file.separator") + OutFNm
				+ System.getProperty("file.separator")
				+ String.format("nIso-%s", OutFNm) + ".png");

		file.getParentFile().mkdirs();

		GP.setTerminal(png);
		GP.setTitle(Desc);
		GP.getAxis("x").setLabel("Fraction of observed data (P)");
		GP.getAxis("y").setLabel("Number of isolated nodes" + MStr);

		Plot.addPlot(GP, NIsoInf, Style.LINESPOINTS, "Influence cascade",
				"lw 2", true, false, false, false, false, false, false);
		Plot.addPlot(GP, NIsoNet, Style.LINESPOINTS, "Network cascade", "lw 2",
				true, false, false, false, false, false, false);
		GP.plot();
		try {
			ImageIO.write(png.getImage(), "png", file);
		} catch (IOException ex) {
			System.err.print(ex);
		}

		GP = new JavaPlot();
		png = new ImageTerminal();
		file = new File("." + System.getProperty("file.separator") + "data"
				+ System.getProperty("file.separator") + OutFNm
				+ System.getProperty("file.separator")
				+ String.format("nRt-%s", OutFNm) + ".png");

		file.getParentFile().mkdirs();

		GP.setTerminal(png);
		GP.setTitle(Desc);
		GP.getAxis("x").setLabel("Fraction of observed data (P)");
		GP.getAxis("y").setLabel("Number of root nodes" + MStr);

		Plot.addPlot(GP, NRtInf, Style.LINESPOINTS, "Influence cascade",
				"lw 2", true, false, false, false, false, false, false);
		Plot.addPlot(GP, NRtNet, Style.LINESPOINTS, "Network cascade", "lw 2",
				true, false, false, false, false, false, false);
		GP.plot();
		try {
			ImageIO.write(png.getImage(), "png", file);
		} catch (IOException ex) {
			System.err.print(ex);
		}

		GP = new JavaPlot();
		png = new ImageTerminal();
		file = new File("." + System.getProperty("file.separator") + "data"
				+ System.getProperty("file.separator") + OutFNm
				+ System.getProperty("file.separator")
				+ String.format("nLf-%s", OutFNm) + ".png");

		file.getParentFile().mkdirs();

		GP.setTerminal(png);
		GP.setTitle(Desc);
		GP.getAxis("x").setLabel("Fraction of observed data (P)");
		GP.getAxis("y").setLabel(
				"Number of leaves (nodes of zero out-degree)" + MStr);

		Plot.addPlot(GP, NLfInf, Style.LINESPOINTS, "Influence cascade",
				"lw 2", true, false, false, false, false, false, false);
		Plot.addPlot(GP, NLfNet, Style.LINESPOINTS, "Network cascade", "lw 2",
				true, false, false, false, false, false, false);
		GP.plot();
		try {
			ImageIO.write(png.getImage(), "png", file);
		} catch (IOException ex) {
			System.err.print(ex);
		}

		GP = new JavaPlot();
		png = new ImageTerminal();
		file = new File("." + System.getProperty("file.separator") + "data"
				+ System.getProperty("file.separator") + OutFNm
				+ System.getProperty("file.separator")
				+ String.format("outDeg-%s", OutFNm) + ".png");

		file.getParentFile().mkdirs();

		GP.setTerminal(png);
		GP.setTitle(Desc);
		GP.getAxis("x").setLabel("Fraction of observed data (P)");
		GP.getAxis("y").setLabel("Average Out-Degree (of a non-leaf)" + MStr);

		Plot.addPlot(GP, OutDegInf, Style.LINESPOINTS, "Influence cascade",
				"lw 2", true, false, false, false, false, false, false);
		Plot.addPlot(GP, OutDegNet, Style.LINESPOINTS, "Network cascade",
				"lw 2", true, false, false, false, false, false, false);
		GP.plot();
		try {
			ImageIO.write(png.getImage(), "png", file);
		} catch (IOException ex) {
			System.err.print(ex);
		}

		GP = new JavaPlot();
		png = new ImageTerminal();
		file = new File("." + System.getProperty("file.separator") + "data"
				+ System.getProperty("file.separator") + OutFNm
				+ System.getProperty("file.separator")
				+ String.format("inDeg-%s", OutFNm) + ".png");

		file.getParentFile().mkdirs();

		GP.setTerminal(png);
		GP.setTitle(Desc);
		GP.getAxis("x").setLabel("Fraction of observed data (P)");
		GP.getAxis("y").setLabel("Average In-Degree (of a non-root)" + MStr);

		Plot.addPlot(GP, InDegInf, Style.LINESPOINTS, "Influence cascade",
				"lw 2", true, false, false, false, false, false, false);
		Plot.addPlot(GP, InDegNet, Style.LINESPOINTS, "Network cascade",
				"lw 2", true, false, false, false, false, false, false);
		GP.plot();
		try {
			ImageIO.write(png.getImage(), "png", file);
		} catch (IOException ex) {
			System.err.print(ex);
		}

		GP = new JavaPlot();
		png = new ImageTerminal();
		file = new File("." + System.getProperty("file.separator") + "data"
				+ System.getProperty("file.separator") + OutFNm
				+ System.getProperty("file.separator")
				+ String.format("levels-%s", OutFNm) + ".png");

		file.getParentFile().mkdirs();

		GP.setTerminal(png);
		GP.setTitle(Desc);
		GP.getAxis("x").setLabel("Fraction of observed data (P)");
		GP.getAxis("y").setLabel("Average depth of largest component" + MStr);

		Plot.addPlot(GP, DepthInf, Style.LINESPOINTS, "Influence cascade",
				"lw 2", true, false, false, false, false, false, false);
		Plot.addPlot(GP, DepthNet, Style.LINESPOINTS, "Network cascade",
				"lw 2", true, false, false, false, false, false, false);
		GP.plot();
		try {
			ImageIO.write(png.getImage(), "png", file);
		} catch (IOException ex) {
			System.err.print(ex);
		}

		GP = new JavaPlot();
		png = new ImageTerminal();
		file = new File("." + System.getProperty("file.separator") + "data"
				+ System.getProperty("file.separator") + OutFNm
				+ System.getProperty("file.separator")
				+ String.format("width-%s", OutFNm) + ".png");

		file.getParentFile().mkdirs();

		GP.setTerminal(png);
		GP.setTitle(Desc);
		GP.getAxis("x").setLabel("Fraction of observed data (P)");
		GP.getAxis("y").setLabel(
				"Width of largest components (max nodes at any level)" + MStr);

		Plot.addPlot(GP, MxWidInf, Style.LINESPOINTS, "Influence cascade",
				"lw 2", true, false, false, false, false, false, false);
		Plot.addPlot(GP, MxWidNet, Style.LINESPOINTS, "Network cascade",
				"lw 2", true, false, false, false, false, false, false);
		GP.plot();
		try {
			ImageIO.write(png.getImage(), "png", file);
		} catch (IOException ex) {
			System.err.print(ex);
		}

		GP = new JavaPlot();
		png = new ImageTerminal();
		file = new File("." + System.getProperty("file.separator") + "data"
				+ System.getProperty("file.separator") + OutFNm
				+ System.getProperty("file.separator")
				+ String.format("levWidth-%s", OutFNm) + ".png");

		file.getParentFile().mkdirs();

		GP.setTerminal(png);
		GP.setTitle(Desc);
		GP.getAxis("x").setLabel("Fraction of observed data (P)");
		GP.getAxis("y").setLabel("Level with maximum width / Depth" + MStr);

		Plot.addPlot(GP, MxLevInf, Style.LINESPOINTS, "Influence cascade",
				"lw 2", true, false, false, false, false, false, false);
		Plot.addPlot(GP, MxLevNet, Style.LINESPOINTS, "Network cascade",
				"lw 2", true, false, false, false, false, false, false);
		GP.plot();
		try {
			ImageIO.write(png.getImage(), "png", file);
		} catch (IOException ex) {
			System.err.print(ex);
		}

		GP = new JavaPlot();
		png = new ImageTerminal();
		file = new File("." + System.getProperty("file.separator") + "data"
				+ System.getProperty("file.separator") + OutFNm
				+ System.getProperty("file.separator")
				+ String.format("levInc-%s", OutFNm) + ".png");

		file.getParentFile().mkdirs();

		GP.setTerminal(png);
		GP.setTitle(Desc);
		GP.getAxis("x").setLabel("Fraction of observed data (P)");
		GP.getAxis("y").setLabel(
				"Number of levels of increasing width / Depth" + MStr);

		Plot.addPlot(GP, IncLevInf, Style.LINESPOINTS, "Influence cascade",
				"lw 2", true, false, false, false, false, false, false);
		Plot.addPlot(GP, IncLevNet, Style.LINESPOINTS, "Network cascade",
				"lw 2", true, false, false, false, false, false, false);
		GP.plot();
		try {
			ImageIO.write(png.getImage(), "png", file);
		} catch (IOException ex) {
			System.err.print(ex);
		}
	}

	/**
	 * randomly remove nodes from the cascade and store cascade properties as a
	 * function of the fraction of removed nodes for more details see
	 * "Correcting for Missing Data in Information Cascades" by E. Sadikov, M.
	 * Medina, J. Leskovec, H. Garcia-Molina. WSDM, 2011
	 * 
	 * @param infCasc
	 * @param netCasc
	 * @param nIdInfTmH
	 * @param PStep
	 * @param NRuns
	 * @param divByM
	 * @param r
	 */
	public void sampleCascade(DirectedGraph<Integer, DefaultEdge> infCasc,
			DirectedGraph<Integer, DefaultEdge> netCasc,
			Hashtable<Integer, Integer> nIdInfTmH, double PStep, int NRuns,
			boolean divByM, Randoms r) {
		for (int Run = 0; Run < NRuns; Run++) {
			for (double P = PStep; P <= 1.01; P += PStep) {
				Set<Integer> NIdV = new HashSet<Integer>();
				for (Integer v : infCasc.vertexSet()) {
					if (r.GetUniDev() < P) {
						NIdV.add(v);
					}
				}
				DirectedSubgraph<Integer, DefaultEdge> InfG = new DirectedSubgraph<Integer, DefaultEdge>(
						infCasc, NIdV, null);
				DirectedSubgraph<Integer, DefaultEdge> NetG = new DirectedSubgraph<Integer, DefaultEdge>(
						netCasc, NIdV, null);

				if (InfG.vertexSet().isEmpty()) {
					continue;
				}
				takeStat(InfG, NetG, nIdInfTmH, P, divByM);
			}
		}

	}

	/**
	 * 
	 * @param InfG
	 * @param NetG
	 * @param nIdInfTmH
	 * @param P
	 * @param DivByM
	 */
	private void takeStat(DirectedGraph<Integer, DefaultEdge> InfG,
			DirectedGraph<Integer, DefaultEdge> NetG,
			Hashtable<Integer, Integer> nIdInfTmH, double P, boolean DivByM) {
		final double M = DivByM ? InfG.vertexSet().size() : 1d;
		assert (M >= 1);
		DirectedSubgraph<Integer, DefaultEdge> CcInf, CcNet; // largest
																// connected
																// component
		// connected components and sizes
		ConnectivityInspector<Integer, DefaultEdge> ci = new ConnectivityInspector<Integer, DefaultEdge>(
				InfG);
		List<Set<Integer>> CnComV = ci.connectedSets();
		addMoment(NCascInf, P, CnComV.size() / M);
		addMoment(MxSzInf, P, CnComV.get(0).size() / M);

		int a = 0;
		for (int i = 0; i < CnComV.size(); i++) {
			a += CnComV.get(i).size();
		}
		addMoment(AvgSzInf, P, a / (double) CnComV.size() * M);

		CcInf = new DirectedSubgraph<Integer, DefaultEdge>(InfG, CnComV.get(0),
				null);

		ci = new ConnectivityInspector<Integer, DefaultEdge>(NetG);
		CnComV = ci.connectedSets();
		addMoment(NCascNet, P, CnComV.size() / M);
		addMoment(MxSzNet, P, CnComV.get(0).size() / M);

		a = 0;
		for (int i = 0; i < CnComV.size(); i++) {
			a += CnComV.get(i).size();
		}
		addMoment(AvgSzNet, P, a / (double) CnComV.size() * M);

		CcNet = new DirectedSubgraph<Integer, DefaultEdge>(NetG, CnComV.get(0),
				null);

		// count isolated nodes and leaves; average in- and out-degree (skip
		// leaves)

		int i1 = 0, i2 = 0, l1 = 0, l2 = 0, r1 = 0, r2 = 0, ENet = 0, EInf = 0;
		double ci1 = 0, ci2 = 0, co1 = 0, co2 = 0;
		for (Integer v : InfG.vertexSet()) {
			if (InfG.outDegreeOf(v) == 0 && InfG.inDegreeOf(v) > 0) {
				l1++;
			}
			if (InfG.outDegreeOf(v) > 0 && InfG.inDegreeOf(v) == 0) {
				r1++;
			}
			if (InfG.edgesOf(v).size() == 0) {
				i1++;
			}
			if (InfG.inDegreeOf(v) > 0) {
				ci1 += 1;
			}
			if (InfG.outDegreeOf(v) > 0) {
				co1 += 1;
			}
			EInf += InfG.outDegreeOf(v);
		}

		for (Integer v : NetG.vertexSet()) {
			if (NetG.outDegreeOf(v) == 0 && NetG.inDegreeOf(v) > 0) {
				l2++;
			}
			if (NetG.outDegreeOf(v) > 0 && NetG.inDegreeOf(v) == 0) {
				r2++;
			}
			if (NetG.edgesOf(v).size() == 0) {
				i2++;
			}
			if (NetG.inDegreeOf(v) > 0) {
				ci2 += 1;
			}
			if (NetG.outDegreeOf(v) > 0) {
				co2 += 1;
			}
			ENet += NetG.outDegreeOf(v);
		}

		if (ci1 > 0)
			addMoment(InDegInf, P, EInf / ci1);
		if (ci2 > 0)
			addMoment(InDegNet, P, ENet / ci2);
		if (co1 > 0)
			addMoment(OutDegInf, P, EInf / co1);
		if (co2 > 0)
			addMoment(OutDegNet, P, ENet / co2);
		addMoment(NLfInf, P, l1 / M);
		addMoment(NLfNet, P, l2 / M);
		addMoment(NRtInf, P, r1 / M);
		addMoment(NRtNet, P, r2 / M);
		addMoment(NIsoInf, P, i1 / M);
		addMoment(NIsoNet, P, i2 / M);

		// cascade depth
		final double M1 = DivByM ? CcNet.vertexSet().size() : 1;
		assert (M1 >= 1);
		int Root = findCascadeRoot(CcInf, nIdInfTmH);
		Vector<Pair<Integer, Integer>> HopCntV = BreadthFirstSearch.getNodesAtHops(CcInf, Root);
		int MxN = 0, Lev = 0, IncL = 0;
		for (int i = 0; i < HopCntV.size(); i++) {
			if (MxN < HopCntV.get(i).p2) {
				MxN = HopCntV.get(i).p2;
				Lev = HopCntV.get(i).p1;
			}
			if (i > 0 && HopCntV.get(i - 1).p2 <= HopCntV.get(i).p2) {
				IncL++;
			}
		}
		double D = 0;
		int c = 0;
		D = HopCntV.firstElement().p1; // maximum depth
		c = 1;
		if (c != 0 && D != 0) {
			D = D / c;
			addMoment(DepthInf, P, D / M1);
			addMoment(MxWidInf, P, MxN / M1);
			addMoment(MxLevInf, P, Lev / D);
			addMoment(IncLevInf, P, IncL / D);

		}

		Root = findCascadeRoot(CcNet, nIdInfTmH);
		HopCntV = BreadthFirstSearch.getNodesAtHops(CcNet, Root);

		MxN = 0;
		Lev = 0;
		IncL = 0;
		D = 0;
		c = 0;
		for (int i = 0; i < HopCntV.size(); i++) {
			if (MxN < HopCntV.get(i).p2) {
				MxN = HopCntV.get(i).p2;
				Lev = HopCntV.get(i).p1;
			}
			if (i > 0 && HopCntV.get(i - 1).p2 <= HopCntV.get(i).p2) {
				IncL++;
			}
		}

		D = HopCntV.firstElement().p1;
		c = 1; // maximum depth
		if (c != 0 && D != 0) {
			D = D / c;
			addMoment(DepthNet, P, D / M1);
			addMoment(MxWidNet, P, MxN / M1);
			addMoment(MxLevNet, P, Lev / D);
			addMoment(IncLevNet, P, IncL / D);
		}
	}

	/**
	 * 
	 * @param G
	 * @param nIdInfTmH
	 * @return
	 */
	private int findCascadeRoot(DirectedSubgraph<Integer, DefaultEdge> G,
			Hashtable<Integer, Integer> nIdInfTmH) {
		// earliest infected node
		int Min = Integer.MAX_VALUE;
		for (Integer v : G.vertexSet()) {

			if (v < Min && G.inDegreeOf(v) == 0) {

				Min = v;
			}
		}
		assert (Min != Integer.MAX_VALUE);
		return Min;

	}
	
	/**
	 * 
	 * @param x
	 * @param p
	 * @param d
	 */
	private void addMoment(Hashtable<Float, Moment> x, double p, double d) {
		if (!x.containsKey(p)) {
			Moment m = new Moment();
			m.add((float) d);
			x.put((float) p, m);
		} else {
			x.get(p).add((float) d);
		}
	}
}
