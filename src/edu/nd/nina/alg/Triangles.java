package edu.nd.nina.alg;

import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import edu.nd.nina.Graph;
import edu.nd.nina.Graphs;
import edu.nd.nina.structs.Pair;
import edu.nd.nina.structs.Triple;

/**
 * Triangles and clustering coefficient
 * 
 * @author Tim Weninger
 * 
 * @param <V>
 *            Vertex type, must extend Comparable<V>
 * @param <E>
 *            Edge type
 */
public class Triangles<V extends Comparable<V>, E> {

	/**
	 * Computes the distribution of average clustering coefficient as defined in
	 * Watts and Strogatz, Collective dynamics of 'small-world' networks.
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param sampleNodes
	 *            Number of nodes to sample. -1 to process all nodes.
	 * @return Triple<AverageCcF, closedTriangles, openTriangles>
	 */
	public static <V extends Comparable<V>, E> Triple<Float, Integer, Integer> getClsuteringCoefficient(
			Graph<V, E> graph, int sampleNodes) {
		Integer closedTrianlges;
		Integer openTriangles;
		Vector<Triple<V, Integer, Integer>> nodeTriangleCount = getTriangles(
				graph, sampleNodes);
		Hashtable<Integer, Pair<Float, Float>> degreeSumCount = new Hashtable<Integer, Pair<Float, Float>>();
		double sumCcf = 0.0;
		int closedTriads = 0;
		int openTriads = 0;
		for (int i = 0; i < nodeTriangleCount.size(); i++) {
			final int D = nodeTriangleCount.get(i).v2
					+ nodeTriangleCount.get(i).v3;
			final double ccf = D != 0 ? nodeTriangleCount.get(i).v2
					/ (double) D : 0.0;
			closedTriads += nodeTriangleCount.get(i).v2;
			openTriads += nodeTriangleCount.get(i).v3;
			Pair<Float, Float> sumCount = new Pair<Float, Float>((float) ccf,
					1f);

			sumCcf += ccf;
			degreeSumCount.put(graph.edgesOf(nodeTriangleCount.get(i).v1)
					.size(), sumCount);
		}

		// get average clustering coefficient for each degree
		Vector<Pair<Float, Float>> degToCcfV = new Vector<Pair<Float, Float>>();
		for (Entry<Integer, Pair<Float, Float>> e : degreeSumCount.entrySet()) {
			degToCcfV.add(new Pair<Float, Float>((float) e.getKey(), e
					.getValue().p1 / e.getValue().p2));
		}

		closedTrianlges = closedTriads / 3; // each triad is counted 3 times
		openTriangles = openTriads;
		Collections.sort(degToCcfV);
		return new Triple<Float, Integer, Integer>(
				(float) (sumCcf / (float) nodeTriangleCount.size()),
				closedTrianlges, openTriangles);

	}

	/**
	 * Count unique connected triples of nodes all nodes
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param sampleNodes
	 *            Number of nodes to sample. -1 to process all nodes.
	 * @return Vector<Triple<Node, closedTrianlges, openTrianlges>>
	 */
	public static <V extends Comparable<V>, E> Vector<Triple<V, Integer, Integer>> getTriangles(
			Graph<V, E> graph, int sampleNodes) {
		Set<V> nbrSet = new HashSet<V>();
		Vector<V> nodeV = new Vector<V>();

		nodeV.addAll(graph.vertexSet());
		Collections.shuffle(nodeV);

		if (sampleNodes == -1) {
			sampleNodes = graph.vertexSet().size();
		}

		Vector<Triple<V, Integer, Integer>> nodeTriangleCount = new Vector<Triple<V, Integer, Integer>>();

		for (int node = 0; node < sampleNodes; node++) {
			V v = nodeV.get(node);
			if (graph.edgesOf(v).size() < 2) {
				// zero triangles
				nodeTriangleCount.add(new Triple<V, Integer, Integer>(v, 0, 0));
				continue;
			}
			// find neighborhood
			nbrSet.clear();
			nbrSet.addAll(Graphs.neighborListOf(graph, v));

			// count connected neighbors
			int openCount = 0, closedCount = 0;
			int i = 0;
			for (V SrcNode : nbrSet) {
				int j = 0;
				for (V dstNId : nbrSet) {
					if (j <= i) {

						if (graph.containsEdge(SrcNode, dstNId)
								|| graph.containsEdge(dstNId, SrcNode)) {
							closedCount++;
						} // is edge
						else {
							openCount++;
						}
					}
					j++;
				}
				i++;
			}
			assert (2 * (openCount + closedCount) == nbrSet.size()
					* (nbrSet.size() - 1));
			nodeTriangleCount.add(new Triple<V, Integer, Integer>(v,
					closedCount, openCount));
		}
		return nodeTriangleCount;
	}

	/**
	 * For each node count how many triangles in which it participates
	 * 
	 * @param graph
	 *            Graph Snapshot
	 * @return Vector<Pair<NumTriangles, Count>>
	 */
	public static <V extends Comparable<V>, E> Vector<Pair<Integer, Integer>> getTriangleParticipation(
			Graph<V, E> graph) {
		Vector<Pair<Integer, Integer>> TriangleCntV = new Vector<Pair<Integer, Integer>>();
		Hashtable<Integer, Integer> TriangleCntH = new Hashtable<Integer, Integer>();
		for (V v : graph.vertexSet()) {
			final int triangles = getTrianglesForNode(graph, v);
			if (TriangleCntH.containsKey(triangles)) {
				TriangleCntH.put(triangles, TriangleCntH.get(triangles) + 1);
			} else {
				TriangleCntH.put(triangles, 1);
			}

		}
		for (Entry<Integer, Integer> e : TriangleCntH.entrySet()) {
			TriangleCntV.add(new Pair<Integer, Integer>(e.getKey(), e
					.getValue()));
		}
		Collections.sort(TriangleCntV);
		return TriangleCntV;
	}

	/**
	 * Returns number of undirected triangles in which a node participates
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param v
	 *            Node to consider
	 * @return Number of triangles
	 */
	private static <V extends Comparable<V>, E> int getTrianglesForNode(
			final Graph<V, E> graph, final V v) {

		int closedTriangles = 0;
		if (graph.edgesOf(v).size() < 2) {
			return 0;
		}
		// find neighborhood

		Set<V> nbrSet = new HashSet<V>(graph.edgesOf(v).size());
		nbrSet.addAll(Graphs.neighborListOf(graph, v));

		// count connected neighbors
		int i = 0;
		for (V srcNode : nbrSet) {
			int j = 0;
			for (V dstNode : nbrSet) {
				if (j <= i) {
					if (graph.containsEdge(srcNode, dstNode)
							|| graph.containsEdge(dstNode, srcNode)) {
						closedTriangles++;
					} // is edge
				}
				j++;
			}
			i++;
		}

		return closedTriangles;
	}
}
