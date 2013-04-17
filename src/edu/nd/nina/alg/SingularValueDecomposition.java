package edu.nd.nina.alg;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import edu.nd.nina.Graph;
import edu.nd.nina.Graphs;
import edu.nd.nina.structs.Pair;

/**
 * Static convenience class for SVD Computation
 * 
 * @author Tim Weninger
 * 
 * @param <V>
 *            Node type
 * @param <E>
 *            Edge type
 */
public class SingularValueDecomposition<V, E> {

	/**
	 * Computes largest singular values of the adjacency matrix representing a
	 * Graph.
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param singularValues
	 *            Number of singular values to get
	 * @return Vector<Singular Values>
	 */
	public static <V extends Comparable<V>, E> Vector<Float> getSingularValues(
			Graph<V, E> graph, final int singularValues) {
		final int Nodes = graph.vertexSet().size();
		Vector<Float> singularValuesV = new Vector<Float>();
		assert (singularValues > 0);
		Hashtable<Object, Integer> nodeToIdH = new Hashtable<Object, Integer>();
		// perform full SVD
		DoubleMatrix2D adjacencyMatrix = new DenseDoubleMatrix2D(Nodes, Nodes);

		// create adjacency matrix
		int i = 0;
		for (V v : graph.vertexSet()) {
			nodeToIdH.put(v, i);
			i++;
		}
		for (V v : graph.vertexSet()) {
			final int vId = nodeToIdH.get(v);
			for (V t : Graphs.neighborListOf(graph, v)) {
				int tId = nodeToIdH.get(t);
				adjacencyMatrix.set(vId, tId, 1f);
			}
		}
		try { // can fail to converge but results seem to be good
			cern.colt.matrix.linalg.SingularValueDecomposition svd = new cern.colt.matrix.linalg.SingularValueDecomposition(
					adjacencyMatrix);
			for (Double d : svd.getSingularValues()) {
				singularValuesV.add((float) (double) d);
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.err.printf("\n***No SVD convergence: G(%d, %d)\n", Nodes,
					graph.edgeSet().size());
		}

		return singularValuesV;
	}

	/**
	 * Computes the leading left and right singular vector of the adjacency
	 * matrix representing a directed Graph.
	 * 
	 * @param graph
	 *            Graph snapshot
	 * @param leftV
	 *            Left singular vector
	 * @param rightV
	 *            Right singular vector
	 * @return Vector<Singular Values>
	 */
	public static <V extends Comparable<V>, E> Vector<Float> getSingularValuesVector(
			Graph<V, E> graph, Vector<Float> leftV, Vector<Float> rightV) {
		final int Nodes = graph.vertexSet().size();
		Vector<Float> singularValuesV = new Vector<Float>();
		Hashtable<Object, Integer> nodeToIdH = new Hashtable<Object, Integer>();
		// perform full SVD
		DoubleMatrix2D adjacencyMatrix = new DenseDoubleMatrix2D(Nodes, Nodes);

		// create adjecency matrix
		int i = 0;
		for (Object v : graph.vertexSet()) {
			nodeToIdH.put(v, i);
			i++;
		}
		for (V v : graph.vertexSet()) {
			final int vId = nodeToIdH.get(v);
			for (V t : Graphs.neighborListOf(graph, v)) {
				int tId = nodeToIdH.get(t);
				adjacencyMatrix.set(vId, tId, 1f);
			}
		}
		try { // can fail to converge but results seem to be good
			cern.colt.matrix.linalg.SingularValueDecomposition svd = new cern.colt.matrix.linalg.SingularValueDecomposition(
					adjacencyMatrix);
			for (Double d : svd.getSingularValues()) {
				singularValuesV.add((float) (double) d);
			}

			Vector<Pair<Float, Integer>> singularValuesIdV = new Vector<Pair<Float, Integer>>();
			for (int z = 0; z < singularValuesV.size(); z++) {
				singularValuesIdV.add(new Pair<Float, Integer>(singularValuesV
						.get(z), z));
			}
			Collections.sort(singularValuesIdV);
			Collections.sort(singularValuesV);

			for (int v = 0; v < singularValuesIdV.size(); v++) {

				for (int w = 0; w < adjacencyMatrix.rows(); w++) {
					leftV.add((float) svd.getU().get(w,
							singularValuesIdV.get(v).p2));
				}
				for (int w = 0; w < adjacencyMatrix.rows(); w++) {
					rightV.add((float) svd.getV().get(w,
							singularValuesIdV.get(v).p2));
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.printf("\n***No SVD convergence: G(%d, %d)\n", Nodes,
					graph.edgeSet().size());
		}

		return singularValuesV;
	}

}
