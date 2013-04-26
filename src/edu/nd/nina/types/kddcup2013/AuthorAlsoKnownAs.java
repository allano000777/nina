package edu.nd.nina.types.kddcup2013;

import edu.nd.nina.Type;

public class AuthorAlsoKnownAs extends Type{
	
	String name;
	
	public AuthorAlsoKnownAs(){
		this.name = null;
	}
	
	public AuthorAlsoKnownAs(String name){
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}

	@Override
	public String getUniqueIdentifier() {
		return name + "<AuthorAlsoKnownAs>";
	}

}
