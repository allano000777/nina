package edu.nd.nina.alg;

import java.util.HashMap;
import java.util.Map;

import edu.nd.nina.Type;
import edu.nd.nina.graph.TypedSimpleGraph;

public class MetaPathClas {
	TypedSimpleGraph tsg = null;
	Map<Type, Integer> pathNormCount;

	public MetaPathClas(TypedSimpleGraph tsg) {
		this.tsg = tsg;
		this.pathNormCount = new HashMap<Type, Integer>();
	}

}
