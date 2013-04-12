package edu.nd.nina.io;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import edu.nd.nina.DirectedGraph;
import edu.nd.nina.Graph;
import edu.nd.nina.UndirectedGraph;
import edu.nd.nina.alg.BreadthDepthFirstSearch;
import edu.nd.nina.alg.CalculateStatistics;
import edu.nd.nina.alg.ConnectivityInspector;
import edu.nd.nina.alg.StatVal;
import edu.nd.nina.alg.StrongConnectivityInspector;
import edu.nd.nina.alg.Triad;
import edu.nd.nina.structs.Pair;
import edu.nd.nina.structs.Triple;

public class PrintStatistics {

	public static <V extends Comparable<V>, E> void PrintGraphStatTable(
			final Graph<V, E> G, String filename) {
		PrintGraphStatTable(G, filename, "");
	}

	public static <V extends Comparable<V>, E> void PrintGraphStatTable(
			final Graph<V, E> G, String fileName, String desc) {
		long ClosedTriads, OpenTriads;
		int[] FullDiam = { -1 };
		double[] EffDiam = { -1 };
		PrintInfo(G, fileName);
		Long ExeTm = System.currentTimeMillis();

		System.out.printf("C");
		CalculateStatistics<V, E> cs = new CalculateStatistics<V, E>();
		final double CCF = cs.calcClustCf(G);
		ClosedTriads = (long) (float) cs.getVal(StatVal.gsvClosedTriads);
		OpenTriads = (long) (float) cs.getVal(StatVal.gsvOpenTriads);
		System.out.printf("[%s]D", System.currentTimeMillis() - ExeTm);

		new BreadthDepthFirstSearch<V, E>(G).GetBfsEffDiam(1000, false,
				EffDiam, FullDiam);
		System.out.printf("[%s]CC", System.currentTimeMillis() - ExeTm);

		ConnectivityInspector<V, E> ci = new ConnectivityInspector<V, E>(G);
		Graph<V, E> WCC = ci.getMaxWcc();
		Graph<V, E> SCC = WCC;
		if (G instanceof DirectedGraph) {
			StrongConnectivityInspector<V, E> sci = new StrongConnectivityInspector<V, E>(
					(DirectedGraph<V, E>) G);
			SCC = sci.getMaxScc();
		}
		System.out.printf("[%s]\n", System.currentTimeMillis() - ExeTm);

		PrintWriter pw = new PrintWriter(System.out);
		if (!fileName.isEmpty()) {
			try {
				pw = new PrintWriter(String.format("%s.html", fileName));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		pw.printf("\n");
		pw.printf("<table id=\"datatab\" summary=\"Dataset statistics\">\n");
		pw.printf("  <tr> <th colspan=\"2\">Dataset statistics</th> </tr>\n");
		pw.printf("  <tr><td>Nodes</td> <td>%d</td></tr>\n", G.vertexSet()
				.size());
		pw.printf("  <tr><td>Edges</td> <td>%d</td></tr>\n", G.edgeSet().size());
		pw.printf(
				"  <tr><td>Nodes in largest WCC</td> <td>%d (%.3f)</td></tr>\n",
				WCC.vertexSet().size(), WCC.vertexSet().size()
						/ (double) G.vertexSet().size());
		pw.printf(
				"  <tr><td>Edges in largest WCC</td> <td>%d (%.3f)</td></tr>\n",
				WCC.edgeSet().size(), WCC.edgeSet().size()
						/ (double) G.edgeSet().size());
		pw.printf(
				"  <tr><td>Nodes in largest SCC</td> <td>%d (%.3f)</td></tr>\n",
				SCC.vertexSet().size(), SCC.vertexSet().size()
						/ (double) G.vertexSet().size());
		pw.printf(
				"  <tr><td>Edges in largest SCC</td> <td>%d (%.3f)</td></tr>\n",
				SCC.edgeSet().size(), SCC.edgeSet().size()
						/ (double) G.edgeSet().size());
		pw.printf(
				"  <tr><td>Average clustering coefficient</td> <td>%.4f</td></tr>\n",
				CCF);
		pw.printf("  <tr><td>Number of triangles</td> <td>%g</td></tr>\n",
				(double)ClosedTriads);
		pw.printf(
				"  <tr><td>Fraction of closed triangles</td> <td>%.4g</td></tr>\n",
				ClosedTriads / (double) ClosedTriads + OpenTriads);
		pw.printf(
				"  <tr><td>Diameter (longest shortest path)</td> <td>%d</td></tr>\n",
				FullDiam[0]);
		pw.printf(
				"  <tr><td>90-percentile effective diameter</td> <td>%.2g</td></tr>\n",
				EffDiam[0]);
		pw.printf("</table>\n");
		pw.printf("<br>\n");
		if (!fileName.isEmpty()) {
			pw.printf("\n<table id=\"datatab\" summary=\"Table of datasets\">\n");
			pw.printf("<tr>\n");
			pw.printf("  <th>File</th>\n");
			pw.printf("  <th>Description</th>\n");
			pw.printf("</tr>\n");
			pw.printf("<tr>\n");
			pw.printf("  <td><a href=\"%s.txt.gz\">%s.txt.gz</a></td>\n",
					fileName, fileName);
			pw.printf("  <td>%s</td>\n", desc);
			pw.printf("</tr>\n");
			pw.printf("</table>\n");
			pw.close();

			EdgeList.save(G, fileName + ".txt", desc);
		}
	}

	public static <V extends Comparable<V>, E> void PrintInfo(
			final Graph<V, E> g, final String desc) {
		PrintInfo(g, desc, "", true);
	}

	public static <V extends Comparable<V>, E> void PrintInfo(Graph<V, E> g,
			String desc, String fileName, boolean fast) {

		int BiDirEdges = 0, ZeroNodes = 0, ZeroInNodes = 0, ZeroOutNodes = 0, SelfEdges = 0, NonZIODegNodes = 0;
		Set<Pair<V, V>> UniqDirE = new HashSet<Pair<V, V>>();
		Set<Pair<V, V>> UniqUnDirE = new HashSet<Pair<V, V>>();
		PrintStream pw = System.out;
		if (!fileName.isEmpty()) {
			try {
				pw = new PrintStream(fileName);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				return;
			}
		}
		if (!desc.isEmpty()) {
			pw.printf("%s:", desc);
		} else {
			pw.printf("Graph:");
		}

		pw.printf(" %s", g.getClass().getSimpleName());

		// calc stat
		for (V v : g.vertexSet()) {
			if (g.edgesOf(v).size() == 0)
				ZeroNodes++;
			if (g instanceof DirectedGraph) {
				if (((DirectedGraph<V, E>) g).inDegreeOf(v) == 0) {
					ZeroInNodes++;
				}
			} else {
				if (((UndirectedGraph<V, E>) g).degreeOf(v) == 0) {
					ZeroInNodes++;
				}
			}
			if (g instanceof DirectedGraph) {
				if (((DirectedGraph<V, E>) g).outDegreeOf(v) == 0) {
					ZeroOutNodes++;
				}
			} else {
				if (((UndirectedGraph<V, E>) g).degreeOf(v) == 0) {
					ZeroOutNodes++;
				}
			}

			if (g instanceof DirectedGraph) {
				if (((DirectedGraph<V, E>) g).outDegreeOf(v) != 0
						&& ((DirectedGraph<V, E>) g).inDegreeOf(v) != 0) {
					NonZIODegNodes++;
				}
			} else {
				if (((UndirectedGraph<V, E>) g).degreeOf(v) == 0) {
					NonZIODegNodes++;
				}
			}

			if (!fast || g.vertexSet().size() < 1000) {
				for (E e : g.edgesOf(v)) {
					final V DstNId = g.getEdgeTarget(e);

					if (g.containsEdge(DstNId, v))
						BiDirEdges++;
					if (v == DstNId)
						SelfEdges++;
					UniqDirE.add(new Pair<V, V>(v, DstNId));
					UniqUnDirE.add(new Pair<V, V>(v, DstNId));
				}
			}
		}
		long Closed = 0;
		long Open = 0;
		Vector<Triple<V, Integer, Integer>> e = null;
		if (!fast) {
			e = new Triad<V, E>().getTriads(g, -1);
			for (Triple<V, Integer, Integer> t : e) {
				Closed += t.v2;
				Open += t.v3;
			}
		}
		// print info
		pw.printf("\n");
		pw.printf("  Nodes:                    %d\n", g.vertexSet().size());
		pw.printf("  Edges:                    %d\n", g.edgeSet().size());
		pw.printf("  Zero Deg Nodes:           %d\n", ZeroNodes);
		pw.printf("  Zero InDeg Nodes:         %d\n", ZeroInNodes);
		pw.printf("  Zero OutDeg Nodes:        %d\n", ZeroOutNodes);
		pw.printf("  NonZero In-Out Deg Nodes: %d\n", NonZIODegNodes);
		if (!fast) {
			pw.printf("  Unique directed edges:    %d\n", UniqDirE.size());
			pw.printf("  Unique undirected edges:  %d\n", UniqUnDirE.size());
			pw.printf("  Self Edges:               %d\n", SelfEdges);
			pw.printf("  BiDir Edges:              %d\n", BiDirEdges);
			pw.printf("  Closed triangles          %s\n", Closed);
			pw.printf("  Open triangles            %s\n", Open);
			pw.printf("  Frac. of closed triads    %f\n", Closed
					/ (double) (Closed + Open));
		}
		if (!fileName.isEmpty()) {
			pw.close();
		}
	}

}
