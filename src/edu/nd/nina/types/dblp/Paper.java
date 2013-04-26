package edu.nd.nina.types.dblp;

import java.util.HashMap;
import java.util.Map;

import edu.nd.nina.Type;

public class Paper extends Type {
	Integer index;
	String name;
	Integer citations;
	
	Map<String, String> attributes;
	
	public Paper(String title) {
		this.name = title;
		this.index = -1;
		attributes = new HashMap<String, String>();
	}

		
	public void setTitle(String title){
		this.name = title;
	}

	public void setIdx(Integer index) {
		this.index = index;
	}

	public Integer getIdx() {
		return index;
	}
	
	public void setCitations(String citations) {
		this.citations = Integer.parseInt(citations);
	}
	
	public String toString(){
		return name + "<Paper>" + index;
	}

	public void addAttribute(String name, String value) {
		attributes.put(name, value);
	}

	public String getAttribute(String name) {
		return attributes.get(name);
	}


	@Override
	public String getUniqueIdentifier() {
		return name + "<Paper>" + index;
	}


}
