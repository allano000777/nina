package edu.nd.nina.alg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import edu.nd.nina.DirectedGraph;
import edu.nd.nina.Graph;
import edu.nd.nina.Graphs;
import edu.nd.nina.UndirectedGraph;
import edu.nd.nina.graph.Multigraph;
import edu.nd.nina.math.Moment;
import edu.nd.nina.math.Randoms;
import edu.nd.nina.structs.Pair;
import edu.nd.nina.structs.Triple;

/**
 * Statistics of a snapshot of a graph (based on gstat.h/cpp in SNAP)
 * 
 * @author Tim Weninger
 * @date 4/17/2013
 * 
 * @param <V>
 *            Vertex type, must extend Comparable<V>
 * @param <E>
 *            Edge type
 */
public class CalculateStatistics<V extends Comparable<V>, E> {

	private static Logger logger = Logger.getLogger(CalculateStatistics.class
			.getName());

	/**
	 * Number of tests to do when estimating graph diameter
	 */
	private static final Integer NDiamRuns = 10;
	/**
	 * Number of single values in SVD to return
	 */
	private static final Integer TakeSngVals = 100;

	/**
	 * Calculate all statistics. WARNING, this can take a long time for even
	 * moderately sized graphs
	 * 
	 * @param graph
	 *            Single snapshot of a graph
	 * @param wcc
	 *            true if graph is weakly connected component
	 * @param valStatH
	 *            table of single results
	 * @param distrStatH
	 *            table of result distributions
	 */
	public static <V extends Comparable<V>, E> void calcStats(
			Graph<V, E> graph, boolean wcc, Hashtable<StatVal, Float> valStatH,
			Hashtable<StatVal, Vector<Pair<Float, Float>>> distrStatH) {

		System.out.printf("GraphStatistics:  G(%d, %d)\n", graph.vertexSet()
				.size(), graph.edgeSet().size());
		long FullTm = System.currentTimeMillis();

		if (!wcc) {
			calcBasicStat(ConnectivityInspector.getMaxWcc(graph), true,
					valStatH);
		} else {
			calcBasicStat(graph, false, valStatH);
		}
		// diameter
		calcDiameter(graph, 100, valStatH, distrStatH);
		// degrees
		calcDegreeDistribution(graph, distrStatH);
		// components
		calcConnectedComponents(graph, distrStatH);
		// spectral
		calcSpectral(graph, -1, distrStatH);
		// clustering coefficient
		calcClusteringCoefficient(graph, valStatH);

		calcTriangleParticipation(graph, distrStatH);

		System.out.printf("  [%s]\n", System.currentTimeMillis() - FullTm);

	}

	/**
	 * Calculates the triangle participation
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param distrStatH
	 *            table of result distributions
	 */
	public static <V extends Comparable<V>, E> void calcTriangleParticipation(
			Graph<V, E> graph,
			Hashtable<StatVal, Vector<Pair<Float, Float>>> distrStatH) {
		logger.info("Calculate Triangle Participation");
		Vector<Pair<Float, Float>> triangleCountV = new Vector<Pair<Float, Float>>();

		Vector<Pair<Integer, Integer>> countV = Triangles
				.getTriangleParticipation(graph);
		for (int i = 0; i < countV.size(); i++) {
			triangleCountV.add(new Pair<Float, Float>((float) countV.get(i).p1,
					(float) countV.get(i).p2));
		}
		distrStatH.put(StatVal.gsdTriadPart, triangleCountV);
	}

	/**
	 * Calculates the clustering coefficient of the graph
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param valStatH
	 *            table of result
	 */
	public static <V extends Comparable<V>, E> void calcClusteringCoefficient(
			Graph<V, E> graph, Hashtable<StatVal, Float> valStatH) {
		calcClusteringCoefficient(graph, -1, valStatH);
	}

	/**
	 * Calculates the clustering coefficient of the graph
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param sampleNodes
	 *            Number of nodes to Sample, -1 to process all nodes
	 * @param valStatH
	 *            table of result
	 */
	public static <V extends Comparable<V>, E> void calcClusteringCoefficient(
			Graph<V, E> graph, int sampleNodes,
			Hashtable<StatVal, Float> valStatH) {

		Triple<Float, Integer, Integer> t = Triangles.getClusteringCoefficient(
				graph, sampleNodes);
		valStatH.put(StatVal.gsvClustCf, t.v1);
		valStatH.put(StatVal.gsvOpenTriads, (float) t.v3);
		valStatH.put(StatVal.gsvClosedTriads, (float) t.v2);
	}

