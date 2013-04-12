package edu.nd.nina;

import java.util.List;

import edu.nd.nina.graph.TypedEdge;

/**
 *
 * @author Tim Weninger
 * @since April 10, 2013
 */
public interface TypedGraph<E extends TypedEdge<Type, Type>>
    extends Graph<Type, E>
{
	public List<Type> getTypes();
	
}

