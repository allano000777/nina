package edu.nd.nina.math;

import java.util.Vector;

public class Numerical {

	public static void SolveSymetricSystem(Vector<Vector<Float>>[] A,
			final Vector<Float> b, Vector<Float>[] x) {
		assert(A[0].size() == A[0].get(0).size());
		  Vector<Float>[] p = new Vector[1];
		  CholeskyDecomposition(A, p);
		  CholeskySolve(A[0], p[0], b, x);
	}

	private static void CholeskySolve(final Vector<Vector<Float>> A,
			final Vector<Float> p, final Vector<Float> b, Vector<Float>[] x) {
		assert (A.size() == A.get(0).size());
		int n = A.size();
		x[0] = new Vector<Float>(n);
		for (int i = 0; i < n; i++) {
			x[0].add(0f);
		}

		int i, k;
		double sum;

		// Solve L * y = b, storing y in x
		for (i = 1; i <= n; i++) {
			for (sum = b.get(i - 1), k = i - 1; k >= 1; k--) {
				sum -= A.get(i - 1).get(k - 1) * x[0].get(k - 1);
			}
			x[0].set(i - 1, (float) (sum / p.get(i - 1)));
		}

		// Solve L^T * x = y
		for (i = n; i >= 1; i--) {
			for (sum = x[0].get(i - 1), k = i + 1; k <= n; k++) {
				sum -= A.get(k - 1).get(i - 1) * x[0].get(k - 1);
			}
			x[0].set(i - 1, (float) (sum / p.get(i - 1)));
		}
	}

	private static void CholeskyDecomposition(Vector<Vector<Float>>[] A,
			Vector<Float>[] p) {
		assert (A[0].size() == A[0].get(0).size());
		int n = A[0].size();
		p[0] = new Vector<Float>(n);
		for (int i = 0; i < n; i++){
			p[0].add(0f);
		}

		int k;
		double sum;
		for (int i = 1; i <= n; i++) {
			for (int j = i; j <= n; j++) {
				for (sum = A[0].get(i - 1).get(j - 1), k = i - 1; k >= 1; k--)
					sum -= A[0].get(i - 1).get(k - 1) * A[0].get(j - 1).get(k - 1);
				if (i == j) {
					if (sum <= 0.0)
						System.err.println("choldc failed");
					p[0].set(i - 1, (float) Math.sqrt(sum));
				} else {
					A[0].get(j - 1).set(i - 1, (float) (sum / p[0].get(i - 1)));
				}
			}
		}
	}

}
