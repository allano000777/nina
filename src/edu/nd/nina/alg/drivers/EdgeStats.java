package edu.nd.nina.alg.drivers;

import java.util.Hashtable;
import java.util.Vector;

import edu.nd.nina.Graph;
import edu.nd.nina.alg.CalculateStatistics;
import edu.nd.nina.alg.StatVal;
import edu.nd.nina.graph.DefaultEdge;
import edu.nd.nina.io.Dot;
import edu.nd.nina.structs.Pair;

public class EdgeStats {
	public static void main(String[] args){
		Graph<String, DefaultEdge> e = Dot.load("C:\\Users\\weninger\\Downloads\\as-caida.tar\\as-caida20040105.txt", "");
		Hashtable<StatVal, Float> statValH = new Hashtable<StatVal, Float>();
		Hashtable<StatVal, Vector<Pair<Float, Float>>> distrStatH = new Hashtable<StatVal, Vector<Pair<Float, Float>>>();
		
		CalculateStatistics.calcDiameter(e, 10, statValH, distrStatH);
		
		System.out.println(statValH);
		System.out.println(distrStatH);
	}
}
