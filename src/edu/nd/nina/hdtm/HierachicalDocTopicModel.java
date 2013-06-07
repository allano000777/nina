package edu.nd.nina.hdtm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

import edu.nd.nina.graph.DefaultEdge;
import edu.nd.nina.graph.DirectedFeatureGraph;
import edu.nd.nina.graph.load.LoadFromHBase;
import edu.nd.nina.io.FeatureGraph;
import edu.nd.nina.math.Randoms;
import edu.nd.nina.types.Alphabet;
import edu.nd.nina.types.FeatureSequence;
import edu.nd.nina.types.Instance;
import edu.nd.nina.util.ValueSorter;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

/**
 * 
 * Hierarchical Document Topic Models
 * 
 * Generates a hierarchy from a directed feature graph. See Weninger, Bisk, Han.
 * "Hierarchical Document Topic Models" in CIKM 2012 for details.
 * 
 * @author Tim Weninger 4/9/2013
 * 
 */
public class HierachicalDocTopicModel {

	/** Writer to output hierarchy with best log likelihood */
	private PrintWriter bestHierarchyWriter;

	/** Writer for log likelihood traces */
	private PrintWriter logLikelihoodTraceWriter;

	/** Graph of the dataset. Vertices are Instances, Edges are empty */
	private DirectedFeatureGraph<Instance, DefaultEdge> graph;

	/**
	 * Root vertex in the directedFeatureGraph. Input to the
	 * HierarchicalDocTopicModel must be a rooted graph. Currently, the first
	 * node in the input file is designated to be the root
	 */
	private Instance root;

	/** */
	private RWRNode rootNode;

	/** Number of "instances" (documents/vertices probably) in the graph. */
	private int numInstances;

	/** Number of "features" (words probably) in the graph data */
	private int numTypes;

	/** LDA-style smoothing on topic distributions */
	private final double alpha;

	/** LDA-style smoothing on word distributions */
	private final double eta;

	/** LDA-style sum of the eta values */
	private double etaSum;

	/** Restart probability for random walk with restart */
	private final double gamma;

	/** Level in the induced hierarchy indexed with <doc, token> */
	private int[][] levels;

	/**
	 * Array of nodes in the hierarchy. Parents constitute the currently
	 * selected path through the hierarchy
	 */
	private RWRNode[] hierarchyNodes;

	/** Random number utility class */
	private Randoms random;

	/** Shows algorithm progress if true */
	private boolean showProgress = true;

	/** Interval between topic outputs */
	private int displayTopicsInterval = 50;

	/** Number of words to print during output */
	private int numWordsToDisplay = 10;

	/**
	 * Constructor creates empty DirectedFeatureGraph and sets parameters to
	 * default values
	 */
	public HierachicalDocTopicModel(double alpha, double gamma, double eta) {
		graph = new DirectedFeatureGraph<Instance, DefaultEdge>(
				DefaultEdge.class, LoadFromHBase.createPipe());
		this.alpha = alpha;
		this.gamma = gamma;
		this.eta = eta;
	}

	/**
	 * Mutator for log likelihood trace writer
	 * 
	 * @param logLikelihoodTraceWriter
	 *            Writer to print log likelihood trace
	 */
	public void setLLTrace(PrintWriter logLikelihoodTraceWriter) {
		this.logLikelihoodTraceWriter = logLikelihoodTraceWriter;
	}

	/**
	 * Mutator for bestgraph trace writer
	 * 
	 * @param bestGraphWriter
	 *            Writer to print best graph
	 */
	public void setBestGraph(PrintWriter bestGraphWriter) {
		this.bestHierarchyWriter = bestGraphWriter;
	}

	/**
	 * Mutator for progress display parameters
	 * 
	 * @param displayTopicsInterval
	 *            Interval between topic outputs
	 * @param numWordsToDisplay
	 *            K-top words to display when showing topics
	 */
	public void setTopicDisplay(int displayTopicsInterval, int numWordsToDisplay) {
		this.displayTopicsInterval = displayTopicsInterval;
		this.numWordsToDisplay = numWordsToDisplay;
	}

	/**
	 * This parameter determines whether the sampler outputs shows progress by
	 * outputting a character after every iteration.
	 * 
	 * @param showProgress
	 *            Show algorithm progress if true
	 */
	public void setProgressDisplay(boolean showProgress) {
		this.showProgress = showProgress;
	}

