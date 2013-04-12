package edu.nd.nina.alg;

import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import edu.nd.nina.DirectedGraph;
import edu.nd.nina.Graph;
import edu.nd.nina.UndirectedGraph;
import edu.nd.nina.graph.AsDirectedGraph;
import edu.nd.nina.graph.Multigraph;
import edu.nd.nina.math.Moment;
import edu.nd.nina.structs.Pair;



public class CalculateStatistics<V extends Comparable<V>, E> {

	private static final Integer NDiamRuns = 10;
	private static final Integer TakeSngVals = 100;

	// scalar statistics
	Hashtable<StatVal, Float> ValStatH;
	// distribution statistics
	Hashtable<StatVal, Vector<Pair<Float, Float>>> DistrStatH;

	public CalculateStatistics() {
		ValStatH = new Hashtable<StatVal, Float>();
	}

	public void calcStats(Graph<V, E> graph, Set<StatVal> statFSet) {
		if (graph instanceof DirectedGraph) {
			calcStats((DirectedGraph<V, E>) graph, statFSet);
		} else {
			calcStats((UndirectedGraph<V, E>) graph, statFSet);
		}
	}

	private void calcStats(UndirectedGraph<V, E> graph, Set<StatVal> statFSet) {
		System.out.printf("GraphStatistics:  G(%d, %d)\n", graph.vertexSet()
				.size(), graph.edgeSet().size());
		long FullTm = System.currentTimeMillis();

		if (statFSet.contains(StatVal.gsvNone)) {
			return;
		}
		calcBasicStat(graph, false);
		if (statFSet.contains(StatVal.gsdWcc)) {
			ConnectivityInspector<V, E> ci = new ConnectivityInspector<V, E>(
					graph);
			calcBasicStat(ci.getMaxWcc(), true);
		}
		// degrees
		calcDegDistr(graph, statFSet);
		if (statFSet.contains(StatVal.gsvFullDiam)
				|| statFSet.contains(StatVal.gsvEffDiam)
				|| statFSet.contains(StatVal.gsdHops)
				|| statFSet.contains(StatVal.gsvEffWccDiam)
				|| statFSet.contains(StatVal.gsdWccHops)
				|| statFSet.contains(StatVal.gsdWcc)
				|| statFSet.contains(StatVal.gsdScc)
				|| statFSet.contains(StatVal.gsdClustCf)
				|| statFSet.contains(StatVal.gsvClustCf)
				|| statFSet.contains(StatVal.gsdTriadPart)) {
			DirectedGraph<V, E> NGraph = new AsDirectedGraph<V, E>(graph);
			// diameter
			calcDiam(NGraph, statFSet, false);
			// components
			calcConnComp(NGraph, statFSet);
			// spectral
			calcSpectral(NGraph, statFSet, -1);
			// clustering coeffient
			if (statFSet.contains(StatVal.gsdClustCf)
					|| statFSet.contains(StatVal.gsvClustCf)) {
				calcClustCf(NGraph);
			}
			if (statFSet.contains(StatVal.gsdTriadPart)) {
				calcTriadPart(NGraph);
			}
			if (statFSet.contains(StatVal.gsvFullDiam)
					|| statFSet.contains(StatVal.gsvEffWccDiam)) {
				ConnectivityInspector<V, E> ci = new ConnectivityInspector<V, E>(
						graph);
				calcDiam(ci.getMaxWcc(), statFSet, true);
			}
			System.out.printf("  [%s]\n", System.currentTimeMillis() - FullTm);
		}
	}

