package edu.nd.nina.types.kddcup2013;

import edu.nd.nina.Type;

public class Author extends Type {
	
	Integer id;
	String name;
	
	public Author(){
		this.name = null;
		this.id = null;
	}
	
	public Author(Integer id, String name){
		this.id = id;
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name + " (" + id + ")";
	}

	@Override
	public String getUniqueIdentifier() {
		return String.valueOf(id) + "<Author>";
	}

}