	/**
	 * 
	 * @param featureGraphFile
	 * @param random
	 */
	public void initialize(String featureGraphFile, Randoms random) {
		this.random = random;
		this.root = FeatureGraph.loadFeatureGraphFromWikiHbase(
				featureGraphFile, graph);

		if (!(graph.getInstances().get(0).getData() instanceof FeatureSequence)) {
			throw new IllegalArgumentException(
					"Input must be a FeatureSequence");
		}

		numInstances = graph.getInstances().size();
		numTypes = graph.getInstances().getDataAlphabet().size();

		etaSum = eta * numTypes;

		rootNode = new RWRNode(numTypes, root);

		levels = new int[numInstances][];
		hierarchyNodes = new RWRNode[numInstances];

		// Vertices without a proper id should be removed
		ArrayList<Instance> removes = new ArrayList<Instance>();
		for (Instance pn : graph.vertexSet()) {
			if (pn.getSource() == null) {
				removes.add(pn);
			}
		}

		for (Instance ins : removes) {
			graph.removeVertex(ins);
		}

		// The initial hierarchy is a breadth first iteration of the original
		// graph starting from the predefined root node.
		Map<RWRNode, RWRNode> c = new HashMap<RWRNode, RWRNode>();
		Map<Instance, RWRNode> m = new HashMap<Instance, RWRNode>();

		Set<Instance> mark = new HashSet<Instance>();
		Queue<RWRNode> Q = new LinkedList<RWRNode>();
		c.put(rootNode, null);
		m.put(rootNode.ins, rootNode);
		mark.add(rootNode.ins);
		Q.add(rootNode);

		while (!Q.isEmpty()) {
			RWRNode t = Q.poll();

			for (DefaultEdge te : graph.outgoingEdgesOf(t.ins)) {
				Instance o = graph.getEdgeTarget(te);

				if (!mark.contains(o)) {
					RWRNode x = t.addChild(o);
					Q.add(x);
					mark.add(o);
					m.put(o, x);
					c.put(x, t);
				}
			}
		}
		
		//remove non linked
		for (Instance ins : graph.vertexSet()) {
			RWRNode rwrp = m.get(ins);
			if (rwrp == null) {
				removes.add(ins);
				continue;
			}
		}
		
		for (Instance ins : removes) {
			graph.removeVertex(ins);
		}
		

		// Initialize and fill the topic pointer arrays for every document. Set
		// everything to the breadth first hierarchy that we added earlier.
		for (Instance ins : graph.vertexSet()) {
			FeatureSequence fs = (FeatureSequence) ins.getData();
			int seqLen = fs.getLength();

			LinkedList<RWRNode> path = new LinkedList<RWRNode>();
			RWRNode rwrp = m.get(ins);
			if (rwrp == null) {
				continue;
			}

			do {
				rwrp.customers++;
				path.addFirst(rwrp);
				rwrp = c.get(rwrp);
			} while (rwrp != null);

			RWRNode node = path.getLast();

			assert (levels[(Integer) ins.getSource()] != null);

			levels[(Integer) ins.getSource()] = new int[seqLen];
			hierarchyNodes[(Integer) ins.getSource()] = node;

			for (int token = 0; token < seqLen; token++) {
				int type = fs.getIndexAtPosition(token);
				levels[(Integer) ins.getSource()][token] = random.next(path
						.size());// numLevels);
				node = path.get(levels[(Integer) ins.getSource()][token]);
				node.totalTokens++;
				node.typeCounts[type]++;
			}

			path.clear();
		}

	}

	/**
	 * This is the Gibbs sampling control method. For numIterations
	 * Gibbs-iterations, first sample a path from the root to each
	 * instance/vertex and then redistribute the topic words for each
	 * instance/vertex w.r.t its parents
	 * 
	 * @param numIterations
	 *            Number of Gibbs-iterations
	 * @param burnin
	 *            Period at the start for which no samples are taken
	 * @param sample
	 *            After burnin period, this is the number of iterations between
	 *            recording sampels
	 */
	public void estimate(int numIterations, int burnin, int sample) {
		double best = Double.NEGATIVE_INFINITY;

		for (int iteration = 1; iteration <= numIterations; iteration++) {
			// If we have passed the burnin period, and we are on a sampleable
			// iteration, then we should add the current Gibbs-sample to the
			// list of samples and possibly trace the likelihood values
			if (iteration >= burnin && iteration % sample == 0) {
				double ll = calcLogLikelihood(getTypeCounts(), 0, rootNode, 0d);

				if (ll > best) {
					printNodes(iteration, ll);
					best = ll;
				}
				double[] ret = getStats();
				logLikelihoodTraceWriter.println("\n" + iteration + "\t" + ll
						+ "\t" + ret[0] + "\t" + ret[1] + "\t" + ret[2]);
				System.out.println("\n" + iteration + "\t" + ll + "\t" + ret[0]
						+ "\t" + ret[1] + "\t" + ret[2]);
				logLikelihoodTraceWriter.flush();

				for (int doc = 0; doc < numInstances; doc++) {
					recordParent(hierarchyNodes[doc]);
				}
			}

			// First draw a path through the directedFeatureGraph for each
			// vertex
			for (Instance ins : graph.vertexSet()) {
				samplePath(ins);
			}

			// Second redistribute the topic-words for each vertex and its
			// parents
			for (Instance ins : graph.vertexSet()) {
				sampleTopics(ins);
			}

			// Print the algorithms progress
			if (showProgress) {
				System.out.print(".");
				if (iteration % displayTopicsInterval == 0) {
					System.out.println(" " + iteration);
				}
			}

		}
	}