	private void calcStats(DirectedGraph<V, E> graph, Set<StatVal> statFSet) {
		System.out.printf("GraphStatistics:  G(%u, %u)\n", graph.vertexSet()
				.size(), graph.edgeSet().size());
		long FullTm = System.currentTimeMillis();

		if (statFSet.contains(StatVal.gsvNone)) {
			return;
		}

		calcBasicStat(graph, true);
		calcDiam(graph, statFSet, false);
		if (statFSet.contains(StatVal.gsdWcc)
				|| statFSet.contains(StatVal.gsdWccHops)
				|| statFSet.contains(StatVal.gsvFullDiam)
				|| statFSet.contains(StatVal.gsvEffWccDiam)) {
			ConnectivityInspector<V, E> ci = new ConnectivityInspector<V, E>(
					graph);
			UndirectedGraph<V, E> WccGraph = ci.getMaxWcc();
			calcBasicStat(WccGraph, true);
			calcDiam(WccGraph, statFSet, true);
		}

		// degrees
		calcDegDistr(graph, statFSet);
		// components
		calcConnComp(graph, statFSet);
		// spectral
		calcSpectral(graph, statFSet, -1);
		// clustering coeffient
		if (statFSet.contains(StatVal.gsdClustCf)
				|| statFSet.contains(StatVal.gsvClustCf)) {
			calcClustCf(graph);
		}
		if (statFSet.contains(StatVal.gsdTriadPart)) {
			calcTriadPart(graph);
		}
		System.out.printf("  [%s]\n", System.currentTimeMillis() - FullTm);
	}

	private void calcTriadPart(DirectedGraph<V, E> graph) {
		System.out.printf("triadParticip ");
		Vector<Pair<Float, Float>> TriadCntV = new Vector<Pair<Float, Float>>();
		Triad<V, E> t = new Triad<V, E>();
		Vector<Pair<Integer, Integer>> CntV = t.getTriadParticip(graph);
		for (int i = 0; i < CntV.size(); i++) {
			TriadCntV.add(new Pair<Float, Float>((float) CntV.get(i).p1,
					(float) CntV.get(i).p2));
		}
		DistrStatH.put(StatVal.gsdTriadPart, TriadCntV);
	}

	public float calcClustCf(Graph<V, E> graph) {
		return calcClustCf(graph, -1);
	}

	public float calcClustCf(Graph<V, E> graph, int SampleNodes) {
		System.out.printf("clustCf ");
		Triad<V, E> t = new Triad<V, E>();
		float ClustCf = t.getClustCf(graph, SampleNodes);
		setVal(StatVal.gsvClustCf, ClustCf);
		setVal(StatVal.gsvOpenTriads, (float) t.getOpen());
		setVal(StatVal.gsvClosedTriads, (float) t.getClosed());
		return ClustCf;
	}

	void calcSpectral(DirectedGraph<V, E> graph, int _TakeSngVals) {
		Set<StatVal> s = new TreeSet<StatVal>();
		s.add(StatVal.gsdSngVal);
		s.add(StatVal.gsdSngVec);
		calcSpectral(graph, s, _TakeSngVals);
	}

	void calcSpectral(DirectedGraph<V, E> graph, Set<StatVal> statFSet,
			int _TakeSngVals) {
		if (_TakeSngVals == -1) {
			_TakeSngVals = TakeSngVals;
		}
		// singular values, vectors

		if (statFSet.contains(StatVal.gsdSngVal)) {
			final int SngVals = Math.min(_TakeSngVals,
					graph.vertexSet().size() / 2);
			SingularValueDecomposition<V, E> svd = new SingularValueDecomposition<V, E>(
					graph);

			Vector<Float> SngValV1 = svd.GetSngVals(SngVals);
			Collections.sort(SngValV1);
			Vector<Pair<Float, Float>> SngValV = new Vector<Pair<Float, Float>>();

			for (int i = 0; i < SngValV1.size(); i++) {
				SngValV.add(new Pair<Float, Float>((float) i + 1,
						(float) SngValV1.get(i)));
			}
			DistrStatH.put(StatVal.gsdSngVal, SngValV);
		}
		if (statFSet.contains(StatVal.gsdSngVec)) {

			SingularValueDecomposition<V, E> svd = new SingularValueDecomposition<V, E>(
					graph);
			Vector<Float> LeftV = svd.GetSngVec();
			Collections.sort(LeftV);

			Vector<Pair<Float, Float>> SngVec = new Vector<Pair<Float, Float>>();
			for (int i = 0; i < Math.min(10000, LeftV.size() / 2); i++) {
				if (LeftV.get(i) > 0) {
					SngVec.add(new Pair<Float, Float>((float) i, LeftV.get(i)));
				}
			}
			DistrStatH.put(StatVal.gsdSngVec, SngVec);
		}

	}

