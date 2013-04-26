package edu.nd.nina.snap.forestfire;

import java.io.File;

import edu.nd.nina.DirectedGraph;
import edu.nd.nina.experimental.isomorphism.IntegerVertexFactory;
import edu.nd.nina.generate.ForestFireGraphGenerator;
import edu.nd.nina.graph.DefaultDirectedGraph;
import edu.nd.nina.graph.DefaultEdge;

public class ForestFire {
	public static void main(String[] args) {
		Long ExeTm = System.currentTimeMillis();

		try {
			int NNodes = 10000; // Number of nodes (size of the generated graph)
			float FwdProb = 0.35f; // Forward burning probability
			float BckProb = 0.32f; // Backward burning probability
			int StartNodes = 1; // Start graph with S isolated nodes
			float Take2AmbProb = 0.0f; // Probability of a new node choosing 2
										// ambassadors
			float OrphanProb = 0.0f; // Probability of a new node being an
										// orphan (node with zero out-degree)
			
			String OutFNm = ("." + System.getProperty("file.separator")
					+ "data" + System.getProperty("file.separator") + "forestfire"
					+ System.getProperty("file.separator")
					+ String.format("ffgraph-%s", NNodes) + ".txt");

			ForestFireGraphGenerator<Integer, DefaultEdge> ff = new ForestFireGraphGenerator<Integer, DefaultEdge>(
					false, StartNodes, FwdProb, BckProb, 1.0f, Take2AmbProb,
					OrphanProb);
			ff.setNumNodes(NNodes);
			ff.setFloodStop(false);
			// generate forest fire graph
			DirectedGraph<Integer, DefaultEdge> g = new DefaultDirectedGraph<Integer, DefaultEdge>(
					DefaultEdge.class);

			ff.generateGraph(g, new IntegerVertexFactory(-1), null);
			File f = new File(OutFNm);
			f.getParentFile().mkdirs();
			//g.save(f, ff.getParamString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
