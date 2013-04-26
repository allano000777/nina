package edu.nd.nina.types.kddcup2013;

import edu.nd.nina.Type;

public class Paper extends Type{
	
	Integer id;
	String title;
	String keywords;
	
	public Paper(){
		this.id = null;
		this.title = null;
		this.keywords = null;
	}
	
	public Paper(Integer id, String title, String keywords){
		this.id = id;
		this.title = title;
		this.keywords = keywords;
	}
	
	@Override
	public String toString() {
		return title + " (" + id + ")";
	}

	@Override
	public String getUniqueIdentifier() {
		return String.valueOf(id)  + "<Paper>";
	}

}
