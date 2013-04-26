package edu.nd.nina.types.dblp;

import edu.nd.nina.Type;

public class Year extends Type {
	
	private Integer year;

	public Year(Integer year) {
		this.year = year;
	}
	
	@Override
	public String toString(){
		return year + "<Year>";
	}

	@Override
	public String getUniqueIdentifier() {
		return year + "<Year>";
	}

}