	/**
	 * Calculates the spectral properties of the graph
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param distrStatH
	 *            table of result distributions
	 */
	public static <V extends Comparable<V>, E> void calcSpectral(
			Graph<V, E> graph, int singularValues,
			Hashtable<StatVal, Vector<Pair<Float, Float>>> distrStatH) {

		if (singularValues == -1) {
			singularValues = TakeSngVals;
		}

		// singular values, vectors
		singularValues = Math.min(singularValues, graph.vertexSet().size() / 2);

		Vector<Float> singularValues1 = SingularValueDecomposition
				.getSingularValues(graph, singularValues);
		Collections.sort(singularValues1);
		Vector<Pair<Float, Float>> singularValuesV = new Vector<Pair<Float, Float>>();

		for (int i = 0; i < singularValues1.size(); i++) {
			singularValuesV.add(new Pair<Float, Float>((float) i + 1,
					(float) singularValues1.get(i)));
		}
		distrStatH.put(StatVal.gsdSngVal, singularValuesV);

		Vector<Float> leftV = new Vector<Float>();
		Vector<Float> rightV = new Vector<Float>();
		SingularValueDecomposition
				.getSingularValuesVector(graph, leftV, rightV);
		Collections.sort(leftV);
		Collections.sort(rightV);

		Vector<Pair<Float, Float>> singularValuesVectorLeft = new Vector<Pair<Float, Float>>();
		for (int i = 0; i < Math.min(10000, leftV.size() / 2); i++) {
			if (leftV.get(i) > 0) {
				singularValuesVectorLeft.add(new Pair<Float, Float>((float) i,
						leftV.get(i)));
			}
		}
		distrStatH.put(StatVal.gsdSngVecLeft, singularValuesVectorLeft);

		Vector<Pair<Float, Float>> singularValuesVectorRight = new Vector<Pair<Float, Float>>();
		for (int i = 0; i < Math.min(10000, rightV.size() / 2); i++) {
			if (rightV.get(i) > 0) {
				singularValuesVectorRight.add(new Pair<Float, Float>((float) i,
						rightV.get(i)));
			}
		}
		distrStatH.put(StatVal.gsdSngVecRight, singularValuesVectorRight);

	}

	/**
	 * Calculates the connected components
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param distrStatH
	 *            table of result distributions
	 */
	public static <V extends Comparable<V>, E> void calcConnectedComponents(
			Graph<V, E> graph,
			Hashtable<StatVal, Vector<Pair<Float, Float>>> distrStatH) {

		logger.info("Calculate Weakly Connected Component");

		distrStatH.put(StatVal.gsdWcc,
				ConnectivityInspector.getWccSizeCount(graph));

		if (graph instanceof DirectedGraph) {
			logger.info("Calculate Strongly Connected Component");

			distrStatH.put(StatVal.gsdWcc, StrongConnectivityInspector
					.getSccSizeCnt((DirectedGraph<V, E>) graph));
		}

	}

	/**
	 * Calculates the degree distribution
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param distrStatH
	 *            table of result distributions
	 */
	public static <V extends Comparable<V>, E> void calcDegreeDistribution(
			Graph<V, E> graph,
			Hashtable<StatVal, Vector<Pair<Float, Float>>> distrStatH) {

		// degree distribution

		logger.info("Count Indegree");
		Vector<Pair<Float, Float>> indegreeV = countIndegree(graph);
		distrStatH.put(StatVal.gsdInDeg, indegreeV);

		logger.info("Count Outdegree");
		Vector<Pair<Float, Float>> outdegreeV = countOutdegree(graph);
		distrStatH.put(StatVal.gsdOutDeg, outdegreeV);
	}

	/**
	 * Calculates the degree distribution
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param distrStatH
	 *            table of result distributions
	 */
	public static <V extends Comparable<V>, E> void calcDegreeDistribution(
			Graph<V, E> graph,
			Hashtable<String, Vector<Pair<Float, Float>>> distrStatH,
			Set<Class<?>> types) {

		int i = 0;
		for (Class<?> t1 : types) {
			int j = 0;
			for (Class<?> t2 : types) {
				if (graph instanceof UndirectedGraph) {
					if (i < j)
						break;
				}
				logger.info("Count Indegree");
				Vector<Pair<Float, Float>> indegreeV = countIndegree(graph, t1,
						t2);
				distrStatH.put("inDegree: " + t1.getSimpleName() + "-" + t2.getSimpleName(), indegreeV);

				logger.info("Count Outdegree");
				Vector<Pair<Float, Float>> outdegreeV = countOutdegree(graph,
						t1, t2);
				distrStatH.put("outDegree: " + t1.getSimpleName() + "-" + t2.getSimpleName(), outdegreeV);
			}
		}
	}