	/**
	 * Same a path from the root to the current instance/vertex by sampling from
	 * RWR probabilities
	 * 
	 * @param ins
	 *            Current instance/vertex to which a path is drawn
	 */
	private void samplePath(Instance ins) {
		LinkedList<RWRNode> path = new LinkedList<RWRNode>();
		int doc = (Integer) ins.getSource();

		RWRNode node = hierarchyNodes[doc];
		assert (node != null);

		// root doesn't need sampled.
		if (node.parent == null)
			return;

		int depth = node.level + 1;
		hierarchyNodes[doc].dropPath();

		TObjectDoubleHashMap<RWRNode> nodeWeights = new TObjectDoubleHashMap<RWRNode>();

		// Calculate p(c_m | c_{-m})
		calculateRWR(nodeWeights, rootNode, hierarchyNodes[doc], 0.0);

		// Add weights for p(w_m | c, w_{-m}, z)

		// The path may have no further customers and therefore be unavailable,
		// but it should still exist since we haven't reset hierarchyNodes[doc]
		// yet...
		Map<Instance, Map<Integer, TIntIntHashMap>> descTypeCounts = new HashMap<Instance, Map<Integer, TIntIntHashMap>>();

		// Save the counts of every word at each level, and remove counts from
		// the current path
		Set<Instance> desc = hierarchyNodes[doc].descendents;

		for (Instance desIns : desc) {

			int desDoc = (Integer) desIns.getSource();
			node = hierarchyNodes[desDoc];

			while (node != null) {
				path.addFirst(node);
				node = node.parent;
			}

			Map<Integer, TIntIntHashMap> typeCounts = new HashMap<Integer, TIntIntHashMap>();
			descTypeCounts.put(desIns, typeCounts);

			int[] docLevels = levels[desDoc];
			FeatureSequence fs = (FeatureSequence) desIns.getData();
			for (int token = 0; token < docLevels.length; token++) {
				int level = docLevels[token];
				int type = fs.getIndexAtPosition(token);

				if (!typeCounts.containsKey(level)) {
					typeCounts.put(level, new TIntIntHashMap());
				}

				if (!typeCounts.get(level).containsKey(type)) {
					typeCounts.get(level).put(type, 1);
				} else {
					typeCounts.get(level).increment(type);
				}

				path.get(level).typeCounts[type]--;

				assert (path.get(level).typeCounts[type] >= 0);
				assert (path.get(level).totalTokens >= 0);
			}
			path.clear();
		}

		// Calculate the weight for a new path at a given level.
		double[] newTopicWeights = new double[depth];
		int[] levelTotalTokens = new int[depth];
		for (Map<Integer, TIntIntHashMap> typeCounts : descTypeCounts.values()) {
			// Skip the root...
			for (int level = 1; level < typeCounts.size() && level < depth; level++) {
				if (!typeCounts.containsKey(level)) {
					continue;
				}
				int[] types = typeCounts.get(level).keys();

				for (int t : types) {
					for (int i = 0; i < typeCounts.get(level).get(t); i++) {
						newTopicWeights[level] += Math.log((eta + i)
								/ (etaSum + levelTotalTokens[level]));
						levelTotalTokens[level]++;
					}
				}
			}
		}

		// Reevaluate the nodeWeights based on the current topic/word
		// distribution
		calculateWordLikelihood(nodeWeights, rootNode, hierarchyNodes[doc].ins,
				0.0, descTypeCounts, newTopicWeights, 0);

		RWRNode[] nodes = nodeWeights.keys(new RWRNode[] {});
		double[] parenthoodProbabilities = new double[nodes.length];
		double sum = 0.0;
		double max = Double.NEGATIVE_INFINITY;

		// To avoid underflow, we're using log weights and normalizing the node
		// weights so that the largest weight is always 1.
		for (int i = 0; i < nodes.length; i++) {
			if (nodeWeights.get(nodes[i]) > max) {
				max = nodeWeights.get(nodes[i]);
			}
		}

		for (int i = 0; i < nodes.length; i++) {
			parenthoodProbabilities[i] = Math.exp(nodeWeights.get(nodes[i])
					- max);
			sum += parenthoodProbabilities[i];
		}

		assert (parenthoodProbabilities.length > 0);

		// Draw a parent instance/vertex from the probability-set
		RWRNode newParent = nodes[random.GetDiscrete(parenthoodProbabilities,
				sum)];

		// add the picked parent to the hierarchy
		int oldLevel = hierarchyNodes[doc].level;

		hierarchyNodes[doc].parent = newParent;
		newParent.children.add(hierarchyNodes[doc]);

		hierarchyNodes[doc].addPath();

		// Reassign levels to descendants
		propagateLevelsToDesc(hierarchyNodes[doc], newParent.level + 1);

		int newLevel = hierarchyNodes[doc].level;

		RWRNode x = hierarchyNodes[doc];
		RWRNode[] newpath = new RWRNode[x.level + 1];
		for (int i = x.level; i >= 0; i--) {
			newpath[i] = x;
			x = x.parent;
		}

		x = hierarchyNodes[doc];

		for (Instance descIns : descTypeCounts.keySet()) {
			int descDoc = (Integer) descIns.getSource();
			Map<Integer, TIntIntHashMap> typeCounts = descTypeCounts
					.get(descIns);
			for (int level = oldLevel; level > newLevel; level--) {
				// new path is shorter than old path... we add counts to the
				// node
				Set<Integer> a = typeCounts.keySet();
				Integer[] b = new Integer[a.size()];
				a.toArray(b);

				for (int i : b) {
					if (i > newLevel) {
						int[] types = typeCounts.get(i).keys();

						for (int t : types) {
							if (!typeCounts.containsKey(newLevel)) {
								typeCounts.put(newLevel, new TIntIntHashMap());
							}
							if (typeCounts.get(newLevel).containsKey(t)) {
								typeCounts.get(newLevel).adjustValue(t,
										typeCounts.get(i).get(t));
							} else {
								typeCounts.get(newLevel).put(t,
										typeCounts.get(i).get(t));
							}
						}
						typeCounts.remove(i);

					}
				}

				for (int i = 0; i < levels[descDoc].length; i++) {
					if (levels[descDoc][i] > newLevel) {
						levels[descDoc][i] = newLevel;
					}
				}

			}

			x = hierarchyNodes[descDoc];
			for (int level = x.level; level >= 0; level--) {
				if (newLevel > oldLevel
						+ (hierarchyNodes[descDoc].level - level)) {
					// new path is longer than old path... we add counts to head
					// of path
					x = x.parent;
					continue;
				}

				if (!typeCounts.containsKey(level)) {
					x = x.parent;
					continue;
				}
				int[] types = typeCounts.get(level).keys();

				for (int i : typeCounts.keySet()) {
					if (i > hierarchyNodes[descDoc].level) {
						System.out.println();
					}

				}

				for (int t : types) {
					x.typeCounts[t] += typeCounts.get(level).get(t);
					x.totalTokens += typeCounts.get(level).get(t);
				}

				x = x.parent;
			}
		}

		desc = hierarchyNodes[doc].descendents;

		for (Instance descIns : desc) {
			int desDoc = (Integer) descIns.getSource();
			path.clear();
			node = hierarchyNodes[desDoc];

			while (node != null) {
				path.addFirst(node);
				node = node.parent;
			}

			assert (path.getLast().level == path.size() - 1);
		}

	}

