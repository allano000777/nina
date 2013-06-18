package edu.nd.nina.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

import edu.nd.nina.DirectedGraph;
import edu.nd.nina.graph.DefaultDirectedGraph;
import edu.nd.nina.graph.DefaultEdge;
import edu.nd.nina.graph.DirectedFeatureGraph;
import edu.nd.nina.types.Instance;

public class FeatureGraph {
	/**
	 * Loads the directedFeatureGraph from a file. The file should contain
	 * vertex definitions and directed edges in the followuing form:
	 * 
	 * Vertex Definitions - 0 a b c d
	 * 
	 * where 0 is an Integer id for the vertex followed by a tokenizable
	 * sequence of "features" (probably words)
	 * 
	 * Edge Definitions - 0 -> 1
	 * 
	 * where 0 and 1 are previously defined vertices and '->' denotes the
	 * directed edge. Reverse edges ('<-') are not allowed
	 * 
	 * The first vertex defined in the featureGraphFile is declared to be the
	 * root node
	 * 
	 * @param featureGraphFile
	 *            File containing textual representation of a
	 *            directedFeatureGraph
	 * @return The root vertex of the newly loaded directedFeatureGraph
	 */
	public static Instance loadFeatureGraphFromFile(File featureGraphFile, DirectedFeatureGraph<Instance, DefaultEdge> graph, String rootName) {
		System.out.println("Reading " + featureGraphFile);
		Instance root = null;

		HashMap<Integer, Instance> tempMap = new HashMap<Integer, Instance>();

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(featureGraphFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String line = "";
		try {
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				if (line.startsWith("#")) {
					continue;
				}

				if (!line.contains("->")) {
					Instance ins = new Instance(line, false, null, null);
					
					tempMap.put(
							Integer.parseInt(line.substring(0,
									line.indexOf(" "))), ins);
					
					graph.addVertex(ins);
					
					int f = (line).indexOf(" "); 
					String ts = line.substring(f, line.indexOf(" ", f+1)).trim();
			
					
					if (root == null && ts.equals(rootName)) {
						root = ins;
					}
				} else {
					String[] edges = line.split("->");
					graph.addEdge(
							tempMap.get(Integer.parseInt(edges[0].trim())),
							tempMap.get(Integer.parseInt(edges[1].trim())));
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return root;
	}
	
	
	public static Instance loadFeatureGraphFromNutchHBase(
			String domain,
			DirectedFeatureGraph<Instance, DefaultEdge> graph) {
		Configuration config = HBaseConfiguration.create();
		config.set("hbase.zookeeper.quorum", "dmserv3.cs.illinois.edu");
		HashMap<String, Instance> tempMap = new HashMap<String, Instance>();
		Instance root = null;

		HTable testTable = null;
		try {
			testTable = new HTable(config, "webpage");
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		String domainEnd = domain.substring(0, domain.length()-1) + (char)(((int)domain.charAt(domain.length()-1)) + 1);
		
		Scan scan = new Scan(Bytes.toBytes(domain + ":http/"),
				Bytes.toBytes(domainEnd));

		// scan.addFamily(family);
		scan.addColumn(Bytes.toBytes("f"), Bytes.toBytes("bas"));
		scan.addColumn(Bytes.toBytes("p"), Bytes.toBytes("c"));
		

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

				List<KeyValue> cList = r.getColumn(Bytes.toBytes("p"),
						Bytes.toBytes("c"));
				String c = "";
				if (cList.size() >= 1) {
					c = Bytes.toString(cList.get(0).getValue());
				}

				List<KeyValue> basList = r.getColumn(Bytes.toBytes("f"),
						Bytes.toBytes("bas"));
				
				byte[] bas = {};
				if (basList.size() >= 1) {
					bas = basList.get(0).getValue();
				}
				
				String bStr = Bytes.toString(bas);
				if(bStr.contains("/  ")) continue;
			

				Instance ins = new Instance(c, false ,bStr, i);
				tempMap.put(bStr, ins);
				
				System.out.println(bStr);
				
				if (root == null && Bytes.toString(r.getRow()).equals(domain + ":http/") ) {
					root = ins;
				}
				graph.addVertex(ins);
				i++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		scan = new Scan(Bytes.toBytes(domain + ":http/"),
				Bytes.toBytes(domainEnd));

		scan.addFamily(Bytes.toBytes("ol"));
		scan.addColumn(Bytes.toBytes("f"), Bytes.toBytes("bas"));

		try {
			rs = testTable.getScanner(scan);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			for (Result r = rs.next(); r != null; r = rs.next()) {

				List<KeyValue> basList = r.getColumn(Bytes.toBytes("f"),
						Bytes.toBytes("bas"));
				String bas = "";
				if (basList.size() >= 1) {
					bas = Bytes.toString(basList.get(0).getValue());
				}

				Map<byte[], byte[]> valueObj = r.getFamilyMap(Bytes
						.toBytes("ol"));
				for (Map.Entry<byte[], byte[]> x : valueObj.entrySet()) {
					String key = Bytes.toString(x.getKey());

					if (tempMap.containsKey(key) && tempMap.containsKey(bas)) {

						graph.addEdge(tempMap.get(bas), tempMap.get(key));
					}

				}

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			testTable.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return root;
	}
	
	
	public static Instance loadFeatureGraphFromWikiHbase(
			String domain,
			DirectedFeatureGraph<Instance, DefaultEdge> graph) {
		Configuration config = HBaseConfiguration.create();
		config.set("hbase.zookeeper.quorum", "dmserv3.cs.illinois.edu");
		HashMap<String, Instance> tempMap = new HashMap<String, Instance>();
		Instance root = null;

		HTable testTable = null;
		try {
			testTable = new HTable(config, "wikipedia");
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		
		Scan scan = new Scan(Bytes.toBytes("A"), Bytes.toBytes("B"));

		// scan.addFamily(family);
		scan.addColumn(Bytes.toBytes("p"), Bytes.toBytes("t"));
		scan.addColumn(Bytes.toBytes("p"), Bytes.toBytes("text"));
		

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

				List<KeyValue> cList = r.getColumn(Bytes.toBytes("p"),
						Bytes.toBytes("text"));
				String c = "";
				if (cList.size() >= 1) {
					c = Bytes.toString(cList.get(0).getValue());
				}

				List<KeyValue> basList = r.getColumn(Bytes.toBytes("p"),
						Bytes.toBytes("t"));
				
				byte[] bas = {};
				if (basList.size() >= 1) {
					bas = basList.get(0).getValue();
				}
				
				String bStr = Bytes.toString(bas);
			

				Instance ins = new Instance(c, false ,bStr, i);
				tempMap.put(bStr, ins);
				
				System.out.println(bStr);
				
				if(root == null && bStr.equals("A")){
					root = ins;
				}
				
				graph.addVertex(ins);
				i++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		scan = new Scan(Bytes.toBytes("A"), Bytes.toBytes("B"));

		scan.addFamily(Bytes.toBytes("ol"));
		scan.addColumn(Bytes.toBytes("p"), Bytes.toBytes("t"));

		try {
			rs = testTable.getScanner(scan);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			for (Result r = rs.next(); r != null; r = rs.next()) {

				List<KeyValue> basList = r.getColumn(Bytes.toBytes("p"),
						Bytes.toBytes("t"));
				String bas = "";
				if (basList.size() >= 1) {
					bas = Bytes.toString(basList.get(0).getValue());
				}

				Map<byte[], byte[]> olList = r.getFamilyMap(Bytes.toBytes("ol"));
				for (byte[] x : olList.values()) {
					String l = Bytes.toString(x);

					if (tempMap.containsKey(l) && tempMap.containsKey(bas)) {

						graph.addEdge(tempMap.get(bas), tempMap.get(l));
					}

				}

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			testTable.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return root;
	}


	
	
}
