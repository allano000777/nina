package edu.nd.nina.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

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
	public static Instance loadFeatureGraphFromFile(File featureGraphFile, DirectedFeatureGraph<Instance, DefaultEdge> graph) {
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
					if (root == null) {
						root = ins;
					}
					graph.addVertex(ins);
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
}
