package edu.nd.nina.io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import edu.nd.nina.DirectedGraph;
import edu.nd.nina.Graph;
import edu.nd.nina.graph.DefaultEdge;
import edu.nd.nina.graph.SimpleGraph;

public class Dot {
	public static void save(Graph<String, String> graph,
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

		if (graph instanceof DirectedGraph) {
			pw.printf("digraph %s {\n", desc);
			for (String e : graph.edgeSet()) {
				pw.printf("%s->%s\n", graph.getEdgeSource(e),
						graph.getEdgeTarget(e));
			}
			pw.printf("}\n");
		}else{
			pw.printf("graph %s {\n", desc);
			for (String e : graph.edgeSet()) {
				pw.printf("%s--%s\n", graph.getEdgeSource(e),
						graph.getEdgeTarget(e));
			}
			pw.printf("}\n");
		}
		pw.close();
	}
	
	
	public static Graph<String, DefaultEdge> load(
			String fileName, String desc) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fileName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String line = "";
		boolean directed = false;
		
		SimpleGraph<String,DefaultEdge> graph = new SimpleGraph<String,DefaultEdge>(DefaultEdge.class);
		
		try {
			while((line = br.readLine()) != null){
				if(line.startsWith("#")){
					continue;
				}
				if(line.startsWith("graph")){
					directed = false;
					continue;
				}
				if(line.startsWith("digraph")){
					directed = true;
					continue;
				}
				
				String[] lineDiv;
				if(directed){
					lineDiv = line.split("->");
				}else{
					lineDiv = line.split("--");
				}
				
				if(lineDiv.length != 2){
					continue;
				}
				
				if(lineDiv[1].contains("[")){
					lineDiv[1] = lineDiv[1].substring(0, lineDiv[1].indexOf("["));
				}
				
				
				String src =  lineDiv[0];
				String dest =  lineDiv[1];
				
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
