package edu.nd.nina.graph.load;

import java.util.Iterator;

import edu.nd.nina.types.Instance;

public class EmptyInstanceIterator implements Iterator<Instance> {

	public boolean hasNext() {
		return false;
	}

	public Instance next() {
		throw new IllegalStateException(
				"This iterator never has any instances.");
	}

	public void remove() {
		throw new IllegalStateException(
				"This iterator does not support remove().");
	}
}
