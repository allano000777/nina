package edu.nd.nina.alg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.nd.nina.Graphs;
import edu.nd.nina.Type;
import edu.nd.nina.graph.TypedSimpleGraph;
import edu.nd.nina.math.Moment;

public class ConstrainedRandomWalkWithRestart {

	float restartProbability;
	TypedSimpleGraph tsg;

	public ConstrainedRandomWalkWithRestart(TypedSimpleGraph tsg,
			float restartProbability) {
		this.restartProbability = restartProbability;
		this.tsg = tsg;
	}

	public Map<Type, Integer> pathCount(Type start, MetaPath constraint) {
		Map<Type, Integer> pathCount = new HashMap<Type, Integer>();
		// start recursion Graphs.neighborListOf(tsg, start);
		MetaPath mp = new MetaPath(start);
		pathCount(tsg, start, constraint, mp, pathCount);

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

	public Map<Type, Moment> randomWalk(Type start, MetaPath constraint) {
		Map<Type, Moment> randomWalk = new HashMap<Type, Moment>();
		// start recursion Graphs.neighborListOf(tsg, start);
		MetaPath mp = new MetaPath(start);
		randomWalk(tsg, start, constraint, mp, 1f, randomWalk);

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

	public Integer pathNormCount(Type start, MetaPath constraint) {

		// start recursion Graphs.neighborListOf(tsg, start);
		MetaPath mp = new MetaPath(start);
		return pathNormCount(tsg, start, constraint, mp, 0);

	}
	
	private Integer pathNormCount(final TypedSimpleGraph tsg, final Type start,
			final MetaPath constraint, MetaPath path, Integer pathCount) {

		List<Type> l = Graphs.neighborListOf(tsg, start);

		for (Type t : l) {
			if (constraint.matches(path, t.getClass())) {
				
				path.addToPath(t.getClass());
				if (constraint.matchesFully(path)) {
					if (t.equals(constraint.getStart())) {
						pathCount++;
					}
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
