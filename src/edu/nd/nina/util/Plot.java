package edu.nd.nina.util;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Map.Entry;

import com.panayotis.gnuplot.JavaPlot;
import com.panayotis.gnuplot.dataset.Point;
import com.panayotis.gnuplot.dataset.PointDataSet;
import com.panayotis.gnuplot.plot.AbstractPlot;
import com.panayotis.gnuplot.style.Style;

import edu.nd.nina.math.Moment;
import edu.nd.nina.structs.Pair;
import edu.nd.nina.structs.Triple;

public class Plot {
	public static int addPlot(JavaPlot GP,
			final Hashtable<Float, Moment> ValMomH, final Style SeriesTy,
			final String Label, final String Style, boolean PlotAvg,
			boolean PlotMed, boolean PlotMin, boolean PlotMax,
			boolean PlotSDev, boolean PlotStdErr, final boolean ExpBucket) {
		Vector<Triple<Float, Float, Float>> AvgV = new Vector<Triple<Float, Float, Float>>();
		Vector<Triple<Float, Float, Float>> StdErrV = new Vector<Triple<Float, Float, Float>>();
		Vector<Pair<Float, Float>> AvgV2 = new Vector<Pair<Float, Float>>();
		Vector<Pair<Float, Float>> MedV = new Vector<Pair<Float, Float>>();
		Vector<Pair<Float, Float>> MinV = new Vector<Pair<Float, Float>>();
		Vector<Pair<Float, Float>> MaxV = new Vector<Pair<Float, Float>>();
		Vector<Pair<Float, Float>> BucketV = new Vector<Pair<Float, Float>>();
		for (Entry<Float, Moment> e : ValMomH.entrySet()) {
			Moment Mom = e.getValue();
			if (!Mom.isDef()) {
				Mom.def();
			}
			final float x = e.getKey();
			if (PlotAvg) {
				if (PlotSDev) {
					AvgV.add(new Triple<Float, Float, Float>(x, Mom.getMean(),
							Mom.getSDev()));
				} // std deviation
				else {
					AvgV2.add(new Pair<Float, Float>(x, Mom.getMean()));
				}
				if (PlotStdErr) {
					StdErrV.add(new Triple<Float, Float, Float>(x, Mom
							.getMean(), (float) (Mom.getSDev() / Math.sqrt(Mom
							.getVals()))));
				}
			}
			if (PlotMed) {
				MedV.add(new Pair<Float, Float>(x, Mom.getMedian()));
			}
			if (PlotMin) {
				MinV.add(new Pair<Float, Float>(x, Mom.getMin()));
			}
			if (PlotMax) {
				MaxV.add(new Pair<Float, Float>(x, Mom.getMax()));
			}
		}
		Collections.sort(AvgV);
		Collections.sort(AvgV2);
		Collections.sort(MedV);
		Collections.sort(MinV);
		Collections.sort(MaxV);

		int PlotId = 0;
		// exponential bucketing
		/*
		if (!AvgV2.isEmpty()) {
			BucketV = makeExpBins(AvgV2);
			Vector<Pair<Float, Float>> t = BucketV;
			BucketV = AvgV2;
			AvgV2 = t;
		}
		if (!MedV.isEmpty()) {
			BucketV = makeExpBins(MedV);
			Vector<Pair<Float, Float>> t = BucketV;
			BucketV = MedV;
			MedV = t;
		}
		if (!MinV.isEmpty()) {
			BucketV = makeExpBins(MinV);
			Vector<Pair<Float, Float>> t = BucketV;
			BucketV = MinV;
			MinV = t;
		}
		if (!MaxV.isEmpty()) {
			BucketV = makeExpBins(MaxV);
			Vector<Pair<Float, Float>> t = BucketV;			
			BucketV = MaxV;
			MaxV = t;
		}*/
		// plot
		if (!AvgV.isEmpty()) {
			PlotId = addErrBar(GP, AvgV, Label + " Average", "StdDev");
		}
		if (!AvgV2.isEmpty()) {
			double[][] zz = new double[AvgV2.size()][2];
			for(int i = 0; i<AvgV2.size(); i++){
				zz[i][0] = AvgV2.get(i).p1;
				zz[i][1] = AvgV2.get(i).p2;
			}
			GP.addPlot(zz);
			int x = GP.getPlots().size()-1;
			((AbstractPlot) GP.getPlots().get(x)).setTitle(Label + " Average");
			((AbstractPlot) GP.getPlots().get(x)).getPlotStyle().setStyle(SeriesTy);
			
			PlotId = x;
		}
		if (!MedV.isEmpty()) {
			double[][] zz = new double[MedV.size()][2];
			for(int i = 0; i<MedV.size(); i++){
				zz[i][0] = MedV.get(i).p1;
				zz[i][1] = MedV.get(i).p2;
			}
			GP.addPlot(zz);
			int x = GP.getPlots().size()-1;
			((AbstractPlot) GP.getPlots().get(x)).setTitle(Label + " Median");
			((AbstractPlot) GP.getPlots().get(x)).getPlotStyle().setStyle(SeriesTy);
			
			PlotId = x;			
		}
		if (!MinV.isEmpty()) {
			double[][] zz = new double[MinV.size()][2];
			for(int i = 0; i<MinV.size(); i++){
				zz[i][0] = MinV.get(i).p1;
				zz[i][1] = MinV.get(i).p2;
			}
			GP.addPlot(zz);
			int x = GP.getPlots().size()-1;
			((AbstractPlot) GP.getPlots().get(x)).setTitle(Label + " Min");
			((AbstractPlot) GP.getPlots().get(x)).getPlotStyle().setStyle(SeriesTy);
			
			PlotId = x;			
		}
		if (!MaxV.isEmpty()) {
			double[][] zz = new double[MaxV.size()][2];
			for(int i = 0; i<MaxV.size(); i++){
				zz[i][0] = MaxV.get(i).p1;
				zz[i][1] = MaxV.get(i).p2;
			}
			GP.addPlot(zz);
			int x = GP.getPlots().size()-1;
			((AbstractPlot) GP.getPlots().get(x)).setTitle(Label + " Max");
			((AbstractPlot) GP.getPlots().get(x)).getPlotStyle().setStyle(SeriesTy);
			
			PlotId = x;
		}
		if (!StdErrV.isEmpty()) {
			PlotId = addErrBar(GP, StdErrV, Label + " Standard error", Style);
		}
		return PlotId;
	}

