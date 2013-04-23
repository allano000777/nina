package edu.nd.nina.generate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.nd.nina.Graph;
import edu.nd.nina.Type;
import edu.nd.nina.VertexFactory;
import edu.nd.nina.graph.ClassBasedVertexFactory;
import edu.nd.nina.structs.Triple;

/**
 * 
 * @author Tim Weninger
 * @since
 */
public class TypedNetworkGenerator<E> implements GraphGenerator<Type, E, Type> {
	public enum TypeEdgeTopology{random, scaleFree};
	
	// ~ Static fields/initializers
	// ---------------------------------------------
	private static long seedUniquifier = 8682522807148012L;

	// ~ Instance fields
	// --------------------------------------------------------
	private Map<Class<? extends Type>, Integer> types;
	private List<Triple<Class<? extends Type>, Class<? extends Type>, TypeEdgeTopology>> generators;
	private List<Triple<Class<? extends Type>, Class<? extends Type>, Float>> edgeProb;
	private long seed;

	protected Random rand;

	// ~ Constructors
	// -----------------------------------------------------------

	public TypedNetworkGenerator(
			Map<Class<? extends Type>, Integer> types,
			List<Triple<Class<? extends Type>, Class<? extends Type>, TypeEdgeTopology>> generators,
			List<Triple<Class<? extends Type>, Class<? extends Type>, Float>> edgeProb,
			long seed) {
		this.types = types;
		this.generators = generators;
		this.edgeProb = edgeProb;

		if (seed == 0) {
			this.seed = chooseRandomSeedOnce();
			this.rand = new Random(this.seed);
		} else {
			this.seed = seed;
			this.rand = new Random(this.seed);
		}
	}

	// ~ Methods
	// ----------------------------------------------------------------

	/**
	 * Should be called only once on creation. Chooses a seed which can be used
	 * later to reset the randomizer before each method call. This
	 * implementation copies the java.util.Random constructor because there is
	 * no getSeed() there, and seed is protected.
	 * 
	 * @author Tim Weninger
	 * @since Apr 22, 2013
	 */
	private synchronized static long chooseRandomSeedOnce() {
		return (++seedUniquifier + System.nanoTime());
	}

	/**
	 * {@inheritDoc}
	 */
	public void generateGraph(Graph<Type, E> target,
			VertexFactory<Type> vertexFactory, Map<String, Type> resultMap) {

		// key = generation order (1st,2nd,3rd,...) value=vertex Object
		// will be used later

		for (int i = 0; i < generators.size(); i++) {

			Map<Integer, Type> map1 = new HashMap<Integer, Type>();
			Map<Integer, Type> map2 = new HashMap<Integer, Type>();

			Class<? extends Type> type1 = generators.get(i).v1;
			Class<? extends Type> type2 = generators.get(i).v2;
			TypeEdgeTopology gen = generators.get(i).v3;

			int numVertices = types.get(type1);
			ClassBasedVertexFactory<Type> vf = new ClassBasedVertexFactory<Type>(
					type1);
			for (int k = 0; k < numVertices; k++) {
				Type currVertex = vf.createVertex(String.class,
						String.valueOf(k));
				target.addVertex(currVertex);
				map1.put(Integer.valueOf(k), currVertex);
			}

			numVertices = types.get(type2);
			vf = new ClassBasedVertexFactory<Type>(type2);
			for (int k = 0; k < numVertices; k++) {
				Type currVertex = vf.createVertex(String.class,
						String.valueOf(k));
				target.addVertex(currVertex);
				map2.put(Integer.valueOf(k), currVertex);
			}

			TypedEdgeTopologyFactory<E> tetf = null;
			
			switch(gen){
			case random:
				tetf = new RandomTypedEdgeTopologyFactory();
				break;
			case scaleFree:
				tetf = new ScaleFreeTypedEdgeTopologyFactory();
				break;
			default:
				tetf = new RandomTypedEdgeTopologyFactory();
			}

			Float ep = edgeProb.get(i).v3;

			tetf.createEdges(target, map1, map2, ep, rand);

		}
	}

	// ~ Inner Interfaces
	// -------------------------------------------------------