	/**
	 * 
	 * @param ins
	 */
	private void sampleTopics(Instance ins) {
		FeatureSequence fs = (FeatureSequence) ins.getData();
		int seqLen = fs.getLength();
		int doc = (Integer) ins.getSource();
		int[] docLevels = levels[doc];

		LinkedList<RWRNode> path = new LinkedList<RWRNode>();
		RWRNode node;
		Map<Integer, Integer> levelCounts = new TreeMap<Integer, Integer>();
		int type, token, level;
		double sum;

		// calculate the depth
		node = hierarchyNodes[doc];
		int depth = 0;
		while (node != null) {
			path.addFirst(node);
			node = node.parent;
			depth++;
		}

		// Get the node
		node = hierarchyNodes[doc];
		assert (node != null);

		// Initialize levelCounts to 0
		for (int i = 0; i < path.size(); i++) {
			levelCounts.put(i, 0);
		}

		// Populate levelCounts
		for (token = 0; token < seqLen; token++) {
			int lev = docLevels[token];
			levelCounts.put(lev, levelCounts.get(lev) + 1);
		}

		double[] levelProbabilities = new double[depth];

		// Calculate probabilities for each words appearing at each level
		for (token = 0; token < seqLen; token++) {
			type = fs.getIndexAtPosition(token);

			int lev = docLevels[token];

			levelCounts.put(lev, levelCounts.get(lev) - 1);
			node = path.get(lev);
			node.typeCounts[type]--;
			node.totalTokens--;

			sum = 0.0;
			for (level = 0; level < depth; level++) {

				levelProbabilities[level] = (alpha + levelCounts.get(level))
						* (eta + path.get(level).typeCounts[type])
						/ (etaSum + path.get(level).totalTokens);

				sum += levelProbabilities[level];
			}

			// Sample a level from the probability set
			level = random.GetDiscrete(levelProbabilities, sum);

			docLevels[token] = level;

			levelCounts.put(docLevels[token],
					levelCounts.get(docLevels[token]) + 1);
			node = path.get(level);
			node.typeCounts[type]++;
			node.totalTokens++;
		}

	}