	/**
	 * Calculate graph diameter
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param numTestNodes
	 *            Number of nodes to test diameter for, -1 to test all nodes
	 * @param statValH
	 *            table of result
	 * @param distrStatH
	 *            table of result distributions
	 */
	public static <V extends Comparable<V>, E> void calcDiameter(
			Graph<V, E> graph, int numTestNodes,
			Hashtable<StatVal, Float> statValH,
			Hashtable<StatVal, Vector<Pair<Float, Float>>> distrStatH) {

		logger.info("Calculate Diameter");

		Moment effectiveDiameterMom = new Moment();
		Moment fullDiameterMom = new Moment();
		Moment averageDiameterMom = new Moment();

		Map<V, Integer> hops = null;
		for (int r = 0; r < NDiamRuns; r++) {
			double[] effectiveDiameter = { 0d };
			int[] fullDiameter = { 0 };
			double[] averageDiameter = { 0d };
			logger.info("Diameter run: " + r);
			hops = BreadthFirstSearch.getBfsEffDiam(graph, numTestNodes,
					effectiveDiameter, fullDiameter, averageDiameter);

			effectiveDiameterMom.add((float) effectiveDiameter[0]);
			fullDiameterMom.add((float) fullDiameter[0]);
			averageDiameterMom.add((float) averageDiameter[0]);
		}
		effectiveDiameterMom.def();
		fullDiameterMom.def();
		averageDiameterMom.def();

		statValH.put(StatVal.gsvFullDiam, fullDiameterMom.getMax());
		statValH.put(StatVal.gsvFullDiamDev, fullDiameterMom.getSDev());
		statValH.put(StatVal.gsvEffDiam, effectiveDiameterMom.getMean());
		statValH.put(StatVal.gsvEffDiamDev, effectiveDiameterMom.getSDev());
		statValH.put(StatVal.gsvAvgDiam, averageDiameterMom.getMean());
		statValH.put(StatVal.gsvAvgDiamDev, averageDiameterMom.getSDev());

		Hashtable<Float, Float> hopCountH = new Hashtable<Float, Float>();
		Vector<Pair<Float, Float>> hopCountV = new Vector<Pair<Float, Float>>();
		for (Integer e : hops.values()) {
			if (!hopCountH.containsKey(Float.valueOf(e))) {
				hopCountH.put(Float.valueOf(e), 1f);
			} else {
				hopCountH.put((float) e, hopCountH.get(Float.valueOf(e)) + 1);
			}
		}

		for (Entry<Float, Float> e : hopCountH.entrySet()) {
			hopCountV.add(new Pair<Float, Float>(e.getKey(), e.getValue()));
		}
		Collections.sort(hopCountV);
		distrStatH.put(StatVal.gsdHops, hopCountV);
	}

	/**
	 * Calculate basic graph statistics
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param isMaxWcc
	 *            true if the graph is a WCC
	 * @param valStatH
	 *            table of results
	 */
	public static <V extends Comparable<V>, E> void calcBasicStat(
			Graph<V, E> graph, boolean isMaxWcc,
			Hashtable<String, Float> valStatH, Set<Class<?>> types) {
		int i = 0;
		for (Class<?> t1 : types) {
			int j = 0;
			for (Class<?> t2 : types) {
				if (graph instanceof UndirectedGraph) {
					if (i < j)
						break;
				}
				logger.info("Calculate Basic Statistics: " + t1.getSimpleName() + " - " + t2.getSimpleName());
				if (!isMaxWcc) {
					final int size = graph.getAllMatchingType(t1).size();
					valStatH.put("nodes: " + t1.getSimpleName() + "-" + t2.getSimpleName(), Float.valueOf(size));
					valStatH.put("zeroNodes: " + t1.getSimpleName() + "-" + t2.getSimpleName(),
							Float.valueOf(countNodesOfDegree(graph, 0, t1, t2)));
					valStatH.put("nonZeroNodes: " + t1.getSimpleName() + "-" + t2.getSimpleName(), size
							- valStatH.get("zeroNodes: " + t1.getSimpleName() + "-" + t2.getSimpleName()));
					valStatH.put(
							"srcNodes: " + t1.getSimpleName() + "-" + t2.getSimpleName(),
							(float) (size - countNodesOfOutdegree(graph, 0, t1,
									t2)));
					valStatH.put(
							"dstNodes: " + t1.getSimpleName() + "-" + t2.getSimpleName(),
							(float) (size - countNodesOfIndegree(graph, 0, t1,
									t2)));
					valStatH.put("edges: " + t1.getSimpleName() + "-" + t2.getSimpleName(),
							(float) countEdges(graph, t1, t2));
					if (!(graph instanceof Multigraph)) {
						valStatH.put("uniqEdges: " + t1.getSimpleName() + "-" + t2.getSimpleName(),
								valStatH.get("edges: " + t1.getSimpleName() + "-" + t2.getSimpleName()));
					} else {
						valStatH.put("uniqEdges: " + t1.getSimpleName() + "-" + t2.getSimpleName(),
								(float) countUniqueEdges(graph, t1, t2));
					}

					if (graph instanceof DirectedGraph) {
						valStatH.put(
								"bidiredges: " + t1 + "-" + t2,
								(float) countUniqueBidirectionalEdges(graph,
										t1, t2));
					} else {
						if ((graph instanceof Multigraph)) {
							valStatH.put("uniqEdges: " + t1 + "-" + t2,
									(float) countUniqueEdges(graph, t1, t2));
						}
					}
				} else {

					logger.info("Weakly Connected Component");
					final int size = graph.vertexSet().size();
					valStatH.put("wccNodes: " + t1 + "-" + t2,
							Float.valueOf(size));
					valStatH.put("wccZeroNodes: " + t1 + "-" + t2,
							Float.valueOf(countNodesOfDegree(graph, 0)));
					valStatH.put("wccNonZeroNodes: " + t1 + "-" + t2, size
							- valStatH.get("zeroNodes"));
					valStatH.put("wccSrcNodes: " + t1 + "-" + t2,
							(float) (size - countNodesOfOutdegree(graph, 0)));
					valStatH.put("wccDstNodes: " + t1 + "-" + t2,
							(float) (size - countNodesOfIndegree(graph, 0)));
					valStatH.put("wccEdges: " + t1 + "-" + t2, (float) graph
							.edgeSet().size());
					if (!(graph instanceof Multigraph)) {
						valStatH.put("wccUniqEdges: " + t1 + "-" + t2,
								(float) graph.edgeSet().size());
					} else {
						valStatH.put("wccUniqEdges: " + t1 + "-" + t2,
								(float) countUniqueEdges(graph));
					}

					if (graph instanceof DirectedGraph) {
						valStatH.put("wccBidiredges: " + t1 + "-" + t2,
								(float) countUniqueBidirectionalEdges(graph));
					} else {
						if ((graph instanceof Multigraph)) {
							valStatH.put("wccUniqEdges: " + t1 + "-" + t2,
									(float) countUniqueEdges(graph));
						}
					}
				}
			}
		}
	}

