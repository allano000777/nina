package edu.nd.nina;


/**
 * 
 * @author Tim Weninger
 * @since April 10, 2013
 */
public abstract class Type implements Comparable<Type> {
	protected String name;
	
	public Type(){
		name = "";
	}

	public final String getName() {
		return name;
	};
	
	public abstract String toString();
	
	@Override
	public int compareTo(Type o) {
		return toString().compareTo(o.toString());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((toString() == null) ? 0 : toString().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Type other = (Type) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
