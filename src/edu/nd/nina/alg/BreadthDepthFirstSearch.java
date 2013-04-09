package edu.nd.nina.alg;

import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Vector;

import edu.nd.nina.DirectedGraph;
import edu.nd.nina.structs.Pair;

public class BreadthDepthFirstSearch<V extends Comparable<V>, E> {
	DirectedGraph<V, E> g;
	Queue<V> queue;
	V StartNId;
	Hashtable<V, Integer> NIdDistH;

	/**
	 * 
	 * @param _g
	 */
	public BreadthDepthFirstSearch(DirectedGraph<V, E> _g) {
		g = _g;
		queue = new LinkedList<V>();
		NIdDistH = new Hashtable<V, Integer>(g.vertexSet().size());
	}

	/**
	 * 
	 * @param g
	 * @param StartNId
	 * @param IsDir
	 * @return
	 */
	public Vector<Pair<Integer, Integer>> getNodesAtHops(final V StartNId, final boolean IsDir) {
		Vector<Pair<Integer, Integer>> HopCntV = new Vector<Pair<Integer, Integer>>();

		doBfs(StartNId, true, !IsDir, null, Integer.MAX_VALUE);
		Hashtable<Integer, Integer> HopCntH = new Hashtable<Integer, Integer>();
		for (Integer val : NIdDistH.values()) {
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

	private int doBfs(final V StartNode, final boolean FollowOut,
			final boolean FollowIn, final V TargetNId, final Integer MxDist) {
		StartNId = StartNode;
		assert (g.containsVertex(StartNId));

		NIdDistH.clear();
		NIdDistH.put(StartNId, 0);
		queue.clear();
		queue.add(StartNId);		
		int MaxDist = 0;
		while (!queue.isEmpty()) {
			final V NId = queue.poll();
			final int Dist = NIdDistH.get(NId);
			// max distance limit reached
			if (Dist == MxDist) {
				break;
			}

			// out-links
			if (FollowOut) {
				// out-links
				for (E e : g.outgoingEdgesOf(NId)) {
					final V DstNId = g.getEdgeTarget(e);
					if (!NIdDistH.containsKey(DstNId)) {
						NIdDistH.put(DstNId, Dist + 1);
						MaxDist = Math.max(MaxDist, Dist + 1);
						if (DstNId == TargetNId) {
							return MaxDist;
						}
						queue.add(DstNId);
					}
				}
			}

			// in-links
			if (FollowIn) {
				for (E e : g.incomingEdgesOf(NId)) {
					final V DstNId = g.getEdgeSource(e);
					if (!NIdDistH.containsKey(DstNId)) {
						NIdDistH.put(DstNId, Dist + 1);
						MaxDist = Math.max(MaxDist, Dist + 1);
						if (DstNId == TargetNId) {
							return MaxDist;
						}
						queue.add(DstNId);
					}
				}
			}
		}
		return MaxDist;

	}
}
