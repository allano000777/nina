package edu.nd.nina.types.dblp;

import edu.nd.nina.Type;

public class Term extends Type {
	
	String name;
	
	public Term(String term) {
		this.name = term;
	}
	
	public String toString(){
		return name + "<Term>";
	}

	@Override
	public String getUniqueIdentifier() {
		return name + "<Term>";
	}

}
