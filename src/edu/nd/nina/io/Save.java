package edu.nd.nina.io;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import edu.nd.nina.Graph;
import edu.nd.nina.Type;
import edu.nd.nina.graph.TypedEdge;
import edu.nd.nina.graph.TypedSimpleGraph;

public class Save {
	
	enum Shape {
		box, ellipse, triangle, oval, circle, point, egg, diamond, trapezium, parallelogram
	};

	public static <V, E> void saveToDot(Graph<V, E> graph, String filename) {
		if (graph instanceof TypedSimpleGraph) {
			saveToDot((TypedSimpleGraph) graph, filename);
		}else{
			throw new NotImplementedException();
		}
	}
	
	public static <V,E> void saveToDot(TypedSimpleGraph graph, String filename){
		try {
			PrintWriter pw = new PrintWriter(new FileWriter(filename));
			saveToDot(graph, pw, filename);
			pw.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}
	}
	
	public static <V,E> void saveToDot(TypedSimpleGraph graph, PrintWriter pw, String desc){
		pw.println("graph " + desc + " {");
		Map<Type, Integer> d = new HashMap<Type, Integer>();
		Map<Class<? extends Type>, Integer> s = new HashMap<Class<? extends Type>, Integer>();
		int i=0;
		for(Type t : graph.vertexSet()){
			if(!s.containsKey(t.getClass())){
				s.put(t.getClass(), s.size());
			}
			d.put(t, i);
			pw.println(i + " [label=" + t.getName() + "];");
			pw.println(i + " [shape=" + Shape.values()[s.get(t.getClass())] + "];");
			i++;
		}
		
		for(TypedEdge e : graph.edgeSet()){
			pw.println(d.get(e.getSource()) + " -- " + d.get(e.getTarget()));
		}
		
		pw.println("}");
	}

}