	/**
	 * Calculate basic graph statistics
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param isMaxWcc
	 *            true if the graph is a WCC
	 * @param valStatH
	 *            table of results
	 */
	public static <V extends Comparable<V>, E> void calcBasicStat(
			Graph<V, E> graph, boolean isMaxWcc,
			Hashtable<StatVal, Float> valStatH) {
		logger.info("Calculate Basic Statistics");
		if (!isMaxWcc) {
			final int size = graph.vertexSet().size();
			valStatH.put(StatVal.gsvNodes, Float.valueOf(size));
			valStatH.put(StatVal.gsvZeroNodes,
					Float.valueOf(countNodesOfDegree(graph, 0)));
			valStatH.put(StatVal.gsvNonZNodes,
					size - valStatH.get(StatVal.gsvZeroNodes));
			valStatH.put(StatVal.gsvSrcNodes,
					(float) (size - countNodesOfOutdegree(graph, 0)));
			valStatH.put(StatVal.gsvDstNodes,
					(float) (size - countNodesOfIndegree(graph, 0)));
			valStatH.put(StatVal.gsvEdges, (float) graph.edgeSet().size());
			if (!(graph instanceof Multigraph)) {
				valStatH.put(StatVal.gsvUniqEdges, (float) graph.edgeSet()
						.size());
			} else {
				valStatH.put(StatVal.gsvUniqEdges,
						(float) countUniqueEdges(graph));
			}

			if (graph instanceof DirectedGraph) {
				valStatH.put(StatVal.gsvBiDirEdges,
						(float) countUniqueBidirectionalEdges(graph));
			} else {
				valStatH.put(StatVal.gsvUniqEdges,
						(float) countUniqueEdges(graph));
			}
		} else {

			logger.info("Weakly Connected Component");
			final int size = graph.vertexSet().size();
			valStatH.put(StatVal.gsvWccNodes, (float) size);
			valStatH.put(StatVal.gsvWccSrcNodes,
					(float) (size - countNodesOfOutdegree(graph, 0)));
			valStatH.put(StatVal.gsvWccDstNodes,
					(float) (size - countNodesOfIndegree(graph, 0)));
			valStatH.put(StatVal.gsvWccEdges, (float) graph.edgeSet().size());
			if (!(graph instanceof Multigraph)) {
				valStatH.put(StatVal.gsvWccUniqEdges, (float) graph.edgeSet()
						.size());
			} else {
				valStatH.put(StatVal.gsvWccUniqEdges,
						(float) countUniqueEdges(graph));
			}
			if (graph instanceof DirectedGraph) {
				valStatH.put(StatVal.gsvWccBiDirEdges,
						(float) countUniqueBidirectionalEdges(graph));
			} else {
				valStatH.put(StatVal.gsvUniqEdges,
						valStatH.get(StatVal.gsvWccEdges));
			}
		}
	}

	/**
	 * Calculates a distribution of jaccard coefficients for all pairs that
	 * share a node
	 * 
	 * @param graph
	 * @return
	 */
	public static <V extends Comparable<V>, E> void calcJaccardCoefficient(
			Graph<V, E> graph, int sampleNodes, Class<?> startType,
			Hashtable<StatVal, Vector<Pair<Float, Float>>> distrStatH) {

		Vector<Pair<Float, Float>> x = new Vector<Pair<Float, Float>>();
		Randoms r = new Randoms();
		List<Pair<V, V>> newEdges = new ArrayList<Pair<V, V>>();
		// add random edges
		List<V> nodes = new ArrayList<V>(graph.vertexSet());
		Collections.shuffle(nodes);
		for (int i = 0, j=0; i < sampleNodes && j<nodes.size(); j++) {
			V v1 = nodes.get(j);
			if(!v1.getClass().equals(startType)){
				continue;
			}
			List<V> n = Graphs.neighborListOf(graph, v1);
			if(n.size() == 0) continue;
			V v2 = n.get(r.next(n.size()));
			n = Graphs.neighborListOf(graph, v2);
			if(n.remove(v1) != true) {
				assert(false);
			}
			if(n.size() == 0) continue;
			V v3 = n.get(r.next(n.size()));
			newEdges.add(new Pair<V, V>(v1, v3));
			i++;
		}

		//jacVals needs to be decreasing order
		float[] jacVals = { 0.512f, 0.256f, 0.128f, 0.064f, 0.032f, 0.016f,
				0.008f, 0.004f, 0.002f, 0.001f };
		for (float f : jacVals) {
			Float jccf = calcJaccardCoefficient(graph, f, newEdges);
			x.add(new Pair<Float, Float>(f, jccf));
		}

		distrStatH.put(StatVal.gsdJacCoef, x);
	}

