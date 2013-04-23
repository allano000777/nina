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
/* --------------------
 * CompleteGraphDemo.java
 * --------------------
 * (C) Copyright 2003-2008, by Tim Shearouse and Contributors.
 *
 * Original Author:  Tim Shearouse
 * Contributor(s):   -
 *
 * $Id: CompleteGraphDemo.java 645 2008-09-30 19:44:48Z perfecthash $
 *
 * Changes
 * -------
 *
 */
package edu.nd.nina.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.nd.nina.Type;
import edu.nd.nina.generate.TypedNetworkGenerator;
import edu.nd.nina.graph.TypedEdge;
import edu.nd.nina.graph.TypedSimpleGraph;
import edu.nd.nina.io.Save;
import edu.nd.nina.structs.Triple;
import edu.nd.nina.types.dblp.Author;
import edu.nd.nina.types.dblp.Paper;
import edu.nd.nina.types.dblp.Venue;


public final class TypedGraphDemo
{
    //~ Static fields/initializers ---------------------------------------------

    static TypedSimpleGraph typedGraph;

    //Number of vertices
    static int size = 10;

    //~ Methods ----------------------------------------------------------------

    public static void main(String [] args)
    {

		Map<Class<? extends Type>, Integer> types = new HashMap<Class<? extends Type>, Integer>();
		List<Triple<Class<? extends Type>, Class<? extends Type>, TypedNetworkGenerator.TypeEdgeTopology>> generators = new ArrayList<Triple<Class<? extends Type>, Class<? extends Type>, TypedNetworkGenerator.TypeEdgeTopology>>();
		List<Triple<Class<? extends Type>, Class<? extends Type>, Float>> edgeProb = new ArrayList<Triple<Class<? extends Type>, Class<? extends Type>, Float>>();

        //Create the graph object; it is null at this point
    	typedGraph = new TypedSimpleGraph(TypedEdge.class);
    	
    	types.put(Author.class, 100);
    	types.put(Paper.class, 25);
    	types.put(Venue.class, 10);
    	

    	int i=0;
    	for(Class<? extends Type> type1 : types.keySet()){
    		int j=0;
    		for(Class<? extends Type> type2 : types.keySet()){
    			if(i>=j){
    				j++;
    				continue;
    			}
    			generators.add(new Triple<Class<? extends Type>, Class<? extends Type>, TypedNetworkGenerator.TypeEdgeTopology>(type1,type2, TypedNetworkGenerator.TypeEdgeTopology.scaleFree));
    			edgeProb.add(new Triple<Class<? extends Type>, Class<? extends Type>, Float>(type1,type2, 0.22f));  	
    			
    		}
    		i++;
    	}
    	
    	//Create the CompleteGraphGenerator object
        TypedNetworkGenerator<TypedEdge> typedGenerator =
            new TypedNetworkGenerator<TypedEdge>(types, generators, edgeProb, 0);
    	

        //Use the CompleteGraphGenerator object to make completeGraph a
        //complete graph with [size] number of vertices
        typedGenerator.generateGraph(typedGraph, null, null);

        //Print out the graph 
      //  PrintStatistics.PrintTypedGraphStatTable(typedGraph, "randomGraph", null);
        
        Save.saveToDot(typedGraph, "./data/dblp/typeGraph.dot");
    }
}

