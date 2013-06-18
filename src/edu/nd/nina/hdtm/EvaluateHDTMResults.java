package edu.nd.nina.hdtm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import edu.nd.nina.DirectedGraph;
import edu.nd.nina.Graphs;
import edu.nd.nina.UndirectedGraph;
import edu.nd.nina.graph.AsUndirectedGraph;
import edu.nd.nina.graph.DefaultDirectedGraph;
import edu.nd.nina.graph.DefaultDirectedWeightedGraph;
import edu.nd.nina.graph.DefaultEdge;
import edu.nd.nina.graph.DefaultWeightedEdge;
import edu.nd.nina.graph.DirectedFeatureGraph;
import edu.nd.nina.graph.load.LoadFromFeatureGraph;
import edu.nd.nina.io.FeatureGraph;
import edu.nd.nina.types.Instance;

/**
 * @author Tim Weninger 4/9/2013
 * 
 */
public class EvaluateHDTMResults {


	public static void main(String[] args) {

		// The filename in which to write the Gibbs sampling state after at the
		// end of the iterations
		File catFile = new File("./data/hdtm/Category_Agriculture.dot");
		File resultsFile = new File("./data/hdtm/Category_Agriculture_results_0.99.txt");
		File wikiFile = new File("./data/hdtm/Category_Agriculture.txt");
		
		DirectedGraph<String, DefaultEdge> catGraph = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
		try {
			BufferedReader br = new BufferedReader(new FileReader(catFile));
			String line = "";
			while((line = br.readLine()) != null){
				String[] edgeS = line.split("->");
				String v1 = edgeS[0];
				String v2 = edgeS[1];
				catGraph.addVertex(v1);
				catGraph.addVertex(v2);
				catGraph.addEdge(v1, v2);
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> resultsGraph = new DefaultDirectedWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		try {
			BufferedReader br = new BufferedReader(new FileReader(resultsFile));
			String line = "";
			while((line = br.readLine()) != null){
				if(line.isEmpty()) continue;
				String[] edgeS = line.split(" -> ");
				String v1 = edgeS[0];
				String[] vS = edgeS[1].split("\\[weight=\\\"");
				String v2 = vS[0];
				double eW = (double)Integer.parseInt(vS[1].replace("\"]", ""))/(double)100;
				resultsGraph.addVertex(v1);
				resultsGraph.addVertex(v2);
				DefaultWeightedEdge dwe = resultsGraph.addEdge(v1, v2);
				resultsGraph.setEdgeWeight(dwe, (double)eW);
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		DirectedFeatureGraph<Instance, DefaultEdge> wikiGraph = new DirectedFeatureGraph<Instance, DefaultEdge>(
				DefaultEdge.class, LoadFromFeatureGraph.createPipe());
		Instance root = FeatureGraph.loadFeatureGraphFromFile(wikiFile,
				wikiGraph, "Agriculture");
		wikiGraph.resolveInstances();
		
		//for each node in the results graph
		for(String v : resultsGraph.vertexSet()){
			//get the best parent
			String p = getBestParent(resultsGraph,v);
			if(p.isEmpty()) continue; //root
			String s = findPath(new AsUndirectedGraph<String, DefaultEdge>(catGraph), v, p).trim();
			int len = s.length() - s.replaceAll(" ", "").length();

			System.out.print(len + "\t");
		}
		
		System.out.println();
		
		/*
		
		// for each node in the wiki graph
		for (Instance v : wikiGraph.vertexSet()) {
			// does it exist in resultGraph? //because some nodes arent part of
			// root-connected component
			if (!resultsGraph.containsVertex((String) v.getName())) continue;
			int best = 100;			
			for (Instance p : Graphs.predecessorListOf(wikiGraph, v)) {				
				if (!resultsGraph.containsVertex((String) p.getName())) continue;
				String s = findPath(
						new AsUndirectedGraph<String, DefaultEdge>(catGraph),
						(String)v.getName(), (String)p.getName()).trim();
				int len = s.length() - s.replaceAll(" ", "").length();
				if(len == 3){
					best = len; //more complicated than this
				}else if(len < best){
					best = len;
				}
			}
			System.out.print(best + "\t");
		}
		
		System.out.println();
		
		
		//random
		Random r = new Random();
		// for each node in the wiki graph
		for (Instance v : wikiGraph.vertexSet()) {
			// does it exist in resultGraph? //because some nodes arent part of
			// root-connected component
			if (!resultsGraph.containsVertex((String) v.getName())) continue;

			List<Instance> pL = Graphs.predecessorListOf(wikiGraph, v);	
			Instance p = null;
			while(p == null || !resultsGraph.containsVertex((String) p.getName()) ){
				int choice = r.nextInt(pL.size());				
				 p = pL.get(choice);
			}
			
			String s = findPath(
					new AsUndirectedGraph<String, DefaultEdge>(catGraph),
					(String) v.getName(), (String) p.getName()).trim();
			int len = s.length() - s.replaceAll(" ", "").length();

			System.out.print(len + "\t");
		}
		
		System.out.println();
	*/
	}

	public static String findPath(UndirectedGraph<String, DefaultEdge> catGraph, String v, String w) {
	    Queue<String> q = new LinkedList<String>();
	    Set<String> visited = new HashSet<String>();
	    Map<String, String> pathTo = new HashMap<String, String>();

	    q.add(v);
	    pathTo.put(v,v+" ");
	    while(q.peek() != null) {
	        if(runBFS(catGraph, q.poll(),w,visited,q,pathTo))
	        break;
	    }
	    return pathTo.get(w);
	}

	private static boolean runBFS(UndirectedGraph<String, DefaultEdge> catGraph, String v, String w, Set<String> visited, Queue<String> q, Map<String, String>  pathTo) {
	    if(visited.contains(v)) {
	    }
	    else if(v.equals(w)){
	        return true; 
	    }
	    else {
	        visited.add(v);
	        List<String> vi = Graphs.neighborListOf(catGraph, v);
	        for(String nextVertex : vi){
	        	if(nextVertex.startsWith("Category:") || nextVertex.equals(w)){
	        		pathTo.put(nextVertex, pathTo.get(v) + nextVertex + " ");
	            	q.add(nextVertex);
	        	}
	        }
	    }
	    return false;
	}
	

	private static List<String> getCategories(
			DirectedGraph<String, DefaultEdge> catGraph, String v) {
		List<String> succs = Graphs.successorListOf(catGraph, v);
		List<String> cats = new ArrayList<String>();
		for(String suc : succs){
			if(suc.startsWith("Category:")){
				cats.add(suc);
			}
		}
		return cats;
	}

	private static String getBestParent(
			DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> resultsGraph,
			String v) {
		String best = "";
		double bestValue = -1d;
		for(DefaultWeightedEdge e : resultsGraph.incomingEdgesOf(v)){
			if(resultsGraph.getEdgeWeight(e) > bestValue){
				bestValue = resultsGraph.getEdgeWeight(e);
				best = resultsGraph.getEdgeSource(e);
			}
		}
		return best;
		
	}

}