	/**
	 * 
	 * @param GP
	 * @param XYDValV
	 * @param Label
	 * @param style
	 * @return
	 */
	private static int addErrBar(JavaPlot GP,
			Vector<Triple<Float, Float, Float>> XYDValV, String Label,
			String style) {
		Vector<Pair<Float, Float>> XYFltValV = new Vector<Pair<Float, Float>>(
				XYDValV.size());
		Vector<Float> DeltaV = new Vector<Float>(XYDValV.size());
		for (int i = 0; i < XYDValV.size(); i++) {
			XYFltValV.add(new Pair<Float, Float>(XYDValV.get(i).v1, XYDValV
					.get(i).v2));
			DeltaV.add(XYDValV.get(i).v3);
		}
		return addErrBar(GP, XYFltValV, DeltaV, Label);		
	}

	private static int addErrBar(JavaPlot GP, Vector<Pair<Float, Float>> XYFltValV,
			Vector<Float> deltaV, String label) {
		PointDataSet<Float> zz = new PointDataSet<Float>();
		for(int i = 0; i<XYFltValV.size(); i++){
			Point<Float> p = new Point<Float>(XYFltValV.get(i).p1, XYFltValV.get(i).p2, deltaV.get(i));
			zz.add(p);
		}
		GP.addPlot(zz);		
				
		int x = GP.getPlots().size()-1;
		((AbstractPlot) GP.getPlots().get(x)).setTitle(label);
		((AbstractPlot) GP.getPlots().get(x)).getPlotStyle().setStyle(Style.YERRORBARS);
		
		return x;
	}

	/**
	 * 
	 * @param avgV2
	 * @return
	 */
	private static Vector<Pair<Float, Float>> makeExpBins(
			Vector<Pair<Float, Float>> avgV2) {
		return makeExpBins(avgV2, 2, 1);
	}

	/**
	 * 
	 * @param XYValV
	 * @param BinFactor
	 * @param MinYVal
	 * @return
	 */
	private static Vector<Pair<Float, Float>> makeExpBins(
			Vector<Pair<Float, Float>> XYValV, final double BinFactor,
			final double MinYVal) {
		Vector<Pair<Float, Float>> ExpXYValV = new Vector<Pair<Float, Float>>();
		if (XYValV.isEmpty()) {
			ExpXYValV.clear();
			return ExpXYValV;
		}
		assert (!XYValV.isEmpty());

		final Float MxX = XYValV.firstElement().p1;
		// find buckets
		Vector<Float> BucketEndV = new Vector<Float>();
		BucketEndV.add(1f);
		double PrevBPos = 1, BPos = 1;
		while (BPos <= MxX) {
			PrevBPos = Math.floor(BPos);
			BPos *= BinFactor;
			if (Math.floor(BPos) == PrevBPos) {
				BPos = PrevBPos + 1;
			}
			BucketEndV.add((float) Math.floor(BPos));
		}
		// printf("buckets:\n"); for (int i = 0; i < BucketEndV.Len(); i++) {
		// printf("\t%g\n", BucketEndV[i]);}

		for (int i = 0; i < BucketEndV.size(); i++)
			ExpXYValV.add(new Pair<Float, Float>(0f, 0f));
		int CurB = 0;
		double AvgPos = 0, Cnt = 0, AvgVal = 0;
		for (int v = 0; v < XYValV.size(); v++) {
			if (XYValV.get(v).p1 == 0.0) {
				continue;
			}
			AvgPos += XYValV.get(v).p1;// * XYValV[v].Dat; // x
			AvgVal += XYValV.get(v).p2; // y
			Cnt++;
			if (v + 1 == XYValV.size()
					|| XYValV.get(v + 1).p1 > BucketEndV.get(CurB)) {
				if (Cnt != 0) {
					// AvgPos /= AvgVal;
					// AvgVal /= (BucketEndV[CurB]-BucketEndV[CurB-1]);
					AvgPos /= (double) Cnt;
					AvgVal /= (double) Cnt;
					if (AvgVal < MinYVal) {
						AvgVal = MinYVal;
					}
					ExpXYValV.add(new Pair<Float, Float>((float) AvgPos,
							(float) AvgVal));
					// printf("b: %6.2f\t%6.2f\n", AvgPos, AvgVal);
					AvgPos = 0;
					AvgVal = 0;
					Cnt = 0;
				}
				CurB++;
			}
		}
		return ExpXYValV;
	}

}
