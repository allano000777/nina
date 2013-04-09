package edu.nd.nina.math;

import java.util.Vector;

public class LogisticRegressionPrediction {
	private ReferenceCount CRef;
	private Vector<Float> Theta;

	public LogisticRegressionPrediction(final Vector<Float> _bb) {
		Theta = _bb;
	}

	// classifies vector, returns probability that AttrV is positive
	public static void GetCfy(final Vector<Vector<Float>> X,
			Vector<Float> OutV, final Vector<Float> NewTheta) {
		for (int i = 0; i < X.size(); i++) {
			OutV.add(0f);
		}

		for (int r = 0; r < X.size(); r++) {
			OutV.set(r, (float) GetCfy(X.get(r), NewTheta));
		}
	}

	public static double GetCfy(final Vector<Float> AttrV,
			final Vector<Float> NewTheta) {
		int len = AttrV.size();
		double res = 0;
		if (len < NewTheta.size()) {
			res = NewTheta.lastElement();
		} // if feature vector is shorter, add an intercept
		for (int i = 0; i < len; i++) {
			if (i < NewTheta.size()) {
				res += AttrV.get(i) * NewTheta.get(i);
			}
		}
		double mu = 1 / (1 + Math.exp(-res));
		return mu;
	}

	public double GetCfy(final Vector<Float> AttrV) {
		return GetCfy(AttrV, Theta);
	}

	public void SetTheta(final Vector<Float> _Theta) {
		 Theta = _Theta;
	}

	public Vector<Float> GetTheta() {
		return Theta;
	}

	
	public void PrintTheta() {
		for (int t = 0; t < Theta.size(); t++) {
			System.out.printf("Theta[%d] = %f\n", t, Theta.get(t).floatValue());
		}
	}

}
