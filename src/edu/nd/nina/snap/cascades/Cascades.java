package edu.nd.nina.snap.cascades;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import edu.nd.nina.DirectedGraph;
import edu.nd.nina.UndirectedGraph;
import edu.nd.nina.alg.ConnectivityInspector;
import edu.nd.nina.experimental.isomorphism.IntegerVertexFactory;
import edu.nd.nina.generate.RandomGraphGenerator;
import edu.nd.nina.graph.DefaultDirectedGraph;
import edu.nd.nina.graph.DefaultEdge;
import edu.nd.nina.graph.SimpleGraph;
import edu.nd.nina.graph.UndirectedSubgraph;
import edu.nd.nina.math.Randoms;
import edu.nd.nina.structs.Pair;

public class Cascades {
	
	private static Randoms r;
	private Integer seed = 0;
	
	public Cascades() {
		r = new Randoms(seed);
	}
	
	/**
	 * simulate SI model cascade using infection probability Beta until the
	 * cascade reaches size CascSz
	 * 
	 * @param g
	 * @param Beta
	 * @param CascSz
	 * @param NIdInfTmH
	 * @return
	 */
	DirectedGraph<Integer, DefaultEdge> runSICascade(UndirectedSubgraph<Integer, DefaultEdge> g,
			final double Beta, final int CascSz,
			Hashtable<Integer, Integer> NIdInfTmH) {
		DirectedGraph<Integer, DefaultEdge> Casc = new DefaultDirectedGraph<Integer, DefaultEdge>(
				DefaultEdge.class);
		final int StartId = g.randomVertex(r);
		Casc.addVertex(StartId);
		NIdInfTmH.put(StartId, NIdInfTmH.size());
		for (int X = 0; X < 10 * CascSz; X++) {
			Integer[] CascNIdV = new Integer[Casc.vertexSet().size()];
			Casc.vertexSet().toArray(CascNIdV);
			for (Integer v : CascNIdV) {
				for (DefaultEdge e : g.edgesOf(v)) {
					if (Casc.containsVertex(g.getEdgeTarget(e))) {
						continue;
					}
					if (r.GetUniDev() < Beta) {
						Integer t = g.getEdgeTarget(e);
						if (t.equals(v)) {
							t = g.getEdgeSource(e);
						}

						Casc.addVertex(t);
						NIdInfTmH.put(t, NIdInfTmH.size());
						Casc.addEdge(v, t);
						if (Casc.vertexSet().size() == CascSz) {
							return Casc;
						}
					}
				}
			}
		}
		return Casc;
	}

	/**
	 * network cascade: add spurious edges for more details see
	 * "Correcting for Missing Data in Information Cascades" by E. Sadikov, M.
	 * Medina, J. Leskovec, H. Garcia-Molina. WSDM, 2011
	 * 
	 * @param g
	 * @param Casc
	 * @param NIdTmH
	 * @return
	 */
	DirectedGraph<Integer, DefaultEdge> addSpuriousEdges(
			final UndirectedSubgraph<Integer, DefaultEdge> g,
			final DirectedGraph<Integer, DefaultEdge> Casc,
			Hashtable<Integer, Integer> NIdTmH) {
		Vector<Pair<Integer, Integer>> EdgeV = new Vector<Pair<Integer, Integer>>();
		for (Integer GNI : Casc.vertexSet()) {

			final int Tm = NIdTmH.get(GNI);
			for (DefaultEdge e : g.edgesOf(GNI)) {
				Integer dst = g.getEdgeTarget(e);
				if (dst.equals(GNI)) {
					dst = g.getEdgeSource(e);
				}

				if (NIdTmH.containsKey(dst)
						&& Tm < NIdTmH.get(dst)
						&& !Casc.containsEdge(GNI, dst)) {
					EdgeV.add(new Pair<Integer, Integer>(GNI, dst));
				}
			}
		}

		DirectedGraph<Integer, DefaultEdge> NetCasc = new DefaultDirectedGraph<Integer, DefaultEdge>(
				DefaultEdge.class);
		//copy Casc to NetCasc
		for(Integer v : Casc.vertexSet()){
			NetCasc.addVertex(v);
		}
		for(DefaultEdge e : Casc.edgeSet()){
			NetCasc.addEdge(Casc.getEdgeSource(e), Casc.getEdgeTarget(e));
		}
		
		for (int e = 0; e < EdgeV.size(); e++) {
			NetCasc.addEdge(EdgeV.get(e).p1, EdgeV.get(e).p2);
		}
		return NetCasc;
	}
	
	
	
	

	public static void main(String[] args) {

		Long exeTm = System.currentTimeMillis();
		try {
			final String InFNm = "demo"; // Input undirected graph
			final String OutFNm = "influence_demo"; // Output file name prefix
			final double Beta = 0.1; // Beta (infection (i.e., cascade
										// propagation) probability)
			// load
			System.out.printf("Loading %s...", InFNm);
			UndirectedGraph<Integer, DefaultEdge> ug = new SimpleGraph<Integer, DefaultEdge>(DefaultEdge.class);
			if (InFNm == "demo") {
				RandomGraphGenerator<Integer, DefaultEdge> rgg = new RandomGraphGenerator<Integer, DefaultEdge>(
						100, 200);
				Map<String, Integer> resultMap = new HashMap<String, Integer>();
				rgg.generateGraph(ug, new IntegerVertexFactory(), resultMap);
			} else {
				// Graph = TSnap::LoadEdgeList<PUNGraph>(InFNm);
			}
			System.out.printf("nodes:%d  edges:%d\n", ug.vertexSet().size(), ug
					.edgeSet().size());

			// Simulate SI model
			UndirectedSubgraph<Integer, DefaultEdge> g = ConnectivityInspector.getMaxWcc(ug);
			boolean DivByM = true;
			Cascades c = new Cascades();
			CascadeStatistics cascStat = new CascadeStatistics();
			
			System.out.printf("\nGraph:%s -- Beta: %g\n", OutFNm, Beta);
			for (int Run = 0; Run < 10; Run++) { // number of runs
				Hashtable<Integer, Integer> NIdInfTmH = new Hashtable<Integer, Integer>();
				// incluence cascade
				DirectedGraph<Integer, DefaultEdge> InfCasc = c.runSICascade(g, Beta,
						100, NIdInfTmH);
				// min cascade size
				if (InfCasc.vertexSet().size() < 10) {
					System.out.printf(".");
					continue;
				}
				// network cascade
				DirectedGraph<Integer, DefaultEdge> NetCasc = c.addSpuriousEdges(g,
						InfCasc, NIdInfTmH);
				// sample the cascade
				cascStat.sampleCascade(InfCasc, NetCasc, NIdInfTmH, 0.1, 10,
						DivByM, r); // div-by-M
				System.out.printf(".");
			}
			cascStat.plotAll(String
					.format("%s-B%03d", OutFNm, (int)( 100 * Beta)), String
					.format("%s N=%d  E=%d  Beta=%g", OutFNm, g.vertexSet()
							.size(), g.edgeSet().size(), Beta), DivByM);

		} catch (Exception e) {			
			System.err.printf("\nrun time: %s (%s)\n",
					System.currentTimeMillis() - exeTm,
					System.currentTimeMillis());
			e.printStackTrace();
		}
	}
}
