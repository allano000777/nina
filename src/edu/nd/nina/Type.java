package edu.nd.nina;

/**
 * 
 * @author Tim Weninger
 * @since April 10, 2013
 */
public abstract class Type implements Comparable<Type> {

	public abstract String getUniqueIdentifier();

	public abstract String toString();

	protected Type() {
	}

	@Override
	public final int compareTo(Type o) {
		return getUniqueIdentifier().compareTo(o.getUniqueIdentifier());
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((getUniqueIdentifier() == null) ? 0 : getUniqueIdentifier()
						.hashCode());
		return result;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Type other = (Type) obj;
		if (getUniqueIdentifier() == null) {
			if (other.getUniqueIdentifier() != null)
				return false;
		} else if (!getUniqueIdentifier().equals(other.getUniqueIdentifier()))
			return false;
		return true;
	}
}