	/**
	 * Adds the current sample's parent to the list of sampled parents.
	 * 
	 * @param currentVertex
	 *            Vertex to record
	 */
	private void recordParent(RWRNode currentVertex) {
		if (currentVertex != null && currentVertex.parent != null) {
			currentVertex.addParent(currentVertex.parent.ins);
		}
	}

	/**
	 * Convenience method to calculate statistics when displaying algorithm
	 * progress
	 * 
	 * @return array of size 3 containing descriptive statistics: [0] - maximum
	 *         depth of the current hierarchy; [1] - average depth of the
	 *         current hierarchy; [2] - average degree of the current hierarchy
	 */
	private double[] getStats() {
		double[] ret = new double[3];
		double maxDepth = 0;
		double sumDepth = 0;
		double sumDegree = 0;

		for (Instance ins : graph.vertexSet()) {
			int doc = (Integer) ins.getSource();
			if (hierarchyNodes[doc] == null)
				continue;
			maxDepth = Math.max(maxDepth, hierarchyNodes[doc].level);
			sumDepth += hierarchyNodes[doc].level;
			sumDegree += hierarchyNodes[doc].children.size();
		}
		ret[0] = maxDepth;
		ret[1] = sumDepth / (double) numInstances;
		ret[2] = sumDegree / (double) numInstances;

		return ret;
	}

	/**
	 * For each document, get the count of the "type" (word/token/feature) for
	 * each level in the hierarchy
	 * 
	 * @return Map of instance/vertex id to type counts <vertex, <level, type>>
	 */
	private Map<Integer, TIntIntHashMap> getTypeCounts() {
		Map<Integer, TIntIntHashMap> typeCounts = new TreeMap<Integer, TIntIntHashMap>();

		int[] docLevels;
		for (Instance ins : graph.vertexSet()) {
			int doc = (Integer) ins.getSource();
			docLevels = levels[doc];
			if (docLevels == null)
				continue;
			FeatureSequence fs = (FeatureSequence) ins.getData();

			// Save the counts of every word at each level
			for (int token = 0; token < docLevels.length; token++) {
				int level = docLevels[token];
				int type = fs.getIndexAtPosition(token);

				if (!typeCounts.containsKey(level))
					typeCounts.put(level, new TIntIntHashMap());

				if (!typeCounts.get(level).containsKey(type)) {
					typeCounts.get(level).put(type, 1);
				} else {
					typeCounts.get(level).increment(type);
				}

			}
		}

		return typeCounts;

	}

	/**
	 * Recursively calculates the log likelihood of the current hierarchy
	 * (represented by typeCounts).
	 * 
	 * @param typeCounts
	 *            Data structure which stores the topical hierarchy
	 * @param level
	 *            Level currently under consideration (initially 0 i.e., root
	 *            level)
	 * @param node
	 *            The current node (initially root)
	 * @param weight
	 *            The current log probability (initially 0)
	 * @return Log likelihood (goodness of fit). Higher is better.
	 */
	private double calcLogLikelihood(Map<Integer, TIntIntHashMap> typeCounts,
			int level, RWRNode node, double weight) {

		// First calculate the likelihood of the words at this level, given this
		// topic/level.
		double nodeWeight = 0.0, ll = 0.0;
		// recursive base case
		if (typeCounts.get(level) == null)
			return ll;
		int[] types = typeCounts.get(level).keys();
		int totalTokens = 0;

		for (int type : types) {
			for (int i = 0; i < typeCounts.get(level).get(type); i++) {
				nodeWeight += Math.log((eta + node.typeCounts[type] + i)
						/ (etaSum + node.totalTokens + totalTokens));
				totalTokens++;
			}
		}

		// Propagate that weight to the child nodes
		for (RWRNode child : node.children) {
			nodeWeight += calcLogLikelihood(typeCounts, level + 1, child,
					weight + nodeWeight);
		}

		return nodeWeight;
	}

	/**
	 * Updates the levels of the children after reassigning its parent
	 * 
	 * @param node
	 *            Descendant Node to reassign levels
	 * @param level
	 *            New level
	 */
	private void propagateLevelsToDesc(RWRNode node, int level) {
		node.level = level;
		for (RWRNode n : node.children) {
			propagateLevelsToDesc(n, level + 1);
		}
	}

