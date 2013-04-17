package edu.nd.nina.graph;

import java.util.HashSet;
import java.util.Set;

import edu.nd.nina.EdgeFactory;
import edu.nd.nina.Type;
import edu.nd.nina.TypedGraph;
import edu.nd.nina.UndirectedGraph;


public class TypedSimpleGraph
    extends AbstractBaseGraph<Type, TypedEdge>
    implements UndirectedGraph<Type, TypedEdge>, TypedGraph<TypedEdge>
{
    //~ Static fields/initializers ---------------------------------------------

    private static final long serialVersionUID = 3545796589454112304L;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new simple graph with the specified edge factory.
     *
     * @param ef the edge factory of the new graph.
     */
    public TypedSimpleGraph(EdgeFactory<Type, TypedEdge> ef)
    {
        super(ef, false, false);
    }

    /**
     * Creates a new simple graph.
     *
     * @param edgeClass class on which to base factory for edges
     */
    public TypedSimpleGraph(Class<? extends TypedEdge> edgeClass)
    {
        this(new ClassBasedEdgeFactory<Type, TypedEdge>(edgeClass));
    }

	public int inDegreeOf(Type vertex, Class<? extends Type> clazz) {
		int cnt = 0;
		for(TypedEdge e : this.incomingEdgesOf(vertex)){
			if(e.getSource().getClass().equals(clazz)){
				cnt++;
			}
		}
		return cnt;
	}
	
	public Set<Type> incomingEdgesOf(Type vertex, Class<? extends Type> clazz) {
		Set<Type> ret = new HashSet<Type>();
		for(TypedEdge e : this.incomingEdgesOf(vertex)){
			if(e.getSource().getClass().equals(clazz)){
				ret.add(e.getSource());
			}
		}
		return ret;
	}
	
	public Set<Type> outgoingEdgesOf(Type vertex, Class<? extends Type> clazz) {
		Set<Type> ret = new HashSet<Type>();
		for(TypedEdge e : this.outgoingEdgesOf(vertex)){
			if(e.getTarget().getClass().equals(clazz)){
				ret.add(e.getSource());
			}
		}
		return ret;
	}
    
}

// End SimpleGraph.java
