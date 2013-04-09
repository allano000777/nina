package edu.nd.nina.math;

import java.util.Vector;

/**
 * Logistic Regression using gradient descent X: N * M matrix where N = number
 * of examples and M = number of features.
 * 
 * @author weninger
 * 
 */
public class LogisticRegressionFit {

	private Vector<Vector<Float>> X;
	private Vector<Float> Y;
	private Vector<Float> Theta;
	private int M; // number of features

	public LogisticRegressionFit() {
	}

	public LogisticRegressionPrediction CalcLogRegNewton(
			final Vector<Vector<Float>> XPt, final Vector<Float> yPt,
			final String PlotNm, final double ChangeEps, final int MaxStep,
			final boolean Intercept) {
		X = XPt;
		Y = yPt;
		Theta = new Vector<Float>();
		assert (X.size() == Y.size());
		if (Intercept == false) { // if intercept is not included, add it
			for (int r = 0; r < X.size(); r++) {
				X.get(r).add(1f);
			}
		}
		M = X.get(0).size();
		for (int r = 0; r < X.size(); r++) {
			assert (X.get(r).size() == M);
		}
		for (int r = 0; r < Y.size(); r++) {
			if (Y.get(r) >= 0.99999) {
				Y.set(r, (float) 0.99999);
			}
			if (Y.get(r) <= 0.00001) {
				Y.set(r, (float) 0.00001);
			}
		}

		for (int i = 0; i < M; i++) {
			Theta.add(0f);
		}
		MLENewton(ChangeEps, MaxStep, PlotNm);
		return new LogisticRegressionPrediction(Theta);
	}

	public int MLENewton(final double ChangeEps, final int MaxStep,
			final String PlotNm) {
		Long ExeTm = System.currentTimeMillis();
		
		Vector<Float>[] DeltaLV = new Vector[1];
		DeltaLV[0] = new Vector<Float>();
		Vector<Vector<Float>>[] HVV = new Vector[1];		
		int iter = 0;
		double MinVal = -1e10, MaxVal = 1e10;
		for (iter = 0; iter < MaxStep; iter++) {		
			Vector<Float> GradV = Gradient();
			HVV[0] = Hessian();
			GetNewtonStep(HVV, GradV, DeltaLV);
			double Increment = LinearAlgebra.dotProduct(GradV, DeltaLV[0]);
			if (Increment <= ChangeEps) {
				break;
			}
			// InitLearnRate/double(0.01*(double)iter + 1);
			double LearnRate = GetStepSizeByLineSearch(DeltaLV[0], GradV,
					0.15f, 0.5f);			
			for (int i = 0; i < Theta.size(); i++) {
				double Change = LearnRate * DeltaLV[0].get(i);
				Theta.set(i, (float) (Theta.get(i) + Change));
				if (Theta.get(i) < MinVal) {
					Theta.set(i, (float) MinVal);
				}
				if (Theta.get(i) > MaxVal) {
					Theta.set(i, (float) MaxVal);
				}
			}
		}
		if (!PlotNm.isEmpty()) {
			System.out
					.printf("MLE with Newton method completed with %d iterations(%s)\n",
							iter, (System.currentTimeMillis() - ExeTm));
		}

		return iter;
	}

	public double GetStepSizeByLineSearch(final Vector<Float> DeltaV,
			final Vector<Float> GradV, final double Alpha, final double Beta) {
		double StepSize = 1.0;
		double InitLikelihood = Likelihood();
		assert (Theta.size() == DeltaV.size());
		Vector<Float> NewThetaV = new Vector<Float>(Theta.size());
		double MinVal = -1e10, MaxVal = 1e10;
		for (int iter = 0;; iter++) {
			for (int i = 0; i < Theta.size(); i++) {
				NewThetaV.add((float) (Theta.get(i) + StepSize * DeltaV.get(i)));
				if (NewThetaV.get(i) < MinVal) {
					NewThetaV.set(i, (float) MinVal);
				}
				if (NewThetaV.get(i) > MaxVal) {
					NewThetaV.set(i, (float) MaxVal);
				}
			}
			if (Likelihood(NewThetaV) < InitLikelihood + Alpha * StepSize
					* LinearAlgebra.dotProduct(GradV, DeltaV)) {
				StepSize *= Beta;
			} else {
				break;
			}
		}
		return StepSize;
	}