	/**
	 * Recursively calculates the probability of selecting a parent based on the
	 * random walk probability of reaching the target w.r.t the gamma restart
	 * probability. Probability is 0 if there is no edge between currentParent
	 * -> target in the original directedFeatureGraph
	 * 
	 * @param parentProbabilities
	 *            Store the probabilities of each node being the target's parent
	 * @param parentCandidate
	 *            The current parent node to have its probability calculated
	 * @param target
	 *            The node for which paths are being sampled
	 * @param parentProbability
	 *            Calculated RWR probability for currentParent to be the
	 *            target's parent
	 */
	private void calculateRWR(
			TObjectDoubleHashMap<RWRNode> parentProbabilities,
			RWRNode parentCandidate, final RWRNode target,
			double parentProbability) {
		for (RWRNode child : parentCandidate.children) {
			if (child.ins != target.ins) {
				double w = parentProbability
						+ Math.log((1 - gamma)
								/ (double) parentCandidate.children.size());
				calculateRWR(parentProbabilities, child, target, w);
			}
		}

		// Probability is 0 if there is no edge between parentCandidate ->
		// target in the original directedFeatureGraph. In fact, its not even
		// stored in the result map
		if (graph.containsEdge(parentCandidate.ins, target.ins)) {
			parentProbabilities.put(parentCandidate,
					parentProbability + Math.log(gamma));
		}
	}

	/**
	 * Calculates the probabilities for each word appearing in each
	 * topic/document. Given a set of parent probabilities calculated by RWR
	 * only, we reevaluate the parenthood probability w.r.t the words and
	 * topics.
	 * 
	 * @param parentProbabilities
	 *            Store the probabilities of each node being the target's parent
	 * @param parentCandidate
	 *            The current parent node to have its probability calculated
	 * @param target
	 *            The node for which paths are being sampled
	 * @param parentProbability
	 *            Calculated RWR probability for currentParent to be the
	 *            target's parent
	 * @param descTypeCounts
	 *            The counts of the words at each level for each
	 *            instance/vertex; this is the topic distribution along the
	 *            parent path.
	 * @param newTopicWeights
	 *            Weights of the topics along the path from root to target
	 * @param level
	 *            Current level in the hierarchy, i.e,
	 *            specificity/generalizability of the topics
	 */
	private void calculateWordLikelihood(
			TObjectDoubleHashMap<RWRNode> parentProbabilities,
			RWRNode parentCandidate, Instance target, double parentProbability,
			Map<Instance, Map<Integer, TIntIntHashMap>> descTypeCounts,
			double[] newTopicWeights, int level) {

		// First calculate the likelihood of the words at this level, given this
		// topic.
		double nodeWeight = 0.0;

		for (Map<Integer, TIntIntHashMap> typeCounts : descTypeCounts.values()) {
			if (!typeCounts.containsKey(level))
				continue;
			int[] types = typeCounts.get(level).keys();
			int totalTokens = 0;

			for (int type : types) {
				for (int i = 0; i < typeCounts.get(level).get(type); i++) {
					nodeWeight += Math
							.log((eta + parentCandidate.typeCounts[type] + i)
									/ (etaSum + parentCandidate.totalTokens + totalTokens));
					totalTokens++;
				}
			}
		}

		// Propagate that weight to the child nodes
		for (RWRNode child : parentCandidate.children) {
			if (child.descendents.contains(target) && child.ins != target) {
				calculateWordLikelihood(parentProbabilities, child, target,
						parentProbability + nodeWeight, descTypeCounts,
						newTopicWeights, level + 1);
			}
		}

		// Finally, add the weight of a new path
		level++;
		while (level < newTopicWeights.length) {
			nodeWeight += newTopicWeights[level];
			level++;
		}

		if (graph.containsEdge(parentCandidate.ins, target)) {
			assert (parentProbabilities.contains(parentCandidate));
			parentProbabilities.adjustValue(parentCandidate, nodeWeight);
		}

	}

	/**
	 * Write a text file describing the current sampling state.
	 * 
	 * @param writer
	 *            PrintWriter to which output is written
	 * @throws IOException
	 *             Thrown if writer error occurs
	 */
	public void printState(PrintWriter writer) throws IOException {

		Alphabet alphabet = graph.getInstances().getDataAlphabet();

		int count = 0;
		double sum = 0;

		for (Instance ins : graph.getInstances()) {
			int doc = (Integer) ins.getSource();
			FeatureSequence fs = (FeatureSequence) ins.getData();
			int seqLen = fs.getLength();
			int[] docLevels = levels[doc];
			RWRNode node;
			int type, token, level;

			StringBuffer path = new StringBuffer();

			// Start with the leaf, and build a string describing the path for
			// this doc
			node = hierarchyNodes[doc];
			if (node == null)
				continue;
			int depth = node.level;
			for (level = depth - 1; level >= 0; level--) {
				path.append(node.ins.getSource() + " ");
				node = node.parent;
			}

			for (token = 0; token < seqLen; token++) {
				type = fs.getIndexAtPosition(token);
				level = docLevels[token];

				count++;
				sum += level;

				// The "" just tells java we're not trying to add a string and
				// an int
				writer.println(path + "" + type + " "
						+ alphabet.lookupObject(type) + " " + level + " ");
			}
			writer.println(doc + " " + (double) sum / (double) count + " "
					+ ins.getName());

		}
		writer.flush();
	}

