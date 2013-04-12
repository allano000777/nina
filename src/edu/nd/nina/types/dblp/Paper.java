package edu.nd.nina.types.dblp;

import edu.nd.nina.Type;

public class Paper extends Type {
	String index;
	Integer citations;
	
	public Paper(String title) {
		this.name = title;
	}

	public String getName() {
		return this.getClass().getSimpleName();
	}

	public void setIdx(String index) {
		this.index = index;
	}

	public String getIdx() {
		return index;
	}

	public void setCitations(String citations) {
		this.citations = Integer.parseInt(citations);
	}
	
	public String toString(){
		return name + "<" + getName() + ">";
	}

	@Override
	public int compareTo(Type o) {
		return name.compareTo(o.getName());
	}


}
