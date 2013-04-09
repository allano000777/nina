package edu.nd.nina.math;

import java.util.Random;

public class Randoms {
	private final static int RndSeed = 1;
	private final static int a = 16807;
	private final static int m = 2147483647;
	private final static int q = 127773; // m DIV a
	private final static int r = 2836; // m MOD a

	private int seed;

	private Random rand;

	public Randoms(int _seed) {
		putSeed(_seed);
		move(0);
		rand = new Random(seed);
	}

	public Randoms() {
		this(RndSeed);
	}

	private void move(int steps) {
		for (int StepN = 0; StepN < steps; StepN++) {
			getNextSeed();
		}
	}

	private void putSeed(int _seed) {
		assert (_seed >= 0);
		if (_seed == 0) {
			seed = Math.abs((int) System.currentTimeMillis());
		} else {
			seed = _seed;
		}
	}

	/** Return a random boolean, equally likely to be true or false. */
	public synchronized boolean nextBoolean() {
		return (next(32) & 1 << 15) != 0;
	}

	/** Return a random boolean, with probability p of being true. */
	public synchronized boolean nextBoolean(double p) {
		double u = GetUniDev();
		if (u < p)
			return true;
		return false;
	}

	public Double getBinomialDeviance(Double prob, Integer trials) {

		int j;
		int nold = -1;
		double am, em, g, angle, p, bnl, sq, t, y;
		double pold = -1.0, pc = 0, plog = 0, pclog = 0, en = 0, oldg = 0;

		p = (prob <= 0.5 ? prob : 1.0 - prob);
		am = trials * p;
		if (trials < 25) {
			bnl = 0.0;
			for (j = 1; j <= trials; j++)
				if (GetUniDev() < p)
					++bnl;
		} else if (am < 1.0) {
			g = Math.exp(-am);
			t = 1.0;
			for (j = 0; j <= trials; j++) {
				t *= GetUniDev();
				if (t < g)
					break;
			}
			bnl = (j <= trials ? j : trials);
		} else {
			if (trials != nold) {
				en = trials;
				oldg = GammaFunction.lnGamma(en + 1.0);
				nold = trials;
			}
			if (p != pold) {
				pc = 1.0 - p;
				plog = Math.log(p);
				pclog = Math.log(pc);
				pold = p;
			}
			sq = Math.sqrt(2.0 * am * pc);
			do {
				do {
					angle = Math.PI * GetUniDev();
					y = Math.tan(angle);
					em = sq * y + am;
				} while (em < 0.0 || em >= (en + 1.0));
				em = Math.floor(em);
				t = 1.2
						* sq
						* (1.0 + y * y)
						* Math.exp(oldg - (em + 1.0)
								- GammaFunction.lnGamma(en - em + 1.0) + em
								* plog + (en - em) * pclog);
			} while (GetUniDev() > t);
			bnl = em;
		}
		if (p != prob)
			bnl = trials - bnl;
		return bnl;
	}

	public double GetUniDev() {
		return getNextSeed() / ((double) m);
	}

	private int getNextSeed() {
		if ((seed = a * (seed % q) - r * (seed / q)) > 0) {
			return seed;
		} else {
			return seed += m;
		}
	}

	public int GetUniDevInt(int range) {
		int seed = getNextSeed();
		if (range == 0) {
			return seed;
		} else {
			return seed % range;
		}
	}

	public int next(int size) {
		return rand.nextInt(size);
	}

	public Random getRandom() {
		return rand;
	}

	public int GetGeoDev(double Prb) {
		return 1 + (int) Math.floor(Math.log(1.0 - GetUniDev())
				/ Math.log(1.0 - Prb));
	}

	/** Draw a single sample from multinomial "a". */
	public synchronized int GetDiscrete(double[] a) {
		double b = 0, r = GetUniDev();
		for (int i = 0; i < a.length; i++) {
			b += a[i];
			if (b > r) {
				return i;
			}
		}
		return a.length - 1;
	}

	/**
	 * draw a single sample from (unnormalized) multinomial "a", with
	 * normalizing factor "sum".
	 */
	public synchronized int GetDiscrete(double[] a, double sum) {
		double b = 0, r = GetUniDev() * sum;
		for (int i = 0; i < a.length; i++) {
			b += a[i];
			if (b > r) {
				return i;
			}
		}
		return a.length - 1;
	}

	private double nextGaussian;
	private boolean haveNextGaussian = false;

	/**
	 * Return a random double drawn from a Gaussian distribution with mean 0 and
	 * variance 1.
	 */
	public synchronized double GetGaussian() {
		if (!haveNextGaussian) {
			double v1 = GetUniDev(), v2 = GetUniDev();
			double x1, x2;
			x1 = Math.sqrt(-2 * Math.log(v1)) * Math.cos(2 * Math.PI * v2);
			x2 = Math.sqrt(-2 * Math.log(v1)) * Math.sin(2 * Math.PI * v2);
			nextGaussian = x2;
			haveNextGaussian = true;
			return x1;
		} else {
			haveNextGaussian = false;
			return nextGaussian;
		}
	}

	/**
	 * Return a random double drawn from a Gaussian distribution with mean m and
	 * variance s2.
	 */
	public synchronized double nextGaussian(double m, double s2) {
		return GetGaussian() * Math.sqrt(s2) + m;
	}

