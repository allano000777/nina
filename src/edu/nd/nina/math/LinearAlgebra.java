package edu.nd.nina.math;

import java.util.Vector;

public class LinearAlgebra {

	public static float sumVec(Vector<Float> x) {
		final int len = x.size();
		float res = 0.0f;
		for (int i = 0; i < len; i++) {
			res += x.get(i);
		}
		return res;
	}

	public static double dotProduct(Vector<Float> x, Vector<Float> y) {
		assert (x.size() == y.size());
		double result = 0.0;
		int Len = x.size();
		for (int i = 0; i < Len; i++)
			result += x.get(i) * y.get(i);
		return result;
	}

	public static double norm(Vector<Float> x) {
		return Math.sqrt(norm2(x));
	}
	
	private static double norm2(Vector<Float> x) {
		return dotProduct(x, x);
	}


}
