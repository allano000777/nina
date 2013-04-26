package edu.nd.nina.types.kddcup2013;

import edu.nd.nina.Type;

public class Affiliation extends Type{
	
	String affiliation;
	
	public Affiliation(){
		this.affiliation = null;
	}
	
	public Affiliation(String affiliation){
		this.affiliation = affiliation;
	}
	
	@Override
	public String toString() {
		return affiliation ;
	}

	@Override
	public String getUniqueIdentifier() {
		return affiliation + "<Affiliation>";
	}

}
