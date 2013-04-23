package edu.nd.nina.graph;

import edu.nd.nina.Type;


/**
 *
 * @author Tim Weninger
 * @since April 10, 2013
 */
public class TypedEdge
{
    Type source;

    Type target;
    
    public TypedEdge(Type source, Type target){
    	this.source = source;
    	this.target = target;
    }
	
    //~ Methods ----------------------------------------------------------------

    /**
     * Retrieves the source of this edge. This is protected, for use by
     * subclasses only (e.g. for implementing toString).
     *
     * @return source of this edge
     */
    public Type getSource()
    {
        return source;
    }

    /**
     * Retrieves the target of this edge. This is protected, for use by
     * subclasses only (e.g. for implementing toString).
     *
     * @return target of this edge
     */
    public Type getTarget()
    {
        return target;
    }

    public String toString()
    {
        return "(" + source + " : " + target + ")";
    }
}

// End DefaultEdge.java
