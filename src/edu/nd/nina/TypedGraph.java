package edu.nd.nina;

import edu.nd.nina.graph.TypedEdge;

/**
 *
 * @author Tim Weninger
 * @since April 10, 2013
 */
public interface TypedGraph<E extends TypedEdge>
    extends Graph<Type, E>
{

}

