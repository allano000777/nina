package edu.nd.nina.types.dblp;

import edu.nd.nina.Type;

public class Year extends Type {
	
	public Year(String year) {
		this.name = year;
	}

	public String getName() {
		return "Year";
	}
	
	public String toString(){
		return name + "<" + getName() + ">";
	}

	@Override
	public int compareTo(Type o) {
		return name.compareTo(o.getName());
	}
	
	

}
