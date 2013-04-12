package edu.nd.nina.graph;

import edu.nd.nina.EdgeFactory;
import edu.nd.nina.Type;
import edu.nd.nina.UndirectedGraph;


public class TypedSimpleGraph
    extends AbstractBaseGraph<Type, TypedEdge<Type, Type>>
    implements UndirectedGraph<Type, TypedEdge<Type, Type>>
{
    //~ Static fields/initializers ---------------------------------------------

    private static final long serialVersionUID = 3545796589454112304L;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new simple graph with the specified edge factory.
     *
     * @param ef the edge factory of the new graph.
     */
    public TypedSimpleGraph(EdgeFactory<Type, TypedEdge<Type, Type>> ef)
    {
        super(ef, false, false);
    }

    /**
     * Creates a new simple graph.
     *
     * @param edgeClass class on which to base factory for edges
     */
    public TypedSimpleGraph(Class<? extends TypedEdge<Type, Type>> edgeClass)
    {
        this(new ClassBasedEdgeFactory<Type, TypedEdge<Type, Type>>(edgeClass));
    }


}

// End SimpleGraph.java
