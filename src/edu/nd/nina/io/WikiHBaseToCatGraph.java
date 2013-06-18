package edu.nd.nina.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NavigableMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

import edu.nd.nina.DirectedGraph;
import edu.nd.nina.graph.DefaultDirectedGraph;
import edu.nd.nina.graph.DefaultEdge;
import edu.nd.nina.types.Instance;

public class WikiHBaseToCatGraph {
	public static Instance loadCategoryFeatureGraphFromWikiHbase(
			String category,
			DirectedGraph<String, DefaultEdge> graph, int depth) {
		
		Configuration config = HBaseConfiguration.create();
		config.set("hbase.zookeeper.quorum", "dmserv3.cs.illinois.edu");
		HashMap<String, Integer> tempMap = new HashMap<String, Integer>();
		Instance root = null;

		HTable testTable = null;
		try {
			testTable = new HTable(config, "wikipedia");
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		DirectedGraph<String, DefaultEdge> dag = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
		
		recur(category, testTable, dag, "", depth);
		
		List<Get> gets = new ArrayList<Get>();
		
		int i=0;
		for(String v : dag.vertexSet()){
			if(v.startsWith("Category:")) continue;
			
			tempMap.put(v, i++);
			Get g = new Get(Bytes.toBytes(v));
			
			g.addFamily(Bytes.toBytes("ol"));
			g.addColumn(Bytes.toBytes("p"), Bytes.toBytes("text"));
			gets.add(g);
			
			graph.addVertex(v);
		}
		
		PrintWriter pw;
		try {
			pw = new PrintWriter("./data/hdtm/" + category.replaceAll(":", "_") + ".dot");
			
			for(DefaultEdge e : dag.edgeSet()){	
				pw.println( dag.getEdgeSource(e).replaceAll(" ", "_") + "->" + dag.getEdgeTarget(e).replaceAll(" ", "_")); 
			}
			pw.close();
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		try {
			pw = new PrintWriter("./data/hdtm/" + category.replaceAll(":", "_") + ".txt");
			
			StringBuilder sb = new StringBuilder();
			for (Result r : testTable.get(gets)) {

				String str = Bytes.toString(r.getRow());

				System.out.println(str.replaceAll(" ", "_"));
				String text = Bytes.toString(r.getColumn(Bytes.toBytes("p"), Bytes.toBytes("text")).get(0).getValue());
				text = text.replaceAll("\\\n", "");

				text = text.replaceAll("\\{(.*?)\\}", "");
				pw.println(tempMap.get(str) + " " + str.replaceAll(" ", "_") + " " + text.replaceAll("[^a-zA-Z0-9 _.]+", "").trim());
				
				NavigableMap<byte[], byte[]> ols = r.getFamilyMap(Bytes.toBytes("ol"));
				for(byte[] ol : ols.values()){
					if(tempMap.containsKey(Bytes.toString(ol))){
						sb.append(tempMap.get(str) + " -> " + tempMap.get(Bytes.toString(ol)) + System.getProperty("line.separator"));
					}
				}
			}
			pw.println();
			pw.println(sb);
			pw.close();
			testTable.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return root;
		
	}
	
	private static void recur(String cat, HTable testTable, DirectedGraph<String, DefaultEdge> dag, String tab, int maxdepth){
		
		if(tab.length() > maxdepth) return;
		
		Scan scan = new Scan();
		scan.addColumn(Bytes.toBytes("c"), Bytes.toBytes(cat.replaceAll("Category:", "")));				
		
		
		dag.addVertex(cat);
		
		ResultScanner rs = null;
		try {
			rs = testTable.getScanner(scan);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			int i=0;
			for (Result r = rs.next(); r != null; r = rs.next()) {

				String str = Bytes.toString(r.getRow());

				dag.addVertex(str);
				dag.addEdge(cat, str);

				System.out.println(tab + str);
				if (str.startsWith("Category:")) {
					recur(str, testTable, dag, tab + "\t", maxdepth);
				}
				
				i++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public static void main(String[] args) {

		String[] cats = {
				"Sports", "Technology" };

		for (String cat : cats) {
			DirectedGraph<String, DefaultEdge> dag = new DefaultDirectedGraph<String, DefaultEdge>(
					DefaultEdge.class);
			loadCategoryFeatureGraphFromWikiHbase("Category:" + cat, dag, 2);
		}
	}
}
