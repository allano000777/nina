package edu.nd.nina.types.kddcup2013;

import edu.nd.nina.Type;

public class Venue extends Type{
	
	Integer id;
	String acronym;
	String name;
	
	public Venue(){
		this.id = null;
		this.acronym = null;
		this.name = null;
	}
	
	
	public Venue(Integer id, String acronym, String name){
		this.id = id;
		this.acronym = acronym;
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name + " (" + id + ")";
	}

	@Override
	public String getUniqueIdentifier() {
		return String.valueOf(id) + "<Venue>";
	}

}
