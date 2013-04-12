package edu.nd.nina;

/**
 * 
 * @author Tim Weninger
 * @since April 10, 2013
 */
public abstract class Type implements Comparable<Type> {
	protected String name;

	public String getName() {
		return name;
	};
}
