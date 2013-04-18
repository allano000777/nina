package edu.nd.nina.alg;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.logging.Logger;

import edu.nd.nina.Graph;
import edu.nd.nina.structs.Pair;
import edu.nd.nina.traverse.BreadthFirstIterator;
import edu.nd.nina.util.GraphUtil;

/**
 * Executes breadth first search operations (based on some SNAP functions)
 * 
 * @author Tim Weninger
 * 
 * @param <V>
 *            Node type, must extend Comparable<V>
 * @param <E>
 *            Edge type
 */
public class BreadthFirstSearch<V extends Comparable<V>, E> {
	
	private static Logger logger = Logger.getLogger(BreadthFirstSearch.class.getName());

	private Map<V, Integer> hopMap = null;

	/**
	 * Constructor runs the breadth first search. Warning - this procedure can
	 * take a long time to compute even for moderately sized graphs
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param startVertex
	 *            Node to begin searching from
	 */
	public BreadthFirstSearch(Graph<V, E> graph, V startVertex) {
		if (!graph.containsVertex(startVertex)) {
			throw new IllegalArgumentException(
					"graph must contain the start vertex");
		}

		BreadthFirstIterator<V, E> iter = new BreadthFirstIterator<V, E>(graph,
				startVertex);

		while (iter.hasNext()) {
			iter.next();
		}

		hopMap = iter.getHopMap();
	}

	/**
	 * Get the number of nodes at a given distance from startVertex. Warning -
	 * this procedure can take a long time to compute even for moderately sized
	 * graphs
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param Node
	 *            to begin searching from
	 * @return Vector<Pair<Distance, Count>>
	 */
	public static <V extends Comparable<V>, E> Vector<Pair<Integer, Integer>> getNodesAtHops(
			final Graph<V, E> graph, final V startVertex) {
		Vector<Pair<Integer, Integer>> HopCntV = new Vector<Pair<Integer, Integer>>();

		BreadthFirstSearch<V, E> bfs = new BreadthFirstSearch<V, E>(graph,
				startVertex);
		Hashtable<Integer, Integer> HopCntH = new Hashtable<Integer, Integer>();
		for (Integer val : bfs.hopMap.values()) {
			if (!HopCntH.containsKey(val)) {
				HopCntH.put(val, 1);
			} else {
				HopCntH.put(val, HopCntH.get(val) + 1);
			}

		}
		for (Entry<Integer, Integer> e : HopCntH.entrySet()) {
			HopCntV.add(new Pair<Integer, Integer>(e.getKey(), e.getValue()));
		}

		Collections.sort(HopCntV);

		return HopCntV;
	}

	/**
	 * Convenience method returns approximation of full diameter. Warning - this
	 * procedure can take a long time to compute even for moderately sized
	 * graphs
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param numberTestNodes
	 *            number of nodes to test in diameter, -1 tests the whole graph
	 *            (not approximate)
	 * @return diameter
	 */
	public static <V extends Comparable<V>, E> int getBfsFullDiam(
			final Graph<V, E> graph, final int numberTestNodes) {
		int[] fullDiameter = { 0 };
		double[] effectiveDiameter = { 0d };
		getBfsEffDiam(graph, numberTestNodes, effectiveDiameter, fullDiameter);
		return fullDiameter[0];
	}

	/**
	 * Convenience method returns approximation of 90% effective approximate
	 * diameter. Warning - this procedure can take a long time to compute even
	 * for moderately sized graphs
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param numberTestNodes
	 *            number of nodes to test in diameter, -1 tests the whole graph
	 *            (not approximate)
	 * @param effectiveDiameter
	 *            reference to return effective diameter
	 * @param fullDiameter
	 *            reference to return full diameter
	 * @return effective Diameter
	 */
	public static <V extends Comparable<V>, E> Map<V, Integer> getBfsEffDiam(
			final Graph<V, E> graph, final int numberTestNodes,
			double[] effectiveDiameter, int[] fullDiameter) {
		double[] averageDiameter = { 0d };
		effectiveDiameter[0] = -1;
		fullDiameter[0] = -1;
		return getBfsEffDiam(graph, numberTestNodes, effectiveDiameter,
				fullDiameter, averageDiameter);
	}

