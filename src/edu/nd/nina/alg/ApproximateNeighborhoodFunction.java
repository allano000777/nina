package edu.nd.nina.alg;

import java.util.Hashtable;
import java.util.Vector;

import edu.nd.nina.Graph;
import edu.nd.nina.math.Randoms;
import edu.nd.nina.structs.Pair;
import edu.nd.nina.util.GraphUtil;

public class ApproximateNeighborhoodFunction<V, E> {
	Graph<V, E> Graph;
	// maintain N parallel approximations (multiple of 8)
	int NApprox;
	int RndSeed;

	Vector<Long> TAnfBitV;
	// NId to byte(!) offset in BitV
	Hashtable<V, Long> NIdToBitPosH;

	// NBits=logNodes+MoreBits; MoreBits: additional R bits; ApproxBytes:
	// Approx/8;
	int NBits, MoreBits, ApproxBytes;

	Randoms Rnd;

	public ApproximateNeighborhoodFunction(Graph<V, E> graph, int approx,
			int moreBits, int rndSeed) {
		this.Graph = graph;
		this.NApprox = approx;
		this.MoreBits = moreBits;
		this.RndSeed = rndSeed;
	}

	/**
	 * 
	 * @param Graph
	 * @param SrcNId
	 *            Starting node.
	 * @param DistNbrsV
	 *            Maps between the distance H (in hops) and the number of nodes
	 *            reachable in <=H hops.
	 * @param MxDist
	 *            Maximum number of hops the algorithm spreads from SrcNId.
	 * @param IsDir
	 *            false: consider links as undirected (drop link directions).
	 * @param NApprox
	 *            Quality of approximation. See the ANF paper.
	 */
	public void GetNodeAnf(final Graph graph, final int SrcNId,
			Vector<Pair<Integer, Float>> DistNbrsV, final int MxDist,
			final boolean IsDir, int NApprox) {

	}

	/**
	 * Approximate Neighborhood Function of a Graph: Returns the number of pairs
	 * of nodes reachable in less than H hops. For example, DistNbrsV.GetDat(0)
	 * is the number of nodes in the graph, DistNbrsV.GetDat(1) is the number of
	 * nodes+edges and so on.
	 * 
	 * @param Graph
	 * @param DistNbrsV
	 *            Maps between the distance H (in hops) and the number of nodes
	 *            reachable in <=H hops.
	 * @param MxDist
	 *            Maximum number of hops the algorithm spreads from SrcNId.
	 * @param IsDir
	 *            false: consider links as undirected (drop link directions).
	 * @param NApprox
	 *            Quality of approximation. See the ANF paper.
	 */
	public Vector<Pair<Integer, Float>> getGraphAnf(int MxDist,
			boolean IsDir) {
		Vector<Long> CurBitsV = new Vector<Long>();
		Vector<Long> LastBitsV = new Vector<Long>();
		InitAnfBits(CurBitsV);
		assert (CurBitsV.firstElement() != null);
		for (int i = 0; i < CurBitsV.size(); i++)
			LastBitsV.add(0l);
		assert (LastBitsV.firstElement() != null);
		float NPairs = 0f;
		Vector<Pair<Integer, Float>> DistNbrsV = new Vector<Pair<Integer, Float>>();
		DistNbrsV.add(new Pair<Integer, Float>(0, (float) Graph.vertexSet()
				.size()));
		DistNbrsV.add(new Pair<Integer, Float>(1, (float) Graph.edgeSet()
				.size()));
		// TExeTm ExeTm;
		for (int dist = 2; dist < (MxDist == -1 ? Integer.MAX_VALUE : MxDist); dist++) {
			// printf("ANF dist %d...", dist); ExeTm.Tick();
			for (int i = 0; i < CurBitsV.size(); i++) {
				LastBitsV.set(i, CurBitsV.get(i));
			}

			for (V v : Graph.vertexSet()) {
				final long NIdOffset = GetNIdOffset(v);
				for (E e : Graph.edgesOf(v)) {
					V tgt = Graph.getEdgeTarget(e);
					if (tgt == v)
						continue;
					final long NId2Offset = GetNIdOffset(tgt);
					Union(CurBitsV, NIdOffset, LastBitsV, NId2Offset);
				}
				if (!IsDir) {
					for (E e : Graph.edgesOf(v)) {
						V src = Graph.getEdgeSource(e);
						if (src == v)
							continue;
						final long NId2Offset = GetNIdOffset(src);
						Union(CurBitsV, NIdOffset, LastBitsV, NId2Offset);
					}
				}
			}
			NPairs = 0f;
			for (Long e : NIdToBitPosH.values()) {
				NPairs += GetCount(CurBitsV, e);
			}
			DistNbrsV.add(new Pair<Integer, Float>(dist, NPairs));
			// printf("pairs: %g  %s\n", NPairs, ExeTm.GetTmStr());
			if (NPairs == 0) {
				break;
			}
			if (DistNbrsV.size() > 1
					&& NPairs < 1.001 * DistNbrsV.lastElement().p2) {
				break;
			} // 0.1% change
			// TGnuPlot::SaveTs(DistNbrsV, "hops.tab", "HOPS, REACHABLE PAIRS");
		}
		return DistNbrsV;
	}

	private double GetCount(Vector<Long> BitV, Long NIdOffset) {
		return Math.pow(2.0, AvgLstZero(BitV, NIdOffset)) / 0.77351;
	}

	private double AvgLstZero(Vector<Long> BitV, Long NIdOffset) {
		int approx, bit, AvgBitPos = 0;
		long BitVPt = 0l;
		for (approx = 0; approx < NApprox; approx++) {
			for (bit = 0; bit < NBits; bit++) {
				if (BitV.get((int) ((BitVPt + NIdOffset + ApproxBytes * bit + approx / 8) & (1 << (approx % 8)))) == 0) {
					break;

				}
			} // found zero
			if (bit > NBits)
				bit = NBits;
			AvgBitPos += bit;
		}
		return AvgBitPos / (double) NApprox;
	}

