/* ==========================================
 * JGraphT : a free Java graph-theory library
 * ==========================================
 *
 * Project Info:  http://jgrapht.sourceforge.net/
 * Project Creator:  Barak Naveh (http://sourceforge.net/users/barak_naveh)
 *
 * (C) Copyright 2003-2008, by Barak Naveh and Contributors.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */
/* -------------------------
 * DefaultDirectedGraph.java
 * -------------------------
 * (C) Copyright 2003-2008, by Barak Naveh and Contributors.
 *
 * Original Author:  Barak Naveh
 * Contributor(s):   Christian Hammer
 *
 * $Id: DefaultDirectedGraph.java 645 2008-09-30 19:44:48Z perfecthash $
 *
 * Changes
 * -------
 * 05-Aug-2003 : Initial revision (BN);
 * 11-Mar-2004 : Made generic (CH);
 * 28-May-2006 : Moved connectivity info from edge to graph (JVS);
 *
 */
package edu.nd.nina.graph;

import edu.nd.nina.DirectedGraph;
import edu.nd.nina.EdgeFactory;
import edu.nd.nina.graph.load.Pipe;
import edu.nd.nina.types.Instance;
import edu.nd.nina.types.InstanceList;

/**
 * A directed feature graph. A default directed graph is a non-simple directed
 * graph in which multiple edges between any two vertices are <i>not</i>
 * permitted, but loops are.
 * 
 * Contains an Instance
 */
public class DirectedFeatureGraph<V extends Instance, E> extends AbstractBaseGraph<V, E>
		implements DirectedGraph<V, E> {
	// ~ Static fields/initializers
	
	InstanceList instances;
	private static final long serialVersionUID = 3544953246956466230L;


	// ~ Constructors
	// -----------------------------------------------------------

	/**
	 * Creates a new directed graph.
	 * 
	 * @param edgeClass
	 *            class on which to base factory for edges
	 */
	public DirectedFeatureGraph(Class<? extends E> edgeClass, Pipe p) {
		this(new ClassBasedEdgeFactory<V, E>(edgeClass), p);
	}

	/**
	 * Creates a new directed graph with the specified edge factory.
	 * 
	 * @param ef
	 *            the edge factory of the new graph.
	 */
	public DirectedFeatureGraph(EdgeFactory<V, E> ef, Pipe p) {
		super(ef, false, true);
		instances = new InstanceList(p);
	}

	@Override
	public boolean addVertex(V v) {
		instances.addThruPipe(v);
		return super.addVertex(v);
	}

	public InstanceList getInstances() {
		return instances;
	}
	
	

	
	/*
	 * @Override public void save(File f, String desc) throws IOException {
	 * PrintWriter pw = new PrintWriter(f); pw.printf("# Directed graph: %s \n",
	 * f.getName());
	 * 
	 * if (!desc.isEmpty()) { pw.printf("# %s\n", desc); }
	 * pw.printf("# Nodes: %d Edges: %d\n", vertexSet().size(), edgeSet()
	 * .size()); pw.printf("# FromNodeId\tToNodeId\n");
	 * 
	 * for (E e : edgeSet()) { pw.println(getEdgeSource(e) + "\t" +
	 * getEdgeTarget(e)); } pw.close(); }
	 */
}

// End DefaultDirectedGraph.java