	/**
	 * Convenience method returns approximation of 90% effective approximate
	 * diameter. Warning - this procedure can take a long time to compute even
	 * for moderately sized graphs.
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param numberTestNodes
	 *            number of nodes to test in diameter, -1 tests the whole graph
	 *            (not approximate)
	 * @param effectiveDiameter
	 *            reference to return effective diameter
	 * @param fullDiameter
	 *            reference to return full diameter
	 * @param averageShortestPathLength
	 *            reference to return average shortest path length
	 * @return effective Diameter
	 */
	public static <V extends Comparable<V>, E> Map<V, Integer> getBfsEffDiam(
			final Graph<V, E> graph, int numberTestNodes,
			double[] effectiveDiameter, int[] fullDiameter,
			double[] averageShortestPathLength) {
		
		effectiveDiameter[0] = -1;
		fullDiameter[0] = -1;
		averageShortestPathLength[0] = -1;
		Hashtable<Integer, Float> distanceToCountH = new Hashtable<Integer, Float>();

		// shortest paths
		Vector<V> nodeV = new Vector<V>();
		nodeV.addAll(graph.vertexSet());
		Collections.shuffle(nodeV);

		if (numberTestNodes == -1) {
			numberTestNodes = graph.vertexSet().size();
		}

		BreadthFirstSearch<V, E> bfs = null;

		numberTestNodes = Math.min(numberTestNodes, graph.vertexSet()
				.size());
		int perc = -1;
		for (int tries = 0; tries < numberTestNodes; tries++) {
			if(perc < (tries/(float)numberTestNodes)*100){
				logger.info(++perc + "%");
			}
			final V v = nodeV.get(tries);
			bfs = new BreadthFirstSearch<V, E>(graph, v);
			for (Integer dist : bfs.hopMap.values()) {
				if (distanceToCountH.containsKey(dist)) {
					distanceToCountH.put(dist,
							(float) distanceToCountH.get(dist) + 1);
				} else {
					distanceToCountH.put(dist, 1f);
				}
			}
		}

		Vector<Pair<Integer, Float>> distanceToNeighborsPDF = new Vector<Pair<Integer, Float>>();
		double sumPathLength = 0, pathCount = 0;
		for (Entry<Integer, Float> e : distanceToCountH.entrySet()) {
			distanceToNeighborsPDF.add(new Pair<Integer, Float>(e.getKey(), e
					.getValue()));
			sumPathLength += e.getKey() * e.getValue();
			pathCount += e.getValue();
		}
		Collections.sort(distanceToNeighborsPDF);

		// effective diameter (90-th percentile)
		effectiveDiameter[0] = calcEffectiveDiameterPDF(distanceToNeighborsPDF,
				0.9);
		// approximate full diameter (max shortest path length over the sampled
		// nodes)
		fullDiameter[0] = distanceToNeighborsPDF.lastElement().p1;
		// average shortest path length
		averageShortestPathLength[0] = sumPathLength / pathCount;
		return bfs.hopMap;
	}

	/**
	 * Calculate the effective diameter from probability density function
	 * 
	 * @param distanceToNeighborsPDF
	 * @param percentile
	 * @return effective diameter
	 */
	public static double calcEffectiveDiameterPDF(
			Vector<Pair<Integer, Float>> distanceToNeighborsPDF,
			double percentile) {
		Vector<Pair<Integer, Float>> cdfV = GraphUtil
				.GetCdf(distanceToNeighborsPDF);
		return calcEffectiveDiameter(cdfV, percentile);
	}

	/**
	 * Calculate the effective diameter from cumulative density function
	 * 
	 * @param distanceToNeighborsCDFV
	 * @param percentile
	 * @return effective diameter
	 */
	public static double calcEffectiveDiameter(
			Vector<Pair<Integer, Float>> distanceToNeighborsCDFV,
			double percentile) {
		final double effectivePairs = percentile
				* distanceToNeighborsCDFV.lastElement().p2;
		int valN;
		for (valN = 0; valN < distanceToNeighborsCDFV.size(); valN++) {
			if (distanceToNeighborsCDFV.get(valN).p2 > effectivePairs) {
				break;
			}
		}
		if (valN >= distanceToNeighborsCDFV.size()) {
			return distanceToNeighborsCDFV.lastElement().p1;
		}
		if (valN == 0) {
			return 1;
		}
		// interpolate
		final double deltaNeighbors = distanceToNeighborsCDFV.get(valN).p2
				- distanceToNeighborsCDFV.get(valN - 1).p2;
		if (deltaNeighbors == 0) {
			return distanceToNeighborsCDFV.get(valN).p1;
		}
		return distanceToNeighborsCDFV.get(valN - 1).p1
				+ (effectivePairs - distanceToNeighborsCDFV.get(valN - 1).p2)
				/ deltaNeighbors;
	}

}
