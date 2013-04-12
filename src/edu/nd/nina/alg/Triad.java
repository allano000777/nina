package edu.nd.nina.alg;

import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import edu.nd.nina.DirectedGraph;
import edu.nd.nina.Graph;
import edu.nd.nina.structs.Pair;
import edu.nd.nina.structs.Triple;

public class Triad<V extends Comparable<V>, E> {
	private long OpenTriads;
	private long ClosedTriads;

	public float getClustCf(Graph<V, E> graph, int SampleNodes) {

		Vector<Triple<V, Integer, Integer>> NIdCOTriadV = getTriads(graph,
				SampleNodes);
		Hashtable<Integer, Pair<Float, Float>> DegSumCnt = new Hashtable<Integer, Pair<Float, Float>>();
		double SumCcf = 0.0;
		long closedTriads = 0;
		long openTriads = 0;
		for (int i = 0; i < NIdCOTriadV.size(); i++) {
			final int D = NIdCOTriadV.get(i).v2 + NIdCOTriadV.get(i).v3;
			final double Ccf = D != 0 ? NIdCOTriadV.get(i).v2 / (double) D
					: 0.0;
			closedTriads += NIdCOTriadV.get(i).v2;
			openTriads += NIdCOTriadV.get(i).v3;
			Pair<Float, Float> SumCnt = new Pair<Float, Float>((float) Ccf, 1f);

			SumCcf += Ccf;
			DegSumCnt.put(graph.edgesOf(NIdCOTriadV.get(i).v1).size(), SumCnt);
		}
		// get average clustering coefficient for each degree
		Vector<Pair<Float, Float>> DegToCCfV = new Vector<Pair<Float, Float>>();
		for (Entry<Integer, Pair<Float, Float>> e : DegSumCnt.entrySet()) {
			DegToCCfV.add(new Pair<Float, Float>((float) e.getKey(), e
					.getValue().p1 / e.getValue().p2));
		}
		// if(closedTriads/3 > (uint64) TInt::Mx) {
		// WarnNotify(TStr::Fmt("[%s line %d] %g closed triads.\n", __FILE__,
		// __LINE__, float(closedTriads/3)).CStr()); }
		// if(openTriads > (uint64) TInt::Mx) {
		// WarnNotify(TStr::Fmt("[%s line %d] %g open triads.\n", __FILE__,
		// __LINE__, float(openTriads/3)).CStr()); }
		ClosedTriads = closedTriads / 3l; // each triad is counted 3 times
		OpenTriads = openTriads;
		Collections.sort(DegToCCfV);
		return (float) (SumCcf / (float) NIdCOTriadV.size());
	}

	public Vector<Triple<V, Integer, Integer>> getTriads(Graph<V, E> graph,
			int SampleNodes) {
		boolean IsDir = false;
		if (graph instanceof DirectedGraph) {
			IsDir = true;
		}
		Set<V> NbrH = new HashSet<V>();
		Vector<V> NIdV = new Vector<V>();

		NIdV.addAll(graph.vertexSet());
		Collections.shuffle(NIdV);

		if (SampleNodes == -1) {
			SampleNodes = graph.vertexSet().size();
		}

		Vector<Triple<V, Integer, Integer>> NIdCOTriadV = new Vector<Triple<V, Integer, Integer>>();

		for (int node = 0; node < SampleNodes; node++) {
			V v = NIdV.get(node);
			if (graph.edgesOf(v).size() < 2) {
				NIdCOTriadV.add(new Triple<V, Integer, Integer>(v, 0, 0)); // zero
																			// triangles
				continue;
			}
			// find neighborhood
			NbrH.clear();
			for (E e : graph.edgesOf(v)) {
				if (graph.getEdgeTarget(e) != v) {

					NbrH.add(graph.getEdgeTarget(e));
				}
			}
			if (IsDir) {

				for (E e : ((DirectedGraph<V, E>) graph).incomingEdgesOf(v)) {
					if (graph.getEdgeSource(e) != v) {

						NbrH.add(graph.getEdgeSource(e));
					}
				}
			}
			// count connected neighbors
			int OpenCnt = 0, CloseCnt = 0;
			int i = 0;
			for (V SrcNode : NbrH) {
				int j = 0;
				for (V dstNId : NbrH) {
					if (j <= i) {

						if (graph.containsEdge(SrcNode, dstNId)
								|| graph.containsEdge(dstNId, SrcNode)) {
							CloseCnt++;
						} // is edge
						else {
							OpenCnt++;
						}
					}
					j++;
				}
				i++;
			}
			assert (2 * (OpenCnt + CloseCnt) == NbrH.size() * (NbrH.size() - 1));
			NIdCOTriadV.add(new Triple<V, Integer, Integer>(v, CloseCnt,
					OpenCnt));
		}
		return NIdCOTriadV;
	}

	public long getOpen() {
		return OpenTriads;
	}

	public long getClosed() {
		return ClosedTriads;
	}

	public Vector<Pair<Integer, Integer>> getTriadParticip(Graph<V, E> graph) {
		Vector<Pair<Integer, Integer>> TriadCntV = new Vector<Pair<Integer, Integer>>();
		Hashtable<Integer, Integer> TriadCntH = new Hashtable<Integer, Integer>();
		for (V v : graph.vertexSet()) {
			final int Triads = getNodeTriads(graph, v);
			if (TriadCntH.containsKey(Triads)) {
				TriadCntH.put(Triads, TriadCntH.get(Triads) + 1);
			} else {
				TriadCntH.put(Triads, 1);
			}

		}
		for (Entry<Integer, Integer> e : TriadCntH.entrySet()) {
			TriadCntV.add(new Pair<Integer, Integer>(e.getKey(), e.getValue()));
		}
		Collections.sort(TriadCntV);
		return TriadCntV;
	}

	// Returns number of undirected triads a node participates in
	int getNodeTriads(final Graph<V, E> graph, final V v) {
		int ClosedTriads = 0, OpenTriads = 0;
		return getNodeTriads(graph, v, ClosedTriads, OpenTriads);
	}

	private int getNodeTriads(Graph<V, E> graph, V v, int closedTriads,
			int openTriads) {
		closedTriads = 0;
		openTriads = 0;
		if (graph.edgesOf(v).size() < 2) {
			return 0;
		}
		// find neighborhood

		Set<V> NbrSet = new HashSet<V>(graph.edgesOf(v).size());
		for (E e : graph.edgesOf(v)) {
			// exclude self edges
			if (graph.getEdgeTarget(e) != v) {
				NbrSet.add(graph.getEdgeTarget(e));
			}
		}
		if (graph instanceof DirectedGraph) {
			for (E e : ((DirectedGraph<V, E>) graph).incomingEdgesOf(v)) {
				// exclude self edges
				if (graph.getEdgeSource(e) != v) {
					NbrSet.add(graph.getEdgeSource(e));
				}
			}
		}
		// count connected neighbors
		int i = 0;
		for (V SrcNode : NbrSet) {
			int j = 0;
			for (V dstNId : NbrSet) {
				if (j <= i) {

					if (graph.containsEdge(SrcNode, dstNId)
							|| graph.containsEdge(dstNId, SrcNode)) {
						closedTriads++;
					} // is edge
					else {
						openTriads++;
					}
				}
				j++;
			}
			i++;
		}

		return closedTriads;
	}
}
