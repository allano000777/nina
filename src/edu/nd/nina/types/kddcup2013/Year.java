package edu.nd.nina.types.kddcup2013;

import edu.nd.nina.Type;

public class Year extends Type{
	
	Integer year;
	
	public Year(){
		this.year = null;
	}
	
	public Year(Integer year){
		this.year = year;
	}
	
	@Override
	public String toString() {
		return String.valueOf(year);
	}

	@Override
	public String getUniqueIdentifier() {
		return String.valueOf(year) + "<Year>"; 
	}

}