	/**
	 * Calculates a distribution of jaccard coefficients for all pairs that
	 * share a node
	 * 
	 * @param graph
	 * @return
	 */
	public static <V extends Comparable<V>, E> Float calcJaccardCoefficient(
			Graph<V, E> graph, Float cutoff, List<Pair<V, V>> edges) {

		float i=0;
		
		for (Pair<V, V> p : edges) {
			float jaccard = calcJaccardCoefficient(graph, p.p1, p.p2);
			if (jaccard > cutoff) {				
				i++;
			}
		}
		return i/(float)edges.size();

	}
	
	public static <V extends Comparable<V>, E> void calcJaccardAssortativity(
			Graph<V, E> graph, int sampleNodes, Class<?> startType,
			Hashtable<StatVal, Vector<Pair<Float, Float>>> distrStatH) {

		Vector<Pair<Float, Float>> x = new Vector<Pair<Float, Float>>();
		Randoms r = new Randoms();
		List<Pair<V, V>> newEdges = new ArrayList<Pair<V, V>>();
		// add random edges
		List<V> nodes = new ArrayList<V>(graph.vertexSet());
		Collections.shuffle(nodes);
		for (int i = 0, j=0; i < sampleNodes && j<nodes.size(); j++) {
			V v1 = nodes.get(j);
			if(!v1.getClass().equals(startType)){
				continue;
			}
			List<V> n = Graphs.neighborListOf(graph, v1);
			if(n.size() == 0) continue;
			V v2 = n.get(r.next(n.size()));
			n = Graphs.neighborListOf(graph, v2);
			if(n.remove(v1) != true) {
				assert(false);
			}
			if(n.size() == 0) continue;
			V v3 = n.get(r.next(n.size()));
			newEdges.add(new Pair<V, V>(v1, v3));
			i++;
		}

		//jacVals needs to be decreasing order
		float[] jacVals = { 0.512f, 0.256f, 0.128f, 0.064f, 0.032f, 0.016f,
				0.008f, 0.004f, 0.002f, 0.001f };
		for (float f : jacVals) {
			Float jccf = calcJaccardAssortativity(graph, f, newEdges);
			x.add(new Pair<Float, Float>(f, jccf));
		}

		distrStatH.put(StatVal.gsdJacCoef, x);
	}
	
	/**
	 * Calculates assortativity with edges parameter and jaccard cutoff
	 * 
	 * @param graph
	 * @return
	 */
	public static <V extends Comparable<V>, E> Float calcJaccardAssortativity(
			Graph<V, E> graph, Float cutoff, List<Pair<V, V>> edges) { 

		float m = graph.edgeSet().size();
		float numerator = 0f;
		float denominator = 0f;
		
		for (Pair<V, V> p : edges) {
			float jaccard = calcJaccardCoefficient(graph, p.p1, p.p2);
			if (jaccard > cutoff) {

				float a_ij = 1f; // edge exists
				float d_i = graph.edgesOf(p.p1).size();
				float d_j = graph.edgesOf(p.p2).size();
				float f_ij = 1f; // some measure/weight just 1 for now

				numerator += (a_ij - ((d_i * d_j) / (2f * m))) * f_ij;
				
				
				float delta_ij = 1; //edge exists
				
				
				denominator += (d_i*delta_ij - ((d_i*d_j)/(2f*m))) * f_ij;

			}
		}
		
		return numerator/denominator;

	}
	
	/**
	 * Calculates assortativity all edges
	 * 
	 * @param graph
	 * @return
	 */
	public static <V extends Comparable<V>, E> Float calcAssortativity(
			Graph<V, E> graph, int sampleNodes) {

		float m = graph.edgeSet().size();
		float numerator = 0f;
		
		for (E e : graph.edgeSet()) {
			float a_ij = 1f; // edge exists
			float d_i = graph.edgesOf(graph.getEdgeSource(e)).size();
			float d_j = graph.edgesOf(graph.getEdgeTarget(e)).size();
			float f_ij = 1f; // some measure/weight just 1 for now
			
			numerator += (a_ij - ((d_i*d_j)/(2f*m))) * f_ij;
		}
		
		float denominator = 0f;
		
		for (E e : graph.edgeSet()) {
			float d_i = graph.edgesOf(graph.getEdgeSource(e)).size();
			float delta_ij = 1; //edge exists
			float d_j = graph.edgesOf(graph.getEdgeTarget(e)).size();
			float f_ij = 1f; // some measure/weight just 1 for now
			
			denominator += (d_i*delta_ij - ((d_i*d_j)/(2f*m))) * f_ij;
		}

		return numerator/denominator;

	}
	