	/**
	 * This class is used to generate the edge topology for a typed graph.
	 * 
	 * @author Tim Weninger
	 * @since Apr 22, 2013
	 */
	public interface TypedEdgeTopologyFactory<E> {
		/**
		 * Two different calls to the createEdges() with the same parameters
		 * must result in the generation of the same. But if the randomizer is
		 * different, it should, usually, create different edge topology.
		 * 
		 * @param targetGraph
		 *            - guaranteed to start with zero edges.
		 * @param orderToVertexMap
		 *            - key=Integer of vertex order . between zero to
		 *            numOfVertexes (exclusive). value = vertex from the graph.
		 *            unique.
		 * @param numberOfEdges
		 *            - to create in the graph
		 * @param randomizer
		 */
		public void createEdges(Graph<Type, E> targetGraph,
				Map<Integer, Type> map1, Map<Integer, Type> map2,
				float edgeProb, Random randomizer);

	}

	// ~ Inner Classes
	// ----------------------------------------------------------

	/**
	 * Default implementation of the TypedEdgeTopologyFactory interface.
	 * randomly chooses an edge and tries to add it. If the add fails from any
	 * reason (like: self edge / multiple edges in unpermitted graph type) it
	 * will just choose another and try again. Performance: <li>when the number
	 * of possible edges becomes slim , this class will have a very poor
	 * performance , cause it will not use greedy methods to choose them. for
	 * example : In simple graph , if #V = N (#x = number Of x) and we want full
	 * mesh #edges= N*(N-1)/2 , the first added edges will do so quickly (O(1) ,
	 * the last will take O(N^2). So , do not use it in this kind of graphs. <li>
	 * If the numberOfEdges is bigger than what the graph can add, there will be
	 * an infinite loop here. It is not tested.
	 * 
	 * @author Tim Weninger
	 * @since Apr 22, 2013
	 */
	public class RandomTypedEdgeTopologyFactory implements
			TypedEdgeTopologyFactory<E> {
		public void createEdges(Graph<Type, E> targetGraph,
				Map<Integer, Type> map1, Map<Integer, Type> map2,
				float edgeProb, Random randomizer) {
			int numVertices1 = map1.size();
			int numVertices2 = map2.size();
			int numberOfEdges = (int) (edgeProb * (float) (numVertices1 * numVertices2));
			int iterationsCounter = 0;
			int edgesCounter = 0;
			while (edgesCounter < numberOfEdges) {
				// randomizer.nextInt(int n) return a number between zero
				// (inclusive) and n(exclusive)
				Type startVertex = map1.get(Integer.valueOf(randomizer
						.nextInt(numVertices1)));
				Type endVertex = map2.get(Integer.valueOf(randomizer
						.nextInt(numVertices2)));
				try {
					E resultEdge = targetGraph.addEdge(startVertex, endVertex);
					if (resultEdge != null) {
						edgesCounter++;
					}
				} catch (Exception e) {
					// do nothing.just ignore the edge
				}

				iterationsCounter++;
			}
		}
	}

	public class ScaleFreeTypedEdgeTopologyFactory implements
			TypedEdgeTopologyFactory<E> {
		public void createEdges(Graph<Type, E> target,
				Map<Integer, Type> map1, Map<Integer, Type> map2,
				float edgeProb, Random random) {
			if(map1.size() < map2.size()){
				Map<Integer, Type> tmp = map1;
				map1 = map2;
				map2 = tmp;
			}
	        
	        List<Integer> degrees = new ArrayList<Integer>();
	        int degreeSum = 0;
	        for (int i = 0; i < map1.size(); i++) {
	            Type newVertex = map1.get(i);	            
	            int newDegree = 0;
	            while ((newDegree == 0) && (i != 0)) // we want our graph to be
	                                                 // connected

	            {
	                for (int j = 0; j < Math.min(degrees.size(), map2.size()); j++) {
	                    if ((degreeSum == 0)
	                        || (random.nextInt(degreeSum) < degrees.get(j)))
	                    {
	                        degrees.set(j, degrees.get(j) + 1);
	                        newDegree++;
	                        degreeSum += 1; //not two because its just on a single type
	                        target.addEdge(map2.get(j), newVertex);
	                    }
	                }
	            }
	            degrees.add(newDegree);
	        }
		}
	}

}
