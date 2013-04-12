package edu.nd.nina.alg;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import edu.nd.nina.DirectedGraph;
import edu.nd.nina.structs.Pair;

public class SingularValueDecomposition<V, E> {
	DirectedGraph<V, E> graph;

	public SingularValueDecomposition(DirectedGraph<V, E> graph) {
		this.graph = graph;
	}

	public Vector<Float> GetSngVals(final int SngVals) {
		final int Nodes = graph.vertexSet().size();
		Vector<Float> SngValV = new Vector<Float>();
		assert (SngVals > 0);
		Hashtable<Object, Integer> NodeIdH = new Hashtable<Object, Integer>();
		// perform full SVD
		DoubleMatrix2D AdjMtx = new DenseDoubleMatrix2D(Nodes, Nodes);

		// create adjecency matrix
		int i = 1;
		for (Object v : graph.vertexSet()) {
			NodeIdH.put(v, i);
			i++;
		}
		for (V v : graph.vertexSet()) {
			final int NodeId = NodeIdH.get(v);
			for (E e : graph.outgoingEdgesOf(v)) {
				V dst = graph.getEdgeTarget(e);
				int DstNId = NodeIdH.get(dst);
				// no self edges
				if (NodeId != DstNId) {
					AdjMtx.set(NodeId, DstNId, 1f);
				}
			}
		}
		try { // can fail to converge but results seem to be good
			cern.colt.matrix.linalg.SingularValueDecomposition svd = new cern.colt.matrix.linalg.SingularValueDecomposition(
					AdjMtx);
			for (Double d : svd.getSingularValues()) {
				SngValV.add((float) (double) d);
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.err.printf("\n***No SVD convergence: G(%d, %d)\n", Nodes,
					graph.edgeSet().size());
		}

		return SngValV;

		// if (SngValV.Len() > SngVals) {
		// SngValV.Del(SngVals, SngValV.Len()-1); }
		// else {
		// while (SngValV.Len() < SngVals) SngValV.Add(1e-6); }
		// IAssert(SngValV.Len() == SngVals);
	}

	public Vector<Float> GetSngVec() {
		final int Nodes = graph.vertexSet().size();
		Vector<Float> SngValV = new Vector<Float>();
		Vector<Float> LeftSV = new Vector<Float>();
		Vector<Float> RightSV = new Vector<Float>();
		Hashtable<Object, Integer> NodeIdH = new Hashtable<Object, Integer>();
		// perform full SVD
		DoubleMatrix2D AdjMtx = new DenseDoubleMatrix2D(Nodes, Nodes);

		// create adjecency matrix
		int i = 1;
		for (Object v : graph.vertexSet()) {
			NodeIdH.put(v, i);
			i++;
		}
		for (V v : graph.vertexSet()) {
			final int NodeId = NodeIdH.get(v);
			for (E e : graph.outgoingEdgesOf(v)) {
				V dst = graph.getEdgeTarget(e);
				int DstNId = NodeIdH.get(dst);
				// no self edges
				if (NodeId != DstNId) {
					AdjMtx.set(NodeId, DstNId, 1f);
				}
			}
		}
		try { // can fail to converge but results seem to be good
			cern.colt.matrix.linalg.SingularValueDecomposition svd = new cern.colt.matrix.linalg.SingularValueDecomposition(
					AdjMtx);
			for (Double d : svd.getSingularValues()) {
				SngValV.add((float) (double) d);
			}

			Vector<Pair<Float, Integer>> SngValIdV = new Vector<Pair<Float, Integer>>();
			for (int z = 0; z < SngValV.size(); z++) {
				SngValIdV.add(new Pair<Float, Integer>(SngValV.get(z), z));
			}
			Collections.sort(SngValIdV);
			Collections.sort(SngValV);

			for (int v = 0; v < SngValIdV.size(); v++) {

				for (int w = 0; w < AdjMtx.rows(); w++) {
					LeftSV.add((float) svd.getU().get(w, SngValIdV.get(v).p2));
				}
				for (int w = 0; w < AdjMtx.rows(); w++) {
					RightSV.add((float) svd.getV().get(w, SngValIdV.get(v).p2));
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.printf("\n***No SVD convergence: G(%d, %d)\n", Nodes,
					graph.edgeSet().size());
		}

		return SngValV;

		// if (SngValV.Len() > SngVals) {
		// SngValV.Del(SngVals, SngValV.Len()-1); }
		// else {
		// while (SngValV.Len() < SngVals) SngValV.Add(1e-6); }
		// IAssert(SngValV.Len() == SngVals);
	}

}