	void calcConnComp(DirectedGraph<V, E> graph) {
		Set<StatVal> s = new TreeSet<StatVal>();
		s.add(StatVal.gsdWcc);
		s.add(StatVal.gsdScc);
		calcConnComp(graph, s);
	}

	void calcConnComp(DirectedGraph<V, E> graph, Set<StatVal> statFSet) {
		if (statFSet.contains(StatVal.gsdWcc)) {
			System.out.printf("wcc ");
			ConnectivityInspector<V, E> ci = new ConnectivityInspector<V, E>(
					graph);
			Vector<Pair<Float, Float>> WccSzCntV = ci.getWccSizeCnt();
			DistrStatH.put(StatVal.gsdWcc, WccSzCntV);
		}
		if (statFSet.contains(StatVal.gsdScc)) {
			System.out.printf("scc ");
			StrongConnectivityInspector<V, E> ci = new StrongConnectivityInspector<V, E>(
					(DirectedGraph<V, E>) graph);
			Vector<Pair<Float, Float>> WccSzCntV = ci.getSccSizeCnt();
			DistrStatH.put(StatVal.gsdWcc, WccSzCntV);
		}
		if (statFSet.contains(StatVal.gsdWcc)
				|| statFSet.contains(StatVal.gsdScc)) {
			System.out.printf("\n");
		}
	}

	void calcDegDistr(DirectedGraph<V, E> graph) {
		Set<StatVal> s = new TreeSet<StatVal>();
		s.add(StatVal.gsdInDeg);
		s.add(StatVal.gsdOutDeg);
		calcDegDistr(graph, s);
	}

	void calcDegDistr(Graph<V, E> graph, Set<StatVal> statFSet) {
		// degree distribution
		if (statFSet.contains(StatVal.gsdOutDeg)
				|| statFSet.contains(StatVal.gsdOutDeg)) {
			System.out.printf("deg ");
		}
		if (statFSet.contains(StatVal.gsdInDeg)) {
			System.out.printf(" in ");

			Vector<Pair<Float, Float>> InDegV = cntInDeg(graph);
			DistrStatH.put(StatVal.gsdInDeg, InDegV);
		}
		if (statFSet.contains(StatVal.gsdOutDeg)) {
			System.out.printf(" out ");
			Vector<Pair<Float, Float>> OutDegV = cntOutDeg(graph);
			DistrStatH.put(StatVal.gsdInDeg, OutDegV);
		}
		if (statFSet.contains(StatVal.gsdOutDeg)
				|| statFSet.contains(StatVal.gsdOutDeg)) {
			System.out.printf("\n");
		}

	}

	void calcDiam(Graph<V, E> graph, boolean IsMxWcc) {
		Set<StatVal> s = new TreeSet<StatVal>();
		s.add(StatVal.gsvFullDiam);
		s.add(StatVal.gsvEffDiam);
		s.add(StatVal.gsdHops);
		s.add(StatVal.gsvEffWccDiam);
		s.add(StatVal.gsdWccHops);
		calcDiam(graph, s, IsMxWcc);
	}