	/**
	 * Prints the current hierarchy to the bestGraphWriter
	 * 
	 * @param iteration
	 *            Current iteration
	 * @param logLikelihood
	 *            Likelihood of the graph
	 */
	public void printNodes(int iteration, double logLikelihood) {
		bestHierarchyWriter.println("Iteration: " + iteration);
		bestHierarchyWriter.println("LL: " + logLikelihood);
		printNode(rootNode, 0);
		bestHierarchyWriter.println("*****************************");
		bestHierarchyWriter.println();
		bestHierarchyWriter.println();
		bestHierarchyWriter.flush();
	}

	/**
	 * Recursive convenience method to help print hierarchy.
	 * 
	 * @param node
	 *            Current node to be printed (Initially root)
	 * @param indent
	 *            Number of spaced to indent for pretty printing
	 */
	private void printNode(RWRNode node, int indent) {
		StringBuffer out = new StringBuffer();
		for (int i = 0; i < indent; i++) {
			out.append("  ");
		}

		out.append(node.totalTokens + "/" + node.customers + " ");
		out.append(node.ins.getSource() + " ");
		out.append(node.getTopTypes());
		bestHierarchyWriter.println(out);

		for (RWRNode child : node.children) {
			printNode(child, indent + 1);
		}
	}

	/**
	 * Prints the final results of the algorithm. The final hierarchy is defined
	 * by selecting the most frequently sampled parent for each vertex
	 * 
	 * @param writer
	 *            PrintWriter to which output is written
	 */
	public void printResults(PrintWriter writer) {

		for (RWRNode n : hierarchyNodes) {
			int max = 0;
			if (n == null)
				continue;
			Instance best = n.ins;
			for (Entry<Instance, Integer> e : n.parentList.entrySet()) {
				if (e.getValue() > max) {
					max = e.getValue();
					best = e.getKey();
				}
			}
			writer.println(best.getSource().toString() + " -> "
					+ n.ins.getSource().toString());
		}

		writer.println();
		writer.close();
	}

	/**
	 * 
	 * Private internal class which stores the frequently modified hierarchy
	 * 
	 * @author Tim Weninger 4/9/2013
	 * 
	 */
	private class RWRNode {

		/** The vertex in the corresponding DirectedFeatureGraph */
		Instance ins;

		/** Children nodes in the hierarchy */
		List<RWRNode> children;

		/** Parent node in the hierarchy */
		RWRNode parent;

		/** Current level in the hierarchy, i.e., depth */
		int level;

		/** Set of descendants */
		Set<Instance> descendents;

		/** Number of descendants */
		int customers;

		/**
		 * Number of tokens/words in the topic corresponding to the current
		 * vertex/document
		 */
		int totalTokens;

		/**
		 * Counts of terms appearing in the topic corresponding to the current
		 * vertex/document
		 */
		int[] typeCounts;

		/**
		 * Count of sampled parents. Used to generate the final output hierarchy
		 */
		Map<Instance, Integer> parentList;

		/**
		 * Constructor
		 * 
		 * @param parent
		 *            Parent RWRNode in hierarchy or null if root
		 * @param dimensions
		 *            Number of types/words
		 * @param level
		 *            Level/depth if the new RWRNode
		 * @param ins
		 *            The corresponding instance/vertex in the
		 *            DirectedFeatureGraph
		 */
		private RWRNode(RWRNode parent, int dimensions, int level, Instance ins) {
			this.ins = ins;
			this.customers = 0;
			this.parent = parent;
			this.children = new ArrayList<RWRNode>();
			this.level = level;
			this.descendents = new HashSet<Instance>();
			this.totalTokens = 0;
			this.typeCounts = new int[dimensions];
			this.parentList = new HashMap<Instance, Integer>();
		}

		/**
		 * Constructor for root
		 * 
		 * @param dimensions
		 *            Number of types/words
		 * @param ins
		 *            The corresponding instance/vertex in the
		 *            DirectedFeatureGraph
		 */
		private RWRNode(int dimensions, Instance ins) {
			this(null, dimensions, 0, ins);
		}

		@Override
		public String toString() {
			return ins.toString();
		}

		/**
		 * Add sampled parent to the list of sampled parents
		 * 
		 * @param parent
		 *            Newly sampled parent
		 */
		private void addParent(Instance parent) {
			if (parentList.containsKey(parent)) {
				parentList.put(parent, parentList.get(parent) + 1);
			} else {
				parentList.put(parent, 1);
			}
		}

