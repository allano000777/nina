package edu.nd.nina.io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import edu.nd.nina.DirectedGraph;
import edu.nd.nina.Graph;
import edu.nd.nina.graph.ClassBasedEdgeFactory;
import edu.nd.nina.graph.SimpleGraph;

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
	
	
	public static <V extends Comparable<V>, E> Graph<V, E> load(
			String fileName, String desc) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fileName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String line = "";
		
		SimpleGraph<V,E> graph = new SimpleGraph<V,E>(new ClassBasedEdgeFactory<V,E>());
		
		try {
			while((line = br.readLine()) != null){
				String[] lineDiv = line.split("\t");
				
				V src = (V) lineDiv[0];
				V dest = (V) lineDiv[1];
				
				graph.addVertex(src);
				graph.addVertex(dest);
				graph.addEdge(src, dest);
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return graph;
	}
}
