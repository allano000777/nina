package edu.nd.nina.types.dblp;

import edu.nd.nina.Type;

public class Venue extends Type {
	
	public Venue(String venue) {
		this.name = venue;
	}

	public String getName() {
		return this.getClass().getSimpleName();
	}
	
	public String toString(){
		return name + "<" + getName() + ">";
	}
	
	@Override
	public int compareTo(Type o) {
		return name.compareTo(o.getName());
	}

}