	private void calcDiam(Graph<V, E> graph, Set<StatVal> statFSet,
			boolean IsMxWcc) {

		Long ExeTm = 0l;
		if (!IsMxWcc) {
			if (statFSet.contains(StatVal.gsvFullDiam)
					|| statFSet.contains(StatVal.gsvEffDiam)
					|| statFSet.contains(StatVal.gsdHops)) {
				System.out.printf("ANF diam %d runs ", NDiamRuns);
			}
			boolean Line = false;
			if (statFSet.contains(StatVal.gsvEffDiam)
					|| statFSet.contains(StatVal.gsdHops)) {
				Moment DiamMom = new Moment();
				ExeTm++;
				Vector<Pair<Integer, Float>> DistNbrsV = null;
				for (int r = 0; r < NDiamRuns; r++) {
					ApproximateNeighborhoodFunction<V, E> anf = new ApproximateNeighborhoodFunction<V, E>(
							graph, 32, 5, 0);
					DistNbrsV = anf.getGraphAnf(-1, false);
					DiamMom.add((float) anf.GetAnfEffDiam(DistNbrsV, 0.9d));
					System.out.printf(".");
				}
				DiamMom.def();
				setVal(StatVal.gsvEffDiam, DiamMom.getMean());
				setVal(StatVal.gsvEffDiamDev, DiamMom.getSDev());
				Vector<Pair<Float, Float>> HopsV = new Vector<Pair<Float, Float>>();
				for (Pair<Integer, Float> e : DistNbrsV) {
					HopsV.add(new Pair<Float, Float>((float) e.p1, e.p2));
				}

				DistrStatH.put(StatVal.gsdHops, HopsV);

				System.out.printf("  ANF-eff %.1f[%s]", DiamMom.getMean(),
						ExeTm);
				Line = true;
			}
			if (Line) {
				System.out.printf("\n");
			}
		} else {
			if (statFSet.contains(StatVal.gsvEffWccDiam)
					|| statFSet.contains(StatVal.gsdWccHops)) {
				System.out.printf("wcc diam ");
			}
			boolean Line = false;
			if (statFSet.contains(StatVal.gsvFullDiam)) {
				Moment DiamMom = new Moment();
				ExeTm++;
				for (int r = 0; r < NDiamRuns; r++) {
					BreadthDepthFirstSearch<V, E> bfsdfs = new BreadthDepthFirstSearch<V, E>(
							graph);
					int diam = bfsdfs.GetBfsFullDiam(1, false);
					DiamMom.add(diam);
					System.out.printf(".");
				}
				DiamMom.def();
				setVal(StatVal.gsvFullDiam, DiamMom.getMean());
				setVal(StatVal.gsvFullDiamDev, DiamMom.getSDev());
				System.out.printf("    BFS-full %g[%s]", DiamMom.getMean(),
						ExeTm);
				Line = true;
			}
			if (statFSet.contains(StatVal.gsvEffWccDiam)
					|| statFSet.contains(StatVal.gsdWccHops)) {
				Moment DiamMom = new Moment();
				ExeTm++;

				Vector<Pair<Integer, Float>> DistNbrsV = null;
				for (int r = 0; r < NDiamRuns; r++) {
					ApproximateNeighborhoodFunction<V, E> anf = new ApproximateNeighborhoodFunction<V, E>(
							graph, 32, 5, 0);
					DistNbrsV = anf.getGraphAnf(-1, false);
					DiamMom.add((float) anf.GetAnfEffDiam(DistNbrsV, 0.9d));
					System.out.printf(".");
				}

				DiamMom.def();
				setVal(StatVal.gsvEffWccDiam, DiamMom.getMean());
				setVal(StatVal.gsvEffWccDiamDev, DiamMom.getSDev());
				Vector<Pair<Float, Float>> WccHopsV = new Vector<Pair<Float, Float>>();
				for (int i = 0; i < DistNbrsV.size(); i++) {
					WccHopsV.add(new Pair<Float, Float>((float) DistNbrsV
							.get(i).p1, DistNbrsV.get(i).p2));
				}
				DistrStatH.put(StatVal.gsdWccHops, WccHopsV);
				System.out.printf("  ANF-wccEff %.1f[%s]", DiamMom.getMean(),
						ExeTm);
				Line = true;
			}
			if (Line) {
				System.out.printf("\n");
			}
		}
	}

	private void calcBasicStat(Graph<V, E> graph, boolean IsMxWcc) {
		Set<StatVal> s = new TreeSet<StatVal>();
		s.add(StatVal.gsvBiDirEdges);
		s.add(StatVal.gsvWccBiDirEdges);
		calcBasicStat(graph, s, IsMxWcc);
	}

