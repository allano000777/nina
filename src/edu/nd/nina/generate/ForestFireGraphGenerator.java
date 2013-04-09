
package edu.nd.nina.generate;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import edu.nd.nina.DirectedGraph;
import edu.nd.nina.Graph;
import edu.nd.nina.VertexFactory;
import edu.nd.nina.math.Randoms;

enum StopReason {srOK, srFlood, srTimeLimit};

/**
 *
 * @author Tim Weninger
 * @since 
 */
public class ForestFireGraphGenerator<V, E>
    implements GraphGenerator<V, E, V>
{
    //~ Instance fields --------------------------------------------------------
	private Boolean BurnExpFire;   // burn Exponential or Geometric fire
	private Integer StartNodes;     // start a graph with N isolated nodes
	private Float FwdBurnProb, BckBurnProb, ProbDecay; // Forest Fire parameters
	private Float Take2AmbProb, OrphanProb;
	private int timeLimitSec, NNodes;
	private Boolean FloodStop;
	
    //~ Constructors -----------------------------------------------------------

	public ForestFireGraphGenerator(boolean BurnExpFireP, int StartNNodes,
			Float ForwBurnProb, Float BackBurnProb, Float DecayProb,
			Float Take2AmbasPrb, Float OrphanPrb) {
		BurnExpFire = BurnExpFireP;
		StartNodes = StartNNodes;
		FwdBurnProb = ForwBurnProb;
		BckBurnProb = BackBurnProb;
		ProbDecay = DecayProb;
		Take2AmbProb = Take2AmbasPrb;
		OrphanProb = OrphanPrb;
		timeLimitSec = 30*60;
		NNodes = 0;
		FloodStop = true;
	}

    //~ Methods ----------------------------------------------------------------

	/**
     * {@inheritDoc}
     */
    public void generateGraph(
        Graph<V, E> target,
        VertexFactory<V> vertexFactory,
        Map<String, V> resultMap)
    {
    	if(target instanceof DirectedGraph<?,?>){
    		DirectedGraph<V,E> g = (DirectedGraph<V,E>)target;
    		addNodes(g, vertexFactory, NNodes, FloodStop);
    	}else{
    		throw new IllegalArgumentException("Graph needs to be directed");
    	}		
	}

    /**
     * 
     * @param target
     * @param vertexFactory
     * @param GraphNodes
     * @param floodStop
     * @return
     */
	private StopReason addNodes(DirectedGraph<V, E> target,
			VertexFactory<V> vertexFactory, int GraphNodes, Boolean floodStop) {
		System.out
				.printf("\n***ForestFire:  %s  Nodes:%d  StartNodes:%d  Take2AmbProb:%g\n",
						BurnExpFire ? "ExpFire" : "GeoFire", GraphNodes,
						StartNodes, Take2AmbProb);
		System.out
				.printf("                FwdBurnP:%g  BckBurnP:%g  ProbDecay:%g  Orphan:%g\n",
						FwdBurnProb, BckBurnProb, ProbDecay, OrphanProb);
		Long ExeTm = System.currentTimeMillis();
		int Burned1 = 0, Burned2 = 0, Burned3 = 0; // last 3 fire sizes

		// create initial set of nodes
		if (target.vertexSet().size() == 0) {
			for (int n = 0; n < StartNodes; n++) {
				target.addVertex(vertexFactory.createVertex());
			}
		}
		int NEdges = target.edgeSet().size();

		// forest fire
		Randoms r = new Randoms(0);
		ForestFire<V, E> FF = new ForestFire<V, E>(target, FwdBurnProb,
				BckBurnProb, ProbDecay, r);
		// add nodes
		for (int NNodes = target.vertexSet().size() + 1; NNodes <= GraphNodes; NNodes++) {
			V NewId = vertexFactory.createVertex();
			target.addVertex(NewId);
			// not an Orphan (burn fire)
			if (OrphanProb == 0.0 || r.GetUniDev() > OrphanProb) {
				// infect ambassadors
				if (Take2AmbProb == 0.0 || r.GetUniDev() > Take2AmbProb
						|| target.vertexSet().size() - 1 < 2) {
					FF.Infect(target.randomVertex(r)); // take 1 ambassador
				} else {
					final V AmbassadorNId1 = target.randomVertex(r);
					V AmbassadorNId2 = target.randomVertex(r);
					while (AmbassadorNId1 == AmbassadorNId2) {
						AmbassadorNId2 = target.randomVertex(r);
					}
					Vector<V> v = new Vector<V>();
					v.add(AmbassadorNId1);
					v.add(AmbassadorNId2);
					FF.Infect(v); // take 2 ambassadors
				}
				// burn fire
				if (BurnExpFire) {
					FF.BurnExpFire();
				} else {
					FF.BurnGeoFire();
				}
				// add edges to burned nodes
				for (int e = 0; e < FF.GetBurned(); e++) {
					target.addEdge(NewId, FF.GetBurnedNId(e));
					NEdges++;
				}
				Burned1 = Burned2;
				Burned2 = Burned3;
				Burned3 = FF.GetBurned();
			} else {
				// Orphan (zero out-links)
				Burned1 = Burned2;
				Burned2 = Burned3;
				Burned3 = 0;
			}
			if (NNodes % 1000 == 0) {
				System.out.printf("(%d, %d)  burned: [%d,%d,%d]  [%s]\n",
						NNodes, NEdges, Burned1, Burned2, Burned3,
						System.currentTimeMillis() - ExeTm);
			}
			// average node degree is more than 500
			if (FloodStop && NEdges > GraphNodes
					&& (NEdges / (double) NNodes > 1000.0)) {
				System.out.printf(". FLOOD. G(%6d, %6d)\n", NNodes, NEdges);
				return StopReason.srFlood;
			}
			if (NNodes % 1000 == 0
					&& timeLimitSec > 0
					&& (System.currentTimeMillis() - ExeTm) / 1000d > timeLimitSec) {
				System.out.printf(". TIME LIMIT. G(%d, %d)\n", target
						.vertexSet().size(), target.edgeSet().size());
				return StopReason.srTimeLimit;
			}
		}
		assert (target.edgeSet().size() == NEdges);
		return StopReason.srOK;
	}

	public void setNumNodes(int nNodes) {
		NNodes = nNodes;
	}

	public void setFloodStop(boolean b) {
		FloodStop = b;
	}

	public String getParamString() {
		return String
				.format("%s  FWD:%g  BCK:%g, StartNds:%d, Take2:%g, Orphan:%g, ProbDecay:%g",
						BurnExpFire ? "EXP" : "GEO", FwdBurnProb, BckBurnProb,
						StartNodes, Take2AmbProb, OrphanProb, ProbDecay);

	}
}

