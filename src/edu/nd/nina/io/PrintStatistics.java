package edu.nd.nina.io;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Vector;

import edu.nd.nina.Graph;
import edu.nd.nina.alg.CalculateStatistics;
import edu.nd.nina.alg.StatVal;
import edu.nd.nina.structs.Pair;

public class PrintStatistics {

	public static <V extends Comparable<V>, E> void PrintGraphStatTable(
			final Graph<V, E> G, String filename) {
		PrintGraphStatTable(G, filename, "");
	}

	public static <V extends Comparable<V>, E> void PrintGraphStatTable(
			final Graph<V, E> G, String fileName, String desc) {

		Hashtable<StatVal, Float> valStatH = new Hashtable<StatVal, Float>();
		Hashtable<StatVal, Vector<Pair<Float, Float>>> distrStatH = new Hashtable<StatVal, Vector<Pair<Float, Float>>>();

		CalculateStatistics.calcBasicStat(G, false, valStatH);
		// diameter
		CalculateStatistics.calcDiameter(G, 100, valStatH, distrStatH);
		// degrees
		CalculateStatistics.calcDegreeDistribution(G, distrStatH);
		// components
		CalculateStatistics.calcConnectedComponents(G, distrStatH);
		// clustering coefficient
		CalculateStatistics.calcClusteringCoefficient(G, valStatH);

		CalculateStatistics.calcTriangleParticipation(G, distrStatH);

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
		for (Entry<StatVal, Float> e : valStatH.entrySet()) {
			pw.printf("  <tr><td>%s</td> <td>%.4f</td></tr>\n", e.getKey()
					.toString(), e.getValue());
		}
		for (Entry<StatVal, Vector<Pair<Float, Float>>> e : distrStatH
				.entrySet()) {
			pw.printf("  <tr><td>%s</td> <td>", e.getKey());
			for (Pair<Float, Float> p : e.getValue()) {
				pw.printf("%.4f - %.4f<br/>", p.p1, p.p2);
			}
			pw.printf("</td></tr>\n");

		}

		pw.printf("</table>\n");
		pw.close();
	}
}
