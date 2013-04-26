package edu.nd.nina.types.dblp;

import edu.nd.nina.Type;

public class Author extends Type {
	
	String name;
	
	public Author(String name) {
		this.name = name;
	}
	
	public String toString(){
		return name + "<Author>";
	}

	@Override
	public String getUniqueIdentifier() {
		return name + "<Author>";
	}
}