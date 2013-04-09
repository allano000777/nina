package edu.nd.nina.snap.agm;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import edu.nd.nina.UndirectedGraph;
import edu.nd.nina.graph.ClassBasedEdgeFactory;
import edu.nd.nina.graph.DefaultEdge;
import edu.nd.nina.graph.SimpleGraph;
import edu.nd.nina.math.Randoms;
import edu.nd.nina.structs.Pair;

public class AGM {

	public static UndirectedGraph<Integer, DefaultEdge> generateAGM(
			Vector<Vector<Integer>> cmtyVV, final Double densityCoef,
			final Double scaleCoef, Randoms rnd) {
		Vector<Float> cProbV = new Vector<Float>();
		float prob;
		for (int i = 0; i < cmtyVV.size(); i++) {
			prob = (float) (scaleCoef * Math.pow(
					((double) cmtyVV.get(i).size()), -densityCoef));
			if (prob > 1.0) {
				prob = 1;
			}
			cProbV.add(prob);
		}
		return generateAGM(cmtyVV, cProbV, rnd);
	}
	
	private static UndirectedGraph<Integer, DefaultEdge> generateAGM(
			final Vector<Vector<Integer>> cmtyVV, Vector<Float> cProbV, Randoms rnd) {
		return generateAGM(cmtyVV, cProbV, rnd, -1d);
	}

	private static UndirectedGraph<Integer, DefaultEdge> generateAGM(
			final Vector<Vector<Integer>> cmtyVV, Vector<Float> cProbV,
			Randoms rnd, final Double pNoCom) {

		UndirectedGraph<Integer, DefaultEdge> g = new SimpleGraph<Integer, DefaultEdge>(
				new ClassBasedEdgeFactory<Integer, DefaultEdge>(
						DefaultEdge.class));

		System.out.println("AGM begins");

		for (int i = 0; i < cmtyVV.size(); i++) {
			Vector<Integer> cmtyV = cmtyVV.get(i);
			for (int u = 0; u < cmtyV.size(); u++) {
				if (g.containsVertex(cmtyV.get(u)))
					continue;

				g.addVertex(cmtyV.get(u));
			}
			double prob = cProbV.get(i);
			RndConnectInsideCommunity(g, cmtyV, prob, rnd);
		}
		if (pNoCom > 0.0) { // if we want to connect nodes that do not share any
							// community
			Set<Integer> nIDS = new HashSet<Integer>();
			for (int c = 0; c < cmtyVV.size(); c++) {
				for (int u = 0; u < cmtyVV.get(c).size(); u++) {
					nIDS.add(cmtyVV.get(c).get(u));
				}
			}
			Vector<Integer> nIDV = new Vector<Integer>();
			nIDV.addAll(nIDS);

			RndConnectInsideCommunity(g, nIDV, pNoCom, rnd);
		}
		System.out.println("AGM completed (" + g.vertexSet().size() + " nodes "
				+ g.edgeSet().size() + " edges)");
		// g.Defrag();
		return g;
	}

	private static void RndConnectInsideCommunity(
			UndirectedGraph<Integer, DefaultEdge> g,
			final Vector<Integer> cmtyV, final Double prob, Randoms rnd) {

		int cNodes = cmtyV.size(), cEdges;

		if (cNodes < 20) {
			cEdges = rnd.getBinomialDeviance(prob, cNodes * (cNodes - 1) / 2)
					.intValue();
		} else {
			cEdges = (int) (prob * cNodes * (cNodes - 1) / 2);
		}
		Set<Pair<Integer, Integer>> newEdgeSet = new HashSet<Pair<Integer, Integer>>(cEdges);
		for (int edge = 0; edge < cEdges;) {
			int srcNId = cmtyV.get(rnd.GetUniDevInt(cNodes));
			int dstNId = cmtyV.get(rnd.GetUniDevInt(cNodes));
			if (srcNId > dstNId) {
				// swap
				int x = srcNId;
				srcNId = dstNId;
				dstNId = x;
			}
			Pair<Integer, Integer> p = new Pair<Integer, Integer>(srcNId, dstNId);
			if (srcNId != dstNId && !newEdgeSet.contains(p)) {
				newEdgeSet.add(p);
				g.addEdge(srcNId, dstNId);
				edge++;
			}
		}
	}
}
  
