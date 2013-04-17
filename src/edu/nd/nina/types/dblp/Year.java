package edu.nd.nina.types.dblp;

import edu.nd.nina.Type;

public class Year extends Type {
	
	public Year(String year) {
		this.name = year;
	}
	
	public String toString(){
		return name + "<Year>";
	}

}
