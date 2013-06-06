package edu.nd.nina.alg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.nd.nina.Graphs;
import edu.nd.nina.Type;
import edu.nd.nina.graph.TypedSimpleGraph;
import edu.nd.nina.math.Moment;
import edu.nd.nina.structs.Triple;

public class ConstrainedRandomWalkWithRestart {

	float restartProbability;
	TypedSimpleGraph tsg;

	public ConstrainedRandomWalkWithRestart(TypedSimpleGraph tsg,
			float restartProbability) {
		this.restartProbability = restartProbability;
		this.tsg = tsg;
	}
	
	public Triple<Map<Type, Integer>, Float, Map<Type, Moment>> allTopoCounts(
			MetaPath constraint, Integer maxIters) {
		Triple<Map<Type, Integer>, Float, Map<Type, Moment>> pathCount = 
				new Triple<Map<Type, Integer>, Float, Map<Type, Moment>>(
				new HashMap<Type, Integer>(), 0f, new HashMap<Type, Moment>());
		// start recursion Graphs.neighborListOf(tsg, start);
		MetaPath mp = new MetaPath(constraint.getStart(), constraint.getEnd());
		allTopoCounts(tsg, mp.getStart(), constraint, mp, 1f, maxIters, pathCount);

		return pathCount;
	}

	public Triple<Map<Type, Integer>, Float, Map<Type, Moment>> allTopoCounts(
			MetaPath constraint) {
		Triple<Map<Type, Integer>, Float, Map<Type, Moment>> pathCount = 
				new Triple<Map<Type, Integer>, Float, Map<Type, Moment>>(
				new HashMap<Type, Integer>(), 0f, new HashMap<Type, Moment>());
		// start recursion Graphs.neighborListOf(tsg, start);
		MetaPath mp = new MetaPath(constraint.getStart(), constraint.getEnd());
		allTopoCounts(tsg, mp.getStart(), constraint, mp, 1f, Integer.MAX_VALUE, pathCount);

		return pathCount;
	}

	private boolean allTopoCounts(TypedSimpleGraph tsg, Type start,
			MetaPath constraint, MetaPath path, float weight, int maxIters,
			Triple<Map<Type, Integer>, Float, Map<Type, Moment>> pathCount) {

		List<Type> l = Graphs.neighborListOf(tsg, start);
		float s = 0f;
		int i = 0;
		for (Type t : l) {
			if (++i > maxIters)
				continue;
			if (constraint.matches(path, t.getClass())) {
				s++;
			}

		}
		i = 0;
		for (Type t : l) {
			if (++i > maxIters)
				continue;
			if (constraint.matches(path, t.getClass())) {
				float w = weight * ((1f - restartProbability) / (float) s);
				path.addToPath(t.getClass());
				if (constraint.matchesFully(path)) {
					pathCount.v2++;
					if (pathCount.v1.containsKey(t)) {
						Moment m = pathCount.v3.get(t);
						m.add(w);
						pathCount.v3.put(t, m);

						Integer i2 = pathCount.v1.get(t);
						i2++;
						pathCount.v1.put(t, i2);
					} else {
						pathCount.v1.put(t, 1);
						Moment m = new Moment();
						m.add(w);
						pathCount.v3.put(t, m);
					}

					path.removeLast();
					continue;
				}

				allTopoCounts(tsg, t, constraint, path, w, maxIters, pathCount);
				path.removeLast();
			}

		}
		return false;
	}

	public Map<Type, Integer> pathCount(MetaPath constraint) {
		Map<Type, Integer> pathCount = new HashMap<Type, Integer>();
		// start recursion Graphs.neighborListOf(tsg, start);
		MetaPath mp = new MetaPath(constraint.getStart(), constraint.getEnd());
		pathCount(tsg, mp.getStart(), constraint, mp, pathCount);

		return pathCount;

	}

	private boolean pathCount(final TypedSimpleGraph tsg, final Type start,
			final MetaPath constraint, MetaPath path,
			Map<Type, Integer> pathCount) {

		List<Type> l = Graphs.neighborListOf(tsg, start);

		for (Type t : l) {
			if (constraint.matches(path, t.getClass())) {
				path.addToPath(t.getClass());
				if (constraint.matchesFully(path)) {
					if (pathCount.containsKey(t)) {
						Integer m = pathCount.get(t);
						m++;
						pathCount.put(t, m);
					} else {
						pathCount.put(t, 1);
					}
					path.removeLast();
					continue;
				}

				pathCount(tsg, t, constraint, path, pathCount);
				path.removeLast();
			}

		}
		return false;
	}

	public Map<Type, Moment> randomWalk(MetaPath constraint) {
		Map<Type, Moment> randomWalk = new HashMap<Type, Moment>();
		// start recursion Graphs.neighborListOf(tsg, start);
		MetaPath mp = new MetaPath(constraint.getStart());
		randomWalk(tsg, constraint.getStart(), constraint, mp, 1f, randomWalk);

		return randomWalk;

	}

	private boolean randomWalk(TypedSimpleGraph tsg, Type start,
			MetaPath constraint, MetaPath path, float weight,
			Map<Type, Moment> randomWalk) {

		List<Type> l = Graphs.neighborListOf(tsg, start);
		float s = 0f;
		for (Type t : l) {
			if (constraint.matches(path, t.getClass())) {
				s++;
			}
		}
		for (Type t : l) {
			if (constraint.matches(path, t.getClass())) {
				float w = weight * ((1f - restartProbability) / (float) s);
				path.addToPath(t.getClass());
				if (constraint.matchesFully(path)) {
					if (randomWalk.containsKey(t)) {
						Moment m = randomWalk.get(t);
						m.add(w);
						randomWalk.put(t, m);
					} else {
						Moment m = new Moment();
						m.add(w);
						randomWalk.put(t, m);
					}
					path.removeLast();
					continue;
				}

				randomWalk(tsg, t, constraint, path, w, randomWalk);
				path.removeLast();
			}

		}
		return false;
	}

	public Integer pathNormCount(MetaPath constraint) {

		// start recursion Graphs.neighborListOf(tsg, start);
		MetaPath mp = new MetaPath(constraint.getStart());
		return pathNormCount(tsg, constraint.getStart(), constraint, mp, 0);

	}

	private Integer pathNormCount(final TypedSimpleGraph tsg, final Type start,
			final MetaPath constraint, MetaPath path, Integer pathCount) {

		List<Type> l = Graphs.neighborListOf(tsg, start);

		for (Type t : l) {
			if (constraint.matches(path, t.getClass())) {

				path.addToPath(t.getClass());
				if (constraint.matchesFully(path)) {
					pathCount++;
					path.removeLast();
					continue;
				}

				pathCount = pathNormCount(tsg, t, constraint, path, pathCount);
				path.removeLast();
			}

		}
		return pathCount;
	}

}