	public static <V extends Comparable<V>, E> Float calcJaccardCoefficient(Graph<V,E> graph, V v1, V v2){
		Set<V> s = new HashSet<V>();
		List<V> n1 = Graphs.neighborListOf(graph, v1);
		List<V> n2 = Graphs.neighborListOf(graph, v2);
		s.addAll(n1);
		float union = s.size();
		float intersection = 0f;
		for(V v : n2){
			if(s.contains(v)){
				intersection++;
			}else{
				union++;
			}
		}

		return intersection / union;
	}

	/**
	 * Counts unique bidirectional edges: u->v && u<-v
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @return count
	 */
	public static <V extends Comparable<V>, E> int countUniqueBidirectionalEdges(
			Graph<V, E> graph) {

		if (!(graph instanceof DirectedGraph)) {
			// then every edge is bi-directional
			return countUniqueEdges((UndirectedGraph<V, E>) graph);
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

	/**
	 * Counts unique bidirectional edges: u->v && u<-v
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param t1
	 * @param t2
	 * @return count
	 */
	public static <V extends Comparable<V>, E> int countUniqueBidirectionalEdges(
			Graph<V, E> graph, Class<?> t1, Class<?> t2) {

		if (!(graph instanceof DirectedGraph)) {
			// then every edge is bi-directional
			return countUniqueEdges((UndirectedGraph<V, E>) graph, t1, t2);
		}

		DirectedGraph<V, E> dg = (DirectedGraph<V, E>) graph;
		int cnt = 0;
		for (V v : dg.vertexSet()) {
			if (v.getClass().equals(t1)) {
				for (V t : Graphs.successorListOf(dg, v)) {
					if (t.getClass().equals(t2)) {
						if (dg.containsEdge(t, v)) {
							cnt++;
						}
					}
				}
			}
		}
		return cnt;
	}

	/**
	 * Count number of unique edges (if multigraph)
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @return count
	 */
	public static <V extends Comparable<V>, E> int countUniqueEdges(
			Graph<V, E> graph) {
		Set<E> nbrSet = new HashSet<E>();
		int count = 0;
		for (E e : graph.edgeSet()) {
			nbrSet.add(e);
		}
		count += nbrSet.size();
		return count;
	}

	/**
	 * Count number of unique edges (if multigraph)
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param t1
	 * @param t2
	 * @return count
	 */
	public static <V extends Comparable<V>, E> int countUniqueEdges(
			Graph<V, E> graph, Class<?> t1, Class<?> t2) {
		Set<E> nbrSet = new HashSet<E>();
		int count = 0;
		for (E e : graph.edgeSet()) {
			if ((graph.getEdgeSource(e).getClass().equals(t1) && graph
					.getEdgeTarget(e).getClass().equals(t2))
					|| (graph.getEdgeSource(e).getClass().equals(t2) && graph
							.getEdgeTarget(e).getClass().equals(t1))) {
				nbrSet.add(e);
			}
		}
		count = nbrSet.size();
		return count;
	}

	/**
	 * Count number of unique edges (if multigraph)
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param t1
	 * @param t2
	 * @return count
	 */
	public static <V extends Comparable<V>, E> int countEdges(
			Graph<V, E> graph, Class<?> t1, Class<?> t2) {
		int count = 0;
		for (E e : graph.edgeSet()) {
			if ((graph.getEdgeSource(e).getClass().equals(t1) && graph
					.getEdgeTarget(e).getClass().equals(t2))
					|| (graph.getEdgeSource(e).getClass().equals(t2) && graph
							.getEdgeTarget(e).getClass().equals(t1))) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Count the number of nodes containing a certain indegree
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param indegree
	 *            indegree size
	 * @return count
	 */
	public static <V extends Comparable<V>, E> int countNodesOfIndegree(
			Graph<V, E> graph, int indegree) {
		int cnt = 0;
		if (graph instanceof DirectedGraph) {
			for (V v : graph.vertexSet()) {
				if (((DirectedGraph<V, E>) graph).inDegreeOf(v) == indegree) {
					cnt++;
				}
			}
		} else {
			for (V v : graph.vertexSet()) {
				if (graph.edgesOf(v).size() == indegree) {
					cnt++;
				}
			}
		}
		return cnt;
	}

	/**
	 * Count the number of nodes containing a certain outdegree
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param outdegree
	 *            outdegree size
	 * @param t1
	 * @param t2
	 * @return count
	 */
	public static <V extends Comparable<V>, E> int countNodesOfIndegree(
			Graph<V, E> graph, int outdegree, Class<?> t1, Class<?> t2) {
		int count = 0;
		if (graph instanceof DirectedGraph) {
			for (V v : graph.vertexSet()) {
				if (v.getClass().equals(t1)) {
					int z = 0;
					for (V dst : Graphs.predecessorListOf(
							((DirectedGraph<V, E>) graph), v)) {
						if (dst.getClass().equals(t2)) {
							z++;
						}
					}
					if (z == outdegree) {
						count++;
					}
				}
			}
		} else {
			for (V v : graph.vertexSet()) {
				if (v.getClass().equals(t1)) {
					int z = 0;
					for (V dst : Graphs.neighborListOf(graph, v)) {
						if (dst.getClass().equals(t2)) {
							z++;
						}
					}
					if (z == outdegree) {
						count++;
					}
				}
			}
		}
		return count;
	}

	/**
	 * Count the number of nodes containing a certain outdegree
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param outdegree
	 *            outdegree size
	 * @return count
	 */
	public static <V extends Comparable<V>, E> int countNodesOfOutdegree(
			Graph<V, E> graph, int outdegree) {
		int count = 0;
		if (graph instanceof DirectedGraph) {
			for (V v : graph.vertexSet()) {
				if (((DirectedGraph<V, E>) graph).outDegreeOf(v) == outdegree) {
					count++;
				}
			}
		} else {
			for (V v : graph.vertexSet()) {
				if (graph.edgesOf(v).size() == outdegree) {
					count++;
				}
			}
		}
		return count;
	}

	/**
	 * Count the number of nodes containing a certain outdegree
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param outdegree
	 *            outdegree size
	 * @param t1
	 * @param t2
	 * @return count
	 */
	public static <V extends Comparable<V>, E> int countNodesOfOutdegree(
			Graph<V, E> graph, int outdegree, Class<?> t1, Class<?> t2) {
		int count = 0;
		if (graph instanceof DirectedGraph) {
			for (V v : graph.vertexSet()) {
				if (v.getClass().equals(t1)) {
					int z = 0;
					for (V dst : Graphs.successorListOf(
							((DirectedGraph<V, E>) graph), v)) {
						if (dst.getClass().equals(t2)) {
							z++;
						}
					}
					if (z == outdegree) {
						count++;
					}
				}
			}
		} else {
			for (V v : graph.vertexSet()) {
				if (v.getClass().equals(t1)) {
					int z = 0;
					for (V dst : Graphs.neighborListOf(graph, v)) {
						if (dst.getClass().equals(t2)) {
							z++;
						}
					}
					if (z == outdegree) {
						count++;
					}
				}
			}
		}
		return count;
	}

	/**
	 * Count the number of nodes containing a certain degree
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param degree
	 *            degree size
	 * @return count
	 */
	public static <V extends Comparable<V>, E> Integer countNodesOfDegree(
			Graph<V, E> graph, int degree) {
		int count = 0;
		for (V v : graph.vertexSet()) {
			if (graph.edgesOf(v).size() == degree) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Count the number of nodes containing a certain degree
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param degree
	 *            degree size
	 * @param t1
	 * @param t2
	 * @return count
	 */
	public static <V extends Comparable<V>, E> Integer countNodesOfDegree(
			Graph<V, E> graph, int degree, Class<?> t1, Class<?> t2) {
		int count = 0;
		for (V v : graph.vertexSet()) {
			if (v.getClass().equals(t1)) {
				int z = 0;
				for (V dst : Graphs.neighborListOf(graph, v)) {
					if (dst.getClass().equals(t2)) {
						z++;
					}
				}
				if (z == degree)
					count++;
			}

		}
		return count;
	}

	/**
	 * Count the indegree distribution of the graph
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @return Vector<Pair<indegree, count>>
	 */
	public static <V extends Comparable<V>, E> Vector<Pair<Float, Float>> countIndegree(
			Graph<V, E> graph) {
		Vector<Pair<Float, Float>> degreeToCountV = new Vector<Pair<Float, Float>>();
		Hashtable<Integer, Integer> degreeToCountH = new Hashtable<Integer, Integer>();
		for (V v : graph.vertexSet()) {
			if (graph instanceof DirectedGraph) {
				DirectedGraph<V, E> dg = (DirectedGraph<V, E>) graph;
				if (degreeToCountH.containsKey(dg.inDegreeOf(v))) {
					degreeToCountH.put(dg.inDegreeOf(v),
							degreeToCountH.get(dg.inDegreeOf(v)) + 1);
				} else {
					degreeToCountH.put(dg.inDegreeOf(v), 1);
				}
			} else {
				if (degreeToCountH.containsKey(graph.edgesOf(v).size())) {
					degreeToCountH.put(graph.edgesOf(v).size(),
							degreeToCountH.get(graph.edgesOf(v).size()) + 1);
				} else {
					degreeToCountH.put(graph.edgesOf(v).size(), 1);
				}
			}

		}
		for (Entry<Integer, Integer> e : degreeToCountH.entrySet()) {
			degreeToCountV.add(new Pair<Float, Float>((float) e.getKey(),
					(float) e.getValue()));
		}
		Collections.sort(degreeToCountV);
		return degreeToCountV;
	}

	/**
	 * Count the indegree distribution of the graph
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param t1
	 * @param t2
	 * @return Vector<Pair<indegree, count>>
	 */
	public static <V extends Comparable<V>, E> Vector<Pair<Float, Float>> countIndegree(
			Graph<V, E> graph, Class<?> t1, Class<?> t2) {
		Vector<Pair<Float, Float>> degreeToCountV = new Vector<Pair<Float, Float>>();
		Hashtable<Integer, Integer> degreeToCountH = new Hashtable<Integer, Integer>();
		for (V v : graph.vertexSet()) {
			if (!v.getClass().equals(t1))
				continue;
			if (graph instanceof DirectedGraph) {
				DirectedGraph<V, E> dg = (DirectedGraph<V, E>) graph;
				int cnt = 0;
				for (V t : Graphs.predecessorListOf(dg, v)) {
					if (t.getClass().equals(t2)) {
						cnt++;
					}
				}
				if (degreeToCountH.containsKey(cnt)) {
					degreeToCountH.put(cnt, degreeToCountH.get(cnt) + 1);
				} else {
					degreeToCountH.put(cnt, 1);
				}
			} else {
				int cnt = 0;
				for (V t : Graphs.neighborListOf(graph, v)) {
					if (t.getClass().equals(t2)) {
						cnt++;
					}
				}
				if (degreeToCountH.containsKey(cnt)) {
					degreeToCountH.put(cnt, degreeToCountH.get(cnt) + 1);
				} else {
					degreeToCountH.put(cnt, 1);
				}
			}

		}
		for (Entry<Integer, Integer> e : degreeToCountH.entrySet()) {
			degreeToCountV.add(new Pair<Float, Float>((float) e.getKey(),
					(float) e.getValue()));
		}
		Collections.sort(degreeToCountV);
		return degreeToCountV;
	}

	/**
	 * Count the outdegree distribution of the graph
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @return Vector<Pair<outdegree, count>>
	 */
	public static <V extends Comparable<V>, E> Vector<Pair<Float, Float>> countOutdegree(
			Graph<V, E> graph) {
		Vector<Pair<Float, Float>> degreeToCountV = new Vector<Pair<Float, Float>>();
		Hashtable<Integer, Integer> degreeToCountH = new Hashtable<Integer, Integer>();
		for (V v : graph.vertexSet()) {
			if (graph instanceof DirectedGraph) {
				DirectedGraph<V, E> dg = (DirectedGraph<V, E>) graph;
				if (degreeToCountH.containsKey(dg.outDegreeOf(v))) {
					degreeToCountH.put(dg.outDegreeOf(v),
							degreeToCountH.get(dg.outDegreeOf(v)) + 1);
				} else {
					degreeToCountH.put(dg.outDegreeOf(v), 1);
				}
			} else {
				if (degreeToCountH.containsKey(graph.edgesOf(v).size())) {
					degreeToCountH.put(graph.edgesOf(v).size(),
							degreeToCountH.get(graph.edgesOf(v).size()) + 1);
				} else {
					degreeToCountH.put(graph.edgesOf(v).size(), 1);
				}
			}

		}
		for (Entry<Integer, Integer> e : degreeToCountH.entrySet()) {
			degreeToCountV.add(new Pair<Float, Float>((float) e.getKey(),
					(float) e.getValue()));
		}
		Collections.sort(degreeToCountV);
		return degreeToCountV;
	}
	
	/**
	 * Count the indegree distribution of the graph
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param t1
	 * @param t2
	 * @return Vector<Pair<indegree, count>>
	 */
	public static <V extends Comparable<V>, E> Vector<Pair<Float, Float>> countOutdegree(
			Graph<V, E> graph, Class<?> t1, Class<?> t2) {
		Vector<Pair<Float, Float>> degreeToCountV = new Vector<Pair<Float, Float>>();
		Hashtable<Integer, Integer> degreeToCountH = new Hashtable<Integer, Integer>();
		for (V v : graph.vertexSet()) {
			if (!v.getClass().equals(t1))
				continue;
			if (graph instanceof DirectedGraph) {
				DirectedGraph<V, E> dg = (DirectedGraph<V, E>) graph;
				int cnt = 0;
				for (V t : Graphs.successorListOf(dg, v)) {
					if (t.getClass().equals(t2)) {
						cnt++;
					}
				}
				if (degreeToCountH.containsKey(cnt)) {
					degreeToCountH.put(cnt, degreeToCountH.get(cnt) + 1);
				} else {
					degreeToCountH.put(cnt, 1);
				}
			} else {
				int cnt = 0;
				for (V t : Graphs.neighborListOf(graph, v)) {
					if (t.getClass().equals(t2)) {
						cnt++;
					}
				}
				if (degreeToCountH.containsKey(cnt)) {
					degreeToCountH.put(cnt, degreeToCountH.get(cnt) + 1);
				} else {
					degreeToCountH.put(cnt, 1);
				}
			}

		}
		for (Entry<Integer, Integer> e : degreeToCountH.entrySet()) {
			degreeToCountV.add(new Pair<Float, Float>((float) e.getKey(),
					(float) e.getValue()));
		}
		Collections.sort(degreeToCountV);
		return degreeToCountV;
	}


}