	private void calcBasicStat(Graph<V, E> graph, Set<StatVal> statFSet,
			boolean IsMxWcc) {
		if (!IsMxWcc) {
			// gsvNodes, gsvZeroNodes, gsvNonZNodes, gsvSrcNodes, gsvDstNodes,
			// gsvEdges, gsvUniqEdges, gsvBiDirEdges
			System.out.printf("basic wcc...");
			final int Nodes = graph.vertexSet().size();
			setVal(StatVal.gsvNodes, Float.valueOf(Nodes));
			setVal(StatVal.gsvZeroNodes, Float.valueOf(cntDegNodes(graph, 0)));
			setVal(StatVal.gsvNonZNodes, Nodes - getVal(StatVal.gsvZeroNodes));
			setVal(StatVal.gsvSrcNodes,
					(float) (Nodes - cntOutDegNodes(graph, 0)));
			setVal(StatVal.gsvDstNodes,
					(float) (Nodes - cntInDegNodes(graph, 0)));
			setVal(StatVal.gsvEdges, (float) graph.edgeSet().size());
			if (!(graph instanceof Multigraph)) {
				setVal(StatVal.gsvUniqEdges, (float) graph.edgeSet().size());
			} else {
				setVal(StatVal.gsvUniqEdges, (float) cntUniqDirEdges(graph));
			}
			if (statFSet.contains(StatVal.gsvBiDirEdges) == true) {
				if (graph instanceof DirectedGraph) {
					setVal(StatVal.gsvBiDirEdges,
							(float) cntUniqBiDirEdges(graph));
				} else {
					setVal(StatVal.gsvUniqEdges, getVal(StatVal.gsvEdges));
				}
			}
			System.out.printf("\n");
		} else {
			// gsvWccNodes, gsvWccSrcNodes, gsvWccDstNodes, gsvWccEdges,
			// gsvWccUniqEdges, gsvWccBiDirEdges
			System.out.printf("basic...");
			final int Nodes = graph.vertexSet().size();
			setVal(StatVal.gsvWccNodes, (float) Nodes);
			setVal(StatVal.gsvWccSrcNodes,
					(float) (Nodes - cntOutDegNodes(graph, 0)));
			setVal(StatVal.gsvWccDstNodes,
					(float) (Nodes - cntInDegNodes(graph, 0)));
			setVal(StatVal.gsvWccEdges, (float) graph.edgeSet().size());
			if (!(graph instanceof Multigraph)) {
				setVal(StatVal.gsvWccUniqEdges, (float) graph.edgeSet().size());
			} else {
				setVal(StatVal.gsvWccUniqEdges, (float) cntUniqDirEdges(graph));
			}
			if (statFSet.contains(StatVal.gsvBiDirEdges)) {
				if (graph instanceof DirectedGraph) {
					setVal(StatVal.gsvWccBiDirEdges,
							(float) cntUniqBiDirEdges(graph));
				} else {
					setVal(StatVal.gsvUniqEdges, getVal(StatVal.gsvEdges));
				}
			}
			System.out.printf("\n");
		}
	}

	public int cntUniqBiDirEdges(Graph<V, E> graph) {

		if (!(graph instanceof DirectedGraph)) {
			// then every edge is bi-directional
			return cntUniqUndirEdges((UndirectedGraph<V, E>) graph);
		}

		DirectedGraph<V, E> dg = (DirectedGraph<V, E>) graph;
		int cnt = 0;
		for (V v : dg.vertexSet()) {

			for (E e : dg.outgoingEdgesOf(v)) {
				final V t = graph.getEdgeTarget(e);

				if (dg.containsEdge(t, v)) {
					cnt++;
				}
			}
		}
		return cnt;
	}

	private int cntUniqUndirEdges(UndirectedGraph<V, E> graph) {
		Set<E> nbrSet = new HashSet<E>();
		int cnt = 0;
		for (E e : graph.edgeSet()) {
			nbrSet.add(e);
		}
		cnt += nbrSet.size();
		return cnt;
	}

	public int cntUniqDirEdges(Graph<V, E> graph) {
		Set<E> nbrSet = new HashSet<E>();
		int cnt = 0;
		for (E e : graph.edgeSet()) {
			nbrSet.add(e);
		}
		cnt += nbrSet.size();
		return cnt;
	}