	private void Union(Vector<Long> BitV1, long NId1Offset, Vector<Long> BitV2,
			long NId2Offset) {
		long DstI = BitV1.firstElement() + NId1Offset;
		long SrcI = BitV2.firstElement() + NId2Offset;
		for (int b = 0; b < ApproxBytes * NBits; b++, DstI++, SrcI++) {
			DstI = DstI | SrcI;
		}
	}

	private long GetNIdOffset(V v) {
		return NIdToBitPosH.get(v);
	}

	private void InitAnfBits(Vector<Long> BitV) {
		final int NNodes = Graph.vertexSet().size();
		final int LogNodes = (int) Math
				.ceil(Math.log10(NNodes) / Math.log10(2));
		ApproxBytes = NApprox / 8;
		NBits = LogNodes + MoreBits; // bits per node
		final int BytesPerNd = ApproxBytes * NBits; // total bytes per node
		long VSize = (((long) NNodes * (long) BytesPerNd) / 4) + 1;
		if (VSize > Integer.MAX_VALUE) {
			System.err
					.printf("Your graph is too large for Approximate Neighborhood Function, %s is larger than %d",
							VSize, Integer.MAX_VALUE);
		}
		System.out.printf("size %d\n", (int) VSize);
		for (int i = 0; i < VSize; i++)
			BitV.add(0l);

		assert (BitV.firstElement() != null);

		int SetBit = 0;
		long NodeOff = 0l;
		int BitVPt = 0;
		// for each node: 1st bits of all approximations are at BitV[Offset+0],
		// 2nd bits at BitV[Offset+NApprox/32], ...
		for (V v : Graph.vertexSet()) {
			NIdToBitPosH.put(v, NodeOff);
			// init vertex bits
			for (int approx = 0; approx < NApprox; approx++) {
				final int RndNum = Rnd.GetUniDevInt(0);
				for (SetBit = 0; (RndNum & (1 << SetBit)) == 0
						&& SetBit < NBits; SetBit++) {
				}
				if (SetBit >= NBits)
					SetBit = NBits - 1;
				final int BitPos = ApproxBytes * SetBit + approx / 8;
				BitVPt = (int) (BitVPt + NodeOff + BitPos | (1 << (approx % 8)));
				BitV.set(BitVPt, (long) (1 << (approx % 8))); // magically works
																// better than
																// code below
																// (see anf.c)
			}
			NodeOff += BytesPerNd;
		}
	}

	public double GetAnfEffDiam(Vector<Pair<Integer, Float>> DistNbrsCdfV,
			double Percentile) {
		final double EffPairs = Percentile * DistNbrsCdfV.lastElement().p2;
		int ValN;
		for (ValN = 0; ValN < DistNbrsCdfV.size(); ValN++) {
			if (DistNbrsCdfV.get(ValN).p2 > EffPairs) {
				break;
			}
		}
		if (ValN >= DistNbrsCdfV.size()) {
			return DistNbrsCdfV.lastElement().p1;
		}
		if (ValN == 0)
			return 1;
		// interpolate
		final double DeltaNbrs = DistNbrsCdfV.get(ValN).p2
				- DistNbrsCdfV.get(ValN - 1).p2;
		if (DeltaNbrs == 0)
			return DistNbrsCdfV.get(ValN).p1;
		return DistNbrsCdfV.get(ValN - 1).p1
				+ (EffPairs - DistNbrsCdfV.get(ValN - 1).p2) / DeltaNbrs;
	}


	public static double CalcEffDiamPdf(
			Vector<Pair<Integer, Float>> DistNbrsPdfV, double Percentile) {
		  Vector<Pair<Integer, Float>> CdfV = GraphUtil.GetCdf(DistNbrsPdfV);		  
		  return CalcEffDiam(CdfV, Percentile);
	}

	private static double CalcEffDiam(
			Vector<Pair<Integer, Float>> DistNbrsCdfV, double Percentile) {
		final double EffPairs = Percentile * DistNbrsCdfV.lastElement().p2;
		int ValN;
		for (ValN = 0; ValN < DistNbrsCdfV.size(); ValN++) {
			if (DistNbrsCdfV.get(ValN).p2 > EffPairs) {
				break;
			}
		}
		if (ValN >= DistNbrsCdfV.size()){
			return DistNbrsCdfV.lastElement().p1;
		}
		if (ValN == 0){
			return 1;
		}
		// interpolate
		final double DeltaNbrs = DistNbrsCdfV.get(ValN).p2
				- DistNbrsCdfV.get(ValN - 1).p2;
		if (DeltaNbrs == 0){
			return DistNbrsCdfV.get(ValN).p1;
		}
		return DistNbrsCdfV.get(ValN - 1).p1
				+ (EffPairs - DistNbrsCdfV.get(ValN - 1).p2) / DeltaNbrs;
	}
	

	// / Returns a given Percentile of the shortest path length distribution of
	// a Graph (based on a single run of ANF of approximation quality NApprox).
	// / @param IsDir false: consider links as undirected (drop link
	// directions).
	// double GetAnfEffDiam(const PGraph& Graph, const bool& IsDir, const
	// double& Percentile, const int& NApprox);
	// / Returns a 90-th percentile of the shortest path length distribution of
	// a Graph (based on a NRuns runs of ANF of approximation quality NApprox).
	// / @param IsDir false: consider links as undirected (drop link
	// directions).
	// double GetAnfEffDiam(const PGraph& Graph, const int NRuns=1, int
	// NApprox=-1);

}
