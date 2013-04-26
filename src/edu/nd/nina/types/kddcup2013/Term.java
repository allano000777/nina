package edu.nd.nina.types.kddcup2013;

import edu.nd.nina.Type;

public class Term extends Type{
	
	String term;
	
	public Term(){
		this.term = null;
	}
	
	public Term(String term){
		this.term = term;
	}
	
	@Override
	public String toString() {
		return term;
	}

	@Override
	public String getUniqueIdentifier() {
		return String.valueOf(term)  + "<Term>";
	}

}
