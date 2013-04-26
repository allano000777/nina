package edu.nd.nina.types.dblp;

import edu.nd.nina.Type;

public class Venue extends Type {
	
	private String venue;

	public Venue(String venue) {
		this.venue = venue;
	}

	public String toString(){
		return venue + "<Venue>";
	}

	@Override
	public String getUniqueIdentifier() {
		return venue + "<Venue>";
	}

}
