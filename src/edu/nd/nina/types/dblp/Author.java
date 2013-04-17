package edu.nd.nina.types.dblp;

import edu.nd.nina.Type;

public class Author extends Type {
	
	public Author(String name) {
		this.name = name;
	}
	
	public String toString(){
		return name + "<Author>";
	}
}