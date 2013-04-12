package edu.nd.nina.util;

import java.util.Vector;

import edu.nd.nina.structs.Pair;

public class GraphUtil {
	
	public static Vector<Pair<Integer, Float>> GetCdf(final Vector<Pair<Integer, Float>> PdfV) {
		Vector<Pair<Integer, Float>> CdfV = new Vector<Pair<Integer, Float>>();
		CdfV = PdfV;
		for (int i = 1; i < CdfV.size(); i++) {
			CdfV.get(i).p2 = CdfV.get(i - 1).p2 + CdfV.get(i).p2;
		}
		return CdfV;
	}
}