	/**
	 * Return random integer from Poission with parameter lambda. The mean of
	 * this distribution is lambda. The variance is lambda.
	 */
	public synchronized int GetPoisson(double lambda) {
		int v = -1;
		double l = Math.exp(-lambda), p;
		p = 1.0;
		while (p >= l) {
			p *= GetUniDev();
			v++;
		}
		return v;
	}

	/** Return nextPoisson(1). */
	public synchronized int GetPoisson() {
		return GetPoisson(1);
	}

	// generate Gamma(1,1)
	// E(X)=1 ; Var(X)=1
	/**
	 * Return a random double drawn from a Gamma distribution with mean 1.0 and
	 * variance 1.0.
	 */
	public synchronized double GetGamma() {
		return GetGamma(1, 1, 0);
	}

	/**
	 * Return a random double drawn from a Gamma distribution with mean alpha
	 * and variance 1.0.
	 */
	public synchronized double nextGamma(double alpha) {
		return GetGamma(alpha, 1, 0);
	}

	/* Return a sample from the Gamma distribution, with parameter IA */
	/* From Numerical "Recipes in C", page 292 */
	public synchronized double oldGetGamma(int ia) {
		int j;
		double am, e, s, v1, v2, x, y;

		assert (ia >= 1);
		if (ia < 6) {
			x = 1.0;
			for (j = 1; j <= ia; j++)
				x *= GetUniDev();
			x = -Math.log(x);
		} else {
			do {
				do {
					do {
						v1 = 2.0 * GetUniDev() - 1.0;
						v2 = 2.0 * GetUniDev() - 1.0;
					} while (v1 * v1 + v2 * v2 > 1.0);
					y = v2 / v1;
					am = ia - 1;
					s = Math.sqrt(2.0 * am + 1.0);
					x = s * y + am;
				} while (x <= 0.0);
				e = (1.0 + y * y) * Math.exp(am * Math.log(x / am) - s * y);
			} while (GetUniDev() > e);
		}
		return x;
	}

	/**
	 * Return a random double drawn from a Gamma distribution with mean
	 * alpha*beta and variance alpha*beta^2.
	 */
	public synchronized double GetGamma(double alpha, double beta) {
		return GetGamma(alpha, beta, 0);
	}

	/**
	 * Return a random double drawn from a Gamma distribution with mean
	 * alpha*beta+lamba and variance alpha*beta^2. Note that this means the pdf
	 * is:
	 * <code>frac{ x^{alpha-1} exp(-x/beta) }{ beta^alpha Gamma(alpha) }</code>
	 * in other words, beta is a "scale" parameter. An alternative
	 * parameterization would use 1/beta, the "rate" parameter.
	 */
	public synchronized double GetGamma(double alpha, double beta, double lambda) {
		double gamma = 0;
		if (alpha <= 0 || beta <= 0) {
			throw new IllegalArgumentException(
					"alpha and beta must be strictly positive.");
		}
		if (alpha < 1) {
			double b, p;
			boolean flag = false;

			b = 1 + alpha * Math.exp(-1);

			while (!flag) {
				p = b * GetUniDev();
				if (p > 1) {
					gamma = -Math.log((b - p) / alpha);
					if (GetUniDev() <= Math.pow(gamma, alpha - 1)) {
						flag = true;
					}
				} else {
					gamma = Math.pow(p, 1.0 / alpha);
					if (GetUniDev() <= Math.exp(-gamma)) {
						flag = true;
					}
				}
			}
		} else if (alpha == 1) {
			// Gamma(1) is equivalent to Exponential(1). We can
			// sample from an exponential by inverting the CDF:

			gamma = -Math.log(GetUniDev());

			// There is no known closed form for Gamma(alpha != 1)...
		} else {

			// This is Best's algorithm: see pg 410 of
			// Luc Devroye's "non-uniform random variate generation"
			// This algorithm is constant time for alpha > 1.

			double b = alpha - 1;
			double c = 3 * alpha - 0.75;

			double u, v;
			double w, y, z;

			boolean accept = false;

			while (!accept) {
				u = GetUniDev();
				v = GetUniDev();

				w = u * (1 - u);
				y = Math.sqrt(c / w) * (u - 0.5);
				gamma = b + y;

				if (gamma >= 0.0) {
					z = 64 * w * w * w * v * v; // ie: 64 * w^3 v^2

					accept = z <= 1.0 - ((2 * y * y) / gamma);

					if (!accept) {
						accept = (Math.log(z) <= 2 * (b * Math.log(gamma / b) - y));
					}
				}
			}

			/*
			 * // Old version, uses time linear in alpha double y = -Math.log
			 * (nextUniform ()); while (nextUniform () > Math.pow (y * Math.exp
			 * (1 - y), alpha - 1)) y = -Math.log (nextUniform ()); gamma =
			 * alpha * y;
			 */
		}
		return beta * gamma + lambda;
	}

	/**
	 * Return a random double drawn from an Exponential distribution with mean 1
	 * and variance 1.
	 */
	public synchronized double GetExp() {
		return GetGamma(1, 1, 0);
	}

	/**
	 * Return a random double drawn from an Exponential distribution with mean
	 * beta and variance beta^2.
	 */
	public synchronized double GetExp(double beta) {
		return GetGamma(1, beta, 0);
	}

	/**
	 * Return a random double drawn from an Exponential distribution with mean
	 * beta+lambda and variance beta^2.
	 */
	public synchronized double GetExp(double beta, double lambda) {
		return GetGamma(1, beta, lambda);
	}
}
