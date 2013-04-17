package edu.nd.nina.math;

import java.util.Collections;
import java.util.Vector;

import edu.nd.nina.structs.Pair;

/**
 * Statistical-Moments
 * 
 * @author weninger
 * 
 */
public class Moment {

	// ClassTPV(TMom, PMom, TMomV)//{

	private boolean DefP;
	private Vector<Pair<Float, Float>> ValWgtV;
	private Float SumW, ValSumW;
	private Integer Vals;
	private boolean UsableP;
	private Float UnusableVal;
	private Float Mn, Mx;
	private Float Mean, Vari, SDev, SErr;
	private Float Median, Quart1, Quart3;
	private Vector<Float> DecileV; // 0=min 1=1.decile, ..., 9=9.decile, 10=max
	private Vector<Float> PercentileV; // 0=min 1=1.percentile, ...,
										// 9=9.percentile, 10=max

	public Moment() {
		DefP = false;
		ValWgtV = new Vector<Pair<Float, Float>>();
		SumW = 0f;
		ValSumW = 0f;
		Vals = 0;
		UsableP = false;
		UnusableVal = -1f;
		Mn = (float)Integer.MAX_VALUE;
		Mx = (float)Integer.MIN_VALUE;
		Mean = 0f;
		Vari = 0f;
		SDev = 0f;
		SErr = 0f;
		Median = 0f;
		Quart1 = 0f;
		Quart3 = 0f;
		DecileV = new Vector<Float>();
		PercentileV = new Vector<Float>();
	}

	public Moment(final Moment Mom) {
		DefP = Mom.DefP;
		ValWgtV = Mom.ValWgtV;
		SumW = Mom.SumW;
		ValSumW = Mom.ValSumW;
		Vals = Mom.Vals;
		UsableP = Mom.UsableP;
		UnusableVal = Mom.UnusableVal;
		Mn = Mom.Mn;
		Mx = Mom.Mx;
		Mean = Mom.Mean;
		Vari = Mom.Vari;
		SDev = Mom.SDev;
		SErr = Mom.SErr;
		Median = Mom.Median;
		Quart1 = Mom.Quart1;
		Quart3 = Mom.Quart3;
		DecileV = Mom.DecileV;
		PercentileV = Mom.PercentileV;
	}

	public void add(float d) {
		add(d,1f);
	}
	
	public void add(float Val, float Wgt) {
		assert(!DefP);
	    ValWgtV.add(new Pair<Float, Float>(Val, Wgt)); 
	    SumW+=Wgt; 
	    ValSumW+=Wgt*Val; 
	    Vals++;
	}

	public boolean isDef() {
		return DefP;
	}

	public void def() {
		assert (!DefP);
		DefP = true;
		UsableP = (SumW > 0) && (ValWgtV.size() > 0);
		if (UsableP) {
			// Mn, Mx
			Mn = ValWgtV.get(0).p1;
			Mx = ValWgtV.get(0).p1;
			// Mean, Variance (Mn, Mx), Standard-Error
			Mean = ValSumW / SumW;
			Vari = 0f;
			if (ValWgtV.size() > 1) {
				for (int ValN = 0; ValN < ValWgtV.size(); ValN++) {
					final double Val = ValWgtV.get(ValN).p1;
					Vari += ValWgtV.get(ValN).p2
							* (float) Math.sqrt(Math.abs(Val - Mean));
					if (Val < Mn) {
						Mn = (float) Val;
					}
					if (Val > Mx) {
						Mx = (float) Val;
					}
				}
				Vari = Vari / SumW;
				SErr = (float) Math.sqrt(Vari
						/ (ValWgtV.size() * (ValWgtV.size() - 1)));
			}
			// Standard-Deviation
			SDev = (float) Math.sqrt(Vari);
			// Median
			Collections.sort(ValWgtV);

			double CurSumW = 0;
			for (int ValN = 0; ValN < ValWgtV.size(); ValN++) {
				CurSumW += ValWgtV.get(ValN).p2;
				if (CurSumW > 0.5 * SumW) {
					Median = ValWgtV.get(ValN).p1;
					break;
				} else if (CurSumW == 0.5 * SumW) {
					Median = (float) (0.5 * (ValWgtV.get(ValN).p1 + ValWgtV
							.get(ValN + 1).p1));
					break;
				}
			}
			// Quartile-1 and Quartile-3
			Quart1 = Quart3 = (float) Integer.MIN_VALUE;
			CurSumW = 0;
			for (int ValN = 0; ValN < ValWgtV.size(); ValN++) {
				CurSumW += ValWgtV.get(ValN).p2;
				if (Quart1 == (float) Integer.MIN_VALUE) {
					if (CurSumW > 0.25 * SumW) {
						Quart1 = ValWgtV.get(ValN).p1;
					}
					// else if (CurSumW == 0.25*SumW) { Quart1 = 0.5 *
					// (ValWgtV[ValN].Val1+ValWgtV[ValN+1].Val1); }
				}
				if (Quart3 == (float) Integer.MIN_VALUE) {
					if (CurSumW > 0.75 * SumW) {
						Quart3 = ValWgtV.get(ValN).p1;
					}
					// else if (CurSumW == 0.75*SumW) { Quart3 = 0.5 *
					// (ValWgtV[ValN].Val1+ValWgtV[ValN+1].Val1); }
				}
			}
			// Deciles & Percentiles
			CurSumW = 0;
			int DecileN = 1, PercentileN = 1;
			DecileV = new Vector<Float>(11);
			for (int i = 0; i < 11; i++)
				DecileV.add(0f);
			PercentileV = new Vector<Float>(101);
			for (int i = 0; i < 101; i++)
				PercentileV.add(0f);
			DecileV.set(0, Mn);
			DecileV.set(10, Mx);
			PercentileV.set(0, Mn);
			PercentileV.set(100, Mx);
			for (int ValN = 0; ValN < ValWgtV.size(); ValN++) {
				CurSumW += ValWgtV.get(ValN).p2;
				if (CurSumW > SumW * DecileN * 0.1) {
					DecileV.set(DecileN, ValWgtV.get(ValN).p1);
					DecileN++;
				}
				if (CurSumW > SumW * PercentileN * 0.01) {
					PercentileV.set(PercentileN, ValWgtV.get(ValN).p1);
					PercentileN++;
				}
			}
		}
		ValWgtV.clear();
	}

	public Float getMean() {
		return Mean;
	}

	public Float getSDev() {
		return SDev;
	}

	public Integer getVals() {
		return Vals;
	}

	public Float getMedian() {
		return Median;
	}

	public Float getMin() {
		return Mn;
	}

	public Float getMax() {
		return Mx;
	}
}