		/**
		 * Create a new RWRNode corresponding to the provided instance/vertex
		 * 
		 * @param ins
		 *            Instance/vertex to create RWRNode around
		 * @return new RWRNode surrounding the provided instance
		 */
		private RWRNode addChild(Instance ins) {
			RWRNode node = new RWRNode(this, typeCounts.length, level + 1, ins);
			children.add(node);
			RWRNode p = node.parent;
			while (p != null) {
				p.descendents.add(ins);
				p = p.parent;
			}
			return node;
		}

		/**
		 * Removes the provided node from the list of children
		 * 
		 * @param node
		 *            child node to be removed
		 */
		private void removeChild(RWRNode node) {
			children.remove(node);
		}

		/**
		 * Remove the current RWRNode from the hierarchy and update the
		 * descendants
		 */
		private void dropPath() {
			RWRNode node = this;
			int descendents = node.customers;
			node.parent.removeChild(node);
			Set<Instance> desc = node.descendents;
			desc.add(ins);

			while (node.parent != null) {
				node = node.parent;
				node.descendents.removeAll(desc);
				node.customers -= descendents;
				if (node.customers == 0) {
					node.parent.removeChild(node);
				}
			}
		}

		/**
		 * Add a new path to the current RWRNode through the hierarchy and
		 * update its descendants
		 */
		private void addPath() {
			RWRNode node = this;
			int descendents = node.customers;
			Set<Instance> desc = node.descendents;
			desc.add(ins);
			while (node.parent != null) {
				node = node.parent;

				node.descendents.addAll(desc);
				node.customers += descendents;
			}
		}

		/**
		 * Get the top K most frequent types/words
		 * 
		 * @return String with K most frequent types/words
		 */
		private String getTopTypes() {
			ValueSorter[] sortedTypes = new ValueSorter[numWordsToDisplay];

			for (int type = 0; type < numWordsToDisplay; type++) {
				sortedTypes[type] = new ValueSorter(type, typeCounts[type]);
			}
			Arrays.sort(sortedTypes);

			StringBuffer out = new StringBuffer();
			for (int i = 0; i < numWordsToDisplay; i++) {
				out.append(graph.getInstances().getAlphabet()
						.lookupObject(sortedTypes[i].getID())
						+ " ");
			}
			return out.toString();
		}

	}

	public static void main(String[] args) {

		// The filename in which to write the Gibbs sampling state after at the
		// end of the iterations
		// File dataFile = new File("./data/hdtm/illinois.edu.fg");
		File outputFile = new File("./data/hdtm/illinois.edu_output.txt");
		File resultsFile = new File("./data/hdtm/illinois.edu_results.txt");
		File lltraceFile = new File("./data/hdtm/illinois.edu_lltrace.txt");
		File bestgraphFile = new File("./data/hdtm/illinois.edu_bestgraph.txt");

		// The random seed for the Gibbs sampler. Default is 0, which will use
		// the clock.
		Integer randomSeed = 1;

		// The number of iterations of Gibbs sampling
		Integer numIterations = 1000;

		// If true, print a character to standard output after every sampling
		// iteration.
		Boolean showProgress = true;

		// The number of iterations between printing a brief summary of the
		// topics so far
		Integer showTopicsInterval = 50;

		// The number of most probable words to print for each topic after model
		// estimation
		Integer topWords = 5;

		// Alpha parameter: smoothing over level distributions.
		Double alpha = 10.0;

		// Gamma parameter: CRP smoothing parameter; number of imaginary
		// customers at next, as yet unused table
		Double gamma = .95;

		// Eta parameter: smoothing over topic-word distributions
		Double eta = 0.1;

		HierachicalDocTopicModel hlda = new HierachicalDocTopicModel(alpha,
				gamma, eta);

		try {
			hlda.setLLTrace(new PrintWriter(lltraceFile));
			hlda.setBestGraph(new PrintWriter(bestgraphFile));
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Display preferences

		hlda.setTopicDisplay(showTopicsInterval, topWords);
		hlda.setProgressDisplay(showProgress);

		// Initialize random number generator

		Randoms random = null;
		if (randomSeed == 1) {
			random = new Randoms();
		} else {
			random = new Randoms(randomSeed);
		}

		// Initialize and start the sampler

		hlda.initialize(
				"C:\\Users\\weninger\\Downloads\\enwiki-20130503-pages-articles1.xml-p000000010p000010000.bz2",
				random);

		hlda.estimate(numIterations, 100, 10);

		// Output results

		if (outputFile != null) {
			try {
				hlda.printState(new PrintWriter(outputFile));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			hlda.printResults(new PrintWriter(resultsFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}
}
