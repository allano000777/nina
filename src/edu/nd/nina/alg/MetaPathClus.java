package edu.nd.nina.alg;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.nd.nina.Type;
import edu.nd.nina.graph.TypedSimpleGraph;
import edu.nd.nina.math.Moment;
import edu.nd.nina.types.kddcup2013.Author;
import edu.nd.nina.types.kddcup2013.Paper;
import edu.nd.nina.types.kddcup2013.Venue;

public class MetaPathClus {
	TypedSimpleGraph tsg = null;
	Map<Type, Integer> pathNormCount;

	public MetaPathClus(TypedSimpleGraph tsg) {
		this.tsg = tsg;
		this.pathNormCount = new HashMap<Type, Integer>();
	}

	public void go(Author a) {
		ConstrainedRandomWalkWithRestart crwr = new ConstrainedRandomWalkWithRestart(
				tsg, 0.15f);

		MetaPath mp = new MetaPath(a);
		mp.addToPath(Paper.class);
		mp.addToPath(Venue.class);
		mp.addToPath(Paper.class);
		mp.addToPath(Author.class);
		Map<Type, Integer> count = crwr.pathCount(mp);
		Integer normCountA = pathNormCount.get(a);
		pathNormCount.put(a, normCountA);
		if (normCountA == null) {
			normCountA = crwr.pathNormCount(a, mp);
		}
		Map<Type, Float> normCount = new HashMap<Type, Float>();
		for (Type z : count.keySet()) {
			Integer normCountB = pathNormCount.get(z);
			if (normCountB == null) {
				mp.setStart(z);
				normCountB = crwr.pathNormCount(z, mp);
				pathNormCount.put(z, normCountB);
			}
			Integer x = count.get(z);
			normCount.put(z, (float) x / (float) (normCountA + normCountB));
		}
		mp.setStart(a);

		Map<Type, Moment> rw = crwr.randomWalk(a, mp);
		System.out.println(a + " -> ");
		IntValueComparator bvc = new IntValueComparator(count);
		SortedMap<Type, Integer> sortedCount = new TreeMap<Type, Integer>(bvc);
		sortedCount.putAll(count);

		for (Moment m : rw.values()) {
			m.def();
		}

		MeanValueComparator mvc = new MeanValueComparator(rw);
		SortedMap<Type, Moment> sortedMeanCount = new TreeMap<Type, Moment>(mvc);
		sortedMeanCount.putAll(rw);

		int i = 0;
		for (Type ty : sortedMeanCount.keySet()) {
			if (++i >= 10)
				break;
			System.out.println("\t" + ty + ": " + count.get(ty) + " - "
					+ normCount.get(ty) + " - " + rw.get(ty).getMean());
		}

	}

}

class IntValueComparator implements Comparator<Type> {

	Map<Type, Integer> base;

	public IntValueComparator(Map<Type, Integer> base) {
		this.base = base;
	}

	// Note: this comparator imposes orderings that are inconsistent with
	// equals.
	public int compare(Type a, Type b) {
		if (base.get(a) >= base.get(b)) {
			return -1;
		} else {
			return 1;
		} // returning 0 would merge keys
	}
}

class MeanValueComparator implements Comparator<Type> {

	Map<Type, Moment> base;

	public MeanValueComparator(Map<Type, Moment> base) {
		this.base = base;
	}

	// Note: this comparator imposes orderings that are inconsistent with
	// equals.
	public int compare(Type a, Type b) {
		if (base.get(a).getMean() >= base.get(b).getMean()) {
			return -1;
		} else {
			return 1;
		} // returning 0 would merge keys
	}
}
