package edu.nd.nina.types.dblp;

import edu.nd.nina.Type;

public class Venue extends Type {
	
	public Venue(String venue) {
		this.name = venue;
	}

	public String toString(){
		return name + "<Venue>";
	}

}