	private int cntInDegNodes(Graph<V, E> graph, int deg) {
		int cnt = 0;
		if (graph instanceof DirectedGraph) {
			for (V v : graph.vertexSet()) {
				if (((DirectedGraph<V, E>) graph).inDegreeOf(v) == deg) {
					cnt++;
				}
			}
		} else {
			for (V v : graph.vertexSet()) {
				if (graph.edgesOf(v).size() == deg) {
					cnt++;
				}
			}
		}
		return cnt;
	}

	public int cntOutDegNodes(Graph<V, E> graph, int deg) {
		int cnt = 0;
		if (graph instanceof DirectedGraph) {
			for (V v : graph.vertexSet()) {
				if (((DirectedGraph<V, E>) graph).outDegreeOf(v) == deg) {
					cnt++;
				}
			}
		} else {
			for (V v : graph.vertexSet()) {
				if (graph.edgesOf(v).size() == deg) {
					cnt++;
				}
			}
		}
		return cnt;
	}

	public Integer cntDegNodes(Graph<V, E> graph, int deg) {
		int cnt = 0;
		for (V v : graph.vertexSet()) {
			if (graph.edgesOf(v).size() == deg) {
				cnt++;
			}
		}
		return cnt;
	}

	private Vector<Pair<Float, Float>> cntInDeg(Graph<V, E> graph) {
		Vector<Pair<Float, Float>> DegToCntV = new Vector<Pair<Float, Float>>();
		Hashtable<Integer, Integer> DegToCntH = new Hashtable<Integer, Integer>();
		for (V v : graph.vertexSet()) {
			if (graph instanceof DirectedGraph) {
				DirectedGraph<V, E> dg = (DirectedGraph<V, E>) graph;
				if (DegToCntH.containsKey(dg.incomingEdgesOf(v))) {
					DegToCntH.put(dg.inDegreeOf(v),
							DegToCntH.get(dg.inDegreeOf(v)) + 1);
				} else {
					DegToCntH.put(dg.inDegreeOf(v), 1);
				}
			} else {
				if (DegToCntH.containsKey(graph.edgesOf(v))) {
					DegToCntH.put(graph.edgesOf(v).size(),
							DegToCntH.get(graph.edgesOf(v).size()) + 1);
				} else {
					DegToCntH.put(graph.edgesOf(v).size(), 1);
				}
			}

		}
		for (Entry<Integer, Integer> e : DegToCntH.entrySet()) {
			DegToCntV.add(new Pair<Float, Float>((float) e.getKey(), (float) e
					.getValue()));
		}
		Collections.sort(DegToCntV);
		return DegToCntV;
	}

	private Vector<Pair<Float, Float>> cntOutDeg(Graph<V, E> graph) {
		Vector<Pair<Float, Float>> DegToCntV = new Vector<Pair<Float, Float>>();
		Hashtable<Integer, Integer> DegToCntH = new Hashtable<Integer, Integer>();
		for (V v : graph.vertexSet()) {
			if (graph instanceof DirectedGraph) {
				DirectedGraph<V, E> dg = (DirectedGraph<V, E>) graph;
				if (DegToCntH.containsKey(dg.outgoingEdgesOf(v))) {
					DegToCntH.put(dg.outDegreeOf(v),
							DegToCntH.get(dg.outgoingEdgesOf(v)) + 1);
				} else {
					DegToCntH.put(dg.outDegreeOf(v), 1);
				}
			} else {
				if (DegToCntH.containsKey(graph.edgesOf(v))) {
					DegToCntH.put(graph.edgesOf(v).size(),
							DegToCntH.get(graph.edgesOf(v).size()) + 1);
				} else {
					DegToCntH.put(graph.edgesOf(v).size(), 1);
				}
			}

		}
		for (Entry<Integer, Integer> e : DegToCntH.entrySet()) {
			DegToCntV.add(new Pair<Float, Float>((float) e.getKey(), (float) e
					.getValue()));
		}
		Collections.sort(DegToCntV);
		return DegToCntV;
	}

	public void setVal(StatVal key, Float nodes) {
		ValStatH.put(key, nodes);
	}

	public Float getVal(StatVal key) {
		return ValStatH.get(key);
	}
}