class ForestFire<V, E> {

	Randoms Rnd;
	DirectedGraph<V, E> Graph;
	Float FwdBurnProb, BckBurnProb, ProbDecay;
	// nodes to start fire
	Vector<V> InfectNIdV;
	// nodes that got burned (FF model creates edges to them)
	Vector<V> BurnedNIdV;
	// statistics
	// total burned, currently burning, newly burned in current time step
	Vector<Integer> NBurnedTmV, NBurningTmV, NewBurnedTmV;

	public ForestFire(DirectedGraph<V, E> target, Float fwdBurnProb, Float bckBurnProb,
			Float probDecay, Randoms r) {
		Graph = target;
		FwdBurnProb = fwdBurnProb;
		BckBurnProb = bckBurnProb;
		ProbDecay = probDecay;
		Rnd = r;
		
		NBurnedTmV = new Vector<Integer> ();
		NBurningTmV = new Vector<Integer> ();
		NewBurnedTmV = new Vector<Integer> ();
	}

	public int GetBurned() {
		return BurnedNIdV.size();
	}
	
	public V GetBurnedNId(int n){
		return BurnedNIdV.get(n);
	}


	public void BurnGeoFire() {
		final Float OldFwdBurnProb = FwdBurnProb;
		final Float OldBckBurnProb = BckBurnProb;
		final int NInfect = InfectNIdV.size();
		// const TNGraph& G = *Graph;

		// burned nodes
		Hashtable<Integer, V> BurnedNIdH = new Hashtable<Integer, V>();
		// currently burning nodes
		Vector<V> BurningNIdV = InfectNIdV;
		// nodes newly burned in current step
		Vector<V> NewBurnedNIdV = new Vector<V>();
		// has unburned neighbors
		boolean HasAliveInNbrs = false, HasAliveOutNbrs = false;
		// NIds of alive neighbors
		Vector<V> AliveNIdV = new Vector<V>();                

		int NBurned = NInfect, time;
		for (int i = 0; i < InfectNIdV.size(); i++) {
			BurnedNIdH.put(i, InfectNIdV.get(i));
		}
		NBurnedTmV.clear();
		NBurningTmV.clear();
		NewBurnedTmV.clear();
		for (time = 0;; time++) {
			NewBurnedNIdV.clear();
			// for each burning node
			for (int i = 0; i < BurningNIdV.size(); i++) {
				V Node = BurningNIdV.get(i);

				// find unburned links
				HasAliveOutNbrs = false;
				// unburned links
				AliveNIdV.clear();
				// burn forward links (out-links)
				for (E e : Graph.outgoingEdgesOf(Node)) {
					V OutNId = Graph.getEdgeTarget(e);
					// not yet burned
					if (!BurnedNIdH.containsKey(OutNId)) {
						HasAliveOutNbrs = true;
						AliveNIdV.add(OutNId);
					}
				}

				// number of links to burn (geometric coin). Can also burn 0
				// links
				final int BurnNFwdLinks = Rnd.GetGeoDev(1.0 - FwdBurnProb) - 1;
				if (HasAliveOutNbrs && BurnNFwdLinks > 0) {
					Collections.shuffle(AliveNIdV, Rnd.getRandom());
					for (int j = 0; j < Math.min(BurnNFwdLinks,
							AliveNIdV.size()); j++) {
						BurnedNIdH.put(NBurned, AliveNIdV.get(j));
						NewBurnedNIdV.add(AliveNIdV.get(j));
						NBurned++;
					}
				}

				// backward links
				if (BckBurnProb > 0.0) {
					// find unburned links
					HasAliveInNbrs = false;
					AliveNIdV.clear();
					for (E e : Graph.incomingEdgesOf(Node)) {
						V InNId = Graph.getEdgeSource(e);
						if (!BurnedNIdH.containsKey(InNId)) { // not yet burned
							HasAliveInNbrs = true;
							AliveNIdV.add(InNId);
						}
					}
				}
				// number of links to burn (geometric coin). Can also burn 0
				// links
				final int BurnNBckLinks = Rnd.GetGeoDev(1.0 - BckBurnProb) - 1;
				if (HasAliveInNbrs && BurnNBckLinks > 0) {
					Collections.shuffle(AliveNIdV, Rnd.getRandom());
					for (int j = 0; j < Math.min(BurnNBckLinks,
							AliveNIdV.size()); j++) {
						BurnedNIdH.put(NBurned, AliveNIdV.get(j));
						NewBurnedNIdV.add(AliveNIdV.get(j));
						NBurned++;
					}
				}
			}
			NBurnedTmV.add(NBurned);
			NBurningTmV.add(BurningNIdV.size());
			NewBurnedTmV.add(NewBurnedNIdV.size());
			// BurningNIdV.AddV(NewBurnedNIdV); // node is burning eternally
			
			//SWAP
			// node is burning just 1 time step			
			Vector<V> t = BurningNIdV;
			BurningNIdV = NewBurnedNIdV;
			NewBurnedNIdV = t;
			
			if (BurningNIdV.isEmpty()) {
				break;
			}
			FwdBurnProb = FwdBurnProb * ProbDecay;
			BckBurnProb = BckBurnProb * ProbDecay;
		}
		BurnedNIdV = new Vector<V>(BurnedNIdH.size());
		for (int i = 0; i < BurnedNIdH.size(); i++) {
			BurnedNIdV.add(BurnedNIdH.get(i));
		}
		FwdBurnProb = OldFwdBurnProb;
		BckBurnProb = OldBckBurnProb;
	}

