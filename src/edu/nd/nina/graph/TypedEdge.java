package edu.nd.nina.graph;

import edu.nd.nina.Type;


/**
 *
 * @author Tim Weninger
 * @since April 10, 2013
 */
public class TypedEdge<T1 extends Type, T2 extends Type>
{
    T1 source;

    T2 target;
	
    //~ Methods ----------------------------------------------------------------

    /**
     * Retrieves the source of this edge. This is protected, for use by
     * subclasses only (e.g. for implementing toString).
     *
     * @return source of this edge
     */
    protected T1 getSource()
    {
        return source;
    }

    /**
     * Retrieves the target of this edge. This is protected, for use by
     * subclasses only (e.g. for implementing toString).
     *
     * @return target of this edge
     */
    protected T2 getTarget()
    {
        return target;
    }

    public String toString()
    {
        return "(" + source + " : " + target + ")";
    }
}

// End DefaultEdge.java
