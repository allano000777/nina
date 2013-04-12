package edu.nd.nina.alg;

import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Vector;

import edu.nd.nina.Graph;
import edu.nd.nina.structs.Pair;

public class BreadthDepthFirstSearch<V extends Comparable<V>, E> {
	Graph<V, E> g;
	Queue<V> queue;
	V StartNId;
	Hashtable<V, Integer> NIdDistH;

	/**
	 * 
	 * @param _g
	 */
	public BreadthDepthFirstSearch(Graph<V, E> _g) {
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
	public Vector<Pair<Integer, Integer>> getNodesAtHops(final V StartNId,
			final boolean IsDir) {
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

	public int GetBfsFullDiam(final int NTestNodes, final boolean IsDir) {
		int[] FullDiam = {0};
		double[] EffDiam = {0d};
		GetBfsEffDiam(NTestNodes, IsDir, EffDiam, FullDiam);
		return FullDiam[0];
	}

	public double GetBfsEffDiam(final int NTestNodes, final boolean IsDir,
			double[] EffDiam, int[] FullDiam) {
		double[] AvgDiam = {0d};
		EffDiam[0] = -1;
		FullDiam[0] = -1;
		return GetBfsEffDiam(NTestNodes, IsDir, EffDiam, FullDiam, AvgDiam);
	}

	public double GetBfsEffDiam(final int NTestNodes, final boolean IsDir,
			double[] EffDiam, int[] FullDiam, double[] AvgSPL) {
		EffDiam[0] = -1;
		FullDiam[0] = -1;
		AvgSPL[0] = -1;
		Hashtable<Integer, Float> DistToCntH = new Hashtable<Integer, Float>();

		// shotest paths
		Vector<V> NodeIdV = new Vector<V>();
		NodeIdV.addAll(g.vertexSet());
		Collections.shuffle(NodeIdV);

		for (int tries = 0; tries < Math.min(NTestNodes, g.vertexSet().size()); tries++) {
			final V NId = NodeIdV.get(tries);
			doBfs(NId, true, !IsDir, null, Integer.MAX_VALUE);
			for (Entry<V, Integer> e : this.NIdDistH.entrySet()) {
				if (DistToCntH.containsKey(e.getValue())) {
					DistToCntH.put(e.getValue(),
							(float) DistToCntH.get(e.getValue()) + 1);
				} else {
					DistToCntH.put(e.getValue(), 1f);
				}
			}
		}
		Vector<Pair<Integer, Float>> DistNbrsPdfV = new Vector<Pair<Integer, Float>>();
		double SumPathL = 0, PathCnt = 0;
		for (Entry<Integer, Float> e : DistToCntH.entrySet()) {
			DistNbrsPdfV
					.add(new Pair<Integer, Float>(e.getKey(), e.getValue()));
			SumPathL += e.getKey() * e.getValue();
			PathCnt += e.getValue();
		}
		Collections.sort(DistNbrsPdfV);

		// effective diameter (90-th percentile)
		EffDiam[0] = ApproximateNeighborhoodFunction.CalcEffDiamPdf(DistNbrsPdfV,
				0.9);
		// approximate full diameter (max shortest path length over the sampled
		// nodes)
		FullDiam[0] = DistNbrsPdfV.lastElement().p1;
		// average shortest path length
		AvgSPL[0] = SumPathL / PathCnt;
		return EffDiam[0];
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
				for (E e : g.edgesOf(NId)) {
					if(g.getEdgeTarget(e) == NId) continue;
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
				for (E e : g.edgesOf(NId)) {
					if(g.getEdgeSource(e) == NId) continue;
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