	// burn each link independently (forward with FwdBurnProb, backward with
	// BckBurnProb)
	public void BurnExpFire() {
		final Float OldFwdBurnProb = FwdBurnProb;
		final Float OldBckBurnProb = BckBurnProb;
		final int NInfect = InfectNIdV.size();
		// const TNGraph& G = *Graph;

		// burned nodes
		Hashtable<Integer, V> BurnedNIdH = new Hashtable<Integer, V>();
		// currently burning nodes
		Vector<V> BurningNIdV = InfectNIdV;
		// nodes newly burned in current step
		Vector<V> NewBurnedNIdV = new Vector<V>();
		// has unburned neighbors
		boolean HasAliveNbrs;

		int NBurned = NInfect, NDiedFire = 0;
		for (int i = 0; i < InfectNIdV.size(); i++) {
			BurnedNIdH.put(i, InfectNIdV.get(i));
		}
		NBurnedTmV.clear();
		NBurningTmV.clear();
		NewBurnedTmV.clear();
		for (int time = 0;; time++) {
			NewBurnedNIdV.clear();
			// for each burning node
			for (int i = 0; i < BurningNIdV.size(); i++) {
				V Node = BurningNIdV.get(i);

				HasAliveNbrs = false;
				NDiedFire = 0;
				// burn forward links (out-links)
				for (E e : Graph.outgoingEdgesOf(Node)) {
					V OutNId = Graph.getEdgeTarget(e);
					// not yet burned
					if (!BurnedNIdH.containsKey(OutNId)) {
						HasAliveNbrs = true;
						if (Rnd.GetUniDev() < FwdBurnProb) {
							BurnedNIdH.put(NBurned, OutNId);
							NewBurnedNIdV.add(OutNId);
							NBurned++;
						}
					}
				}
				// burn backward links (in-links)
				if (BckBurnProb > 0.0) {
					for (E e : Graph.incomingEdgesOf(Node)) {
						V InNId = Graph.getEdgeSource(e);
						if (!BurnedNIdH.containsKey(InNId)) { // not yet burned
							HasAliveNbrs = true;
							if (Rnd.GetUniDev() < BckBurnProb) {
								BurnedNIdH.put(NBurned, InNId);
								NewBurnedNIdV.add(InNId);
								NBurned++;
							}
						}
					}
				}
				if (!HasAliveNbrs) {
					NDiedFire++;
				}
			}
			NBurnedTmV.add(NBurned);
			NBurningTmV.add(BurningNIdV.size() - NDiedFire);
			NewBurnedTmV.add(NewBurnedNIdV.size());
			// BurningNIdV.AddV(NewBurnedNIdV); // node is burning eternally
			
			//SWAP
			// node is burning just 1 time step			
			Vector<V> t = BurningNIdV;
			BurningNIdV = NewBurnedNIdV;
			NewBurnedNIdV = t;
			
			if (BurningNIdV.isEmpty()) {
				break;
			}
			FwdBurnProb = FwdBurnProb * ProbDecay;
			BckBurnProb = BckBurnProb * ProbDecay;
		}
		BurnedNIdV = new Vector<V>(BurnedNIdH.size());
		for (int i = 0; i < BurnedNIdH.size(); i++) {
			BurnedNIdV.add(BurnedNIdH.get(i));
		}
		FwdBurnProb = OldFwdBurnProb;
		BckBurnProb = OldBckBurnProb;
	}


	public void Infect(V Node) {
		InfectNIdV = new Vector<V>(1);
		InfectNIdV.add(Node);
	}

	public void Infect(Vector<V> Node) {
		InfectNIdV = new Vector<V>(1);
		for (int i = 0; i < Node.size(); i++) {
			InfectNIdV.add(Node.get(i));
		}
	}

}

