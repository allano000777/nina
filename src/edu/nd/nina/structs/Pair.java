package edu.nd.nina.structs;

public class Pair<T extends Comparable<T>, V extends Comparable<V>> implements
		Comparable<Pair<T, V>> {
	public T p1;
	public V p2;

	public Pair(T p, V r) {
		p1 = p;
		p2 = r;
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Pair) {
			Pair<?, ?> x = (Pair<?, ?>) obj;
			if (x.p1.equals(p1) && x.p2.equals(p2)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "<" + p1 + ", " + p2 + ">";
	}

	@Override
	public int compareTo(Pair<T, V> o) {
		if (this.p1.compareTo(o.p1) == 0) {
			return this.p2.compareTo(o.p2);
		} else {
			return this.p1.compareTo(o.p1);
		}
	}

}