	public double Likelihood(final Vector<Float> NewTheta) {
		Vector<Float> OutV = new Vector<Float>();
		LogisticRegressionPrediction.GetCfy(X, OutV, NewTheta);
		double L = 0;
		for (int r = 0; r < OutV.size(); r++) {
			L += Y.get(r) * Math.log(OutV.get(r));
			L += (1 - Y.get(r)) * Math.log(1 - OutV.get(r));
		}
		return L;
	}

	public double Likelihood() {
		return Likelihood(Theta);
	}

	public Vector<Float> Gradient() {
		Vector<Float> GradV = new Vector<Float>(); 
		Vector<Float> OutV = new Vector<Float>();
		LogisticRegressionPrediction.GetCfy(X, OutV, Theta);
		for (int i = 0; i < M; i++) {
			GradV.add(0f);
		}
		for (int r = 0; r < X.size(); r++) {
			// printf("Y[%d] = %f, Out[%d] = %f\n", r, Y[r].Val, r,
			// OutV[r].Val);
			for (int m = 0; m < M; m++) {
				GradV.set(m, GradV.get(m) + (Y.get(r) - OutV.get(r))
						* X.get(r).get(m));
			}
		}
		return GradV;
		// for (int m = 0; m < M; m++) {
		// printf("Theta[%d] = %f, GradV[%d] = %f\n", m, Theta[m].Val, m,
		// GradV[m].Val); }
	}

	public Vector<Vector<Float>> Hessian() {
		Vector<Vector<Float>> HVV = new Vector<Vector<Float>>();
		for (int i = 0; i < Theta.size(); i++) {
			HVV.add(new Vector<Float>());
			for (int j = 0; j < Theta.size(); j++) {
				HVV.get(i).add(0f);
			}
		}
		Vector<Float> OutV = new Vector<Float>();
		LogisticRegressionPrediction.GetCfy(X, OutV, Theta);
		for (int i = 0; i < X.size(); i++) {
			for (int r = 0; r < Theta.size(); r++) {
				HVV.get(r).set(
						r,
						HVV.get(r).get(r)
								+ -(X.get(i).get(r) * OutV.get(i)
										* (1 - OutV.get(i)) * X.get(i).get(r)));
				for (int c = r + 1; c < Theta.size(); c++) {
					HVV.get(r).set(
							c,
							HVV.get(r).get(c)
									+ -(X.get(i).get(r) * OutV.get(i)
											* (1 - OutV.get(i)) * X.get(i).get(
											c)));
					HVV.get(c).set(
							r,
							HVV.get(c).get(r)
									+ -(X.get(i).get(r) * OutV.get(i)
											* (1 - OutV.get(i)) * X.get(i).get(
											c)));
				}
			}
		}
		
		return HVV;
		/*
		 * printf("\n"); for (int r = 0; r < Theta.Len(); r++) { for (int c = 0;
		 * c < Theta.Len(); c++) { printf("%f\t", HVV.At(r, c).Val); }
		 * printf("\n"); }
		 */
	}

	public void GetNewtonStep(Vector<Vector<Float>>[] HVV,
			final Vector<Float> GradV, Vector<Float>[] DeltaLV) {
		boolean HSingular = false;
		  for (int i = 0; i < HVV[0].size(); i++) {
		    if (HVV[0].get(i).get(i) == 0.0) {
		      HVV[0].get(i).set(i, 0.001f);
		      HSingular = true;
		    }
		    DeltaLV[0].add(GradV.get(i) / HVV[0].get(i).get(i));
		  }
		  if (! HSingular) {
		    if ( HVV[0].get(0).get(0) < 0) { // if Hessian is negative definite, convert it to positive definite
		      for (int r = 0; r < Theta.size(); r++) {
		        for (int c = 0; c < Theta.size(); c++) {
		          HVV[0].get(r).set(c, - HVV[0].get(r).get(c));
		        }
		      }
		      Numerical.SolveSymetricSystem(HVV, GradV, DeltaLV);
		    }
		    else {
		    	Numerical.SolveSymetricSystem(HVV, GradV, DeltaLV);
		      for (int i = 0; i < DeltaLV[0].size(); i++) {
		        DeltaLV[0].set(i, - DeltaLV[0].get(i));
		      }
		    }

		  }
	}

	public LogisticRegressionPrediction CalcLogRegNewton(
			Vector<Vector<Float>> XV, Vector<Float> YV, String outfileprefix) {
		return CalcLogRegNewton(XV, YV, outfileprefix, 0.01, 200, false);
	}

}
