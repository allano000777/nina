package edu.nd.nina.io;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import edu.nd.nina.DirectedGraph;
import edu.nd.nina.Graph;

public class EdgeList {
	public static <V extends Comparable<V>, E> void save(Graph<V, E> graph,
			String fileName, String desc) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(fileName);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		}

		if (graph instanceof DirectedGraph) {
			pw.printf("# Directed graph: %s \n", fileName);
		} else {
			pw.printf(
					"# Undirected graph (each unordered pair of nodes is saved once): %s\n",
					fileName);
		}
		if (!desc.isEmpty()) {
			pw.printf("# %s\n", desc);
		}
		pw.printf("# Nodes: %d Edges: %d\n", graph.vertexSet().size(), graph
				.edgeSet().size());
		if (graph instanceof DirectedGraph) {
			pw.printf("# FromNodeId\tToNodeId\n");
		} else {
			pw.printf("# NodeId\tNodeId\n");
		}

		for (E e : graph.edgeSet()) {
			pw.printf("%s\t%s\n", graph.getEdgeSource(e),
					graph.getEdgeTarget(e));
		}
		pw.close();
	}
}
