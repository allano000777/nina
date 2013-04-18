package edu.nd.nina.types.dblp;

import java.util.HashMap;
import java.util.Map;

import edu.nd.nina.Type;

public class Paper extends Type {
	String index;
	Integer citations;
	
	Map<String, String> attributes;
	
	public Paper(String title) {
		this.name = title;
		attributes = new HashMap<String, String>();
	}
	
	public Paper() {		
		this("");
	}
		
	public void setTitle(String title){
		this.name = title;
	}
	
	
	
	public void setIdx(String index) {
		this.index = index;
	}

	public String getIdx() {
		return index;
	}
	
	@Override
	public int compareTo(Type o) {
		if(name.compareTo(o.getName()) != 0){
			return index.compareTo(((Paper)o).index);
		}
		return 0;
	}

	public void setCitations(String citations) {
		this.citations = Integer.parseInt(citations);
	}
	
	public String toString(){
		return name + "<Paper>";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((index == null) ? 0 : index.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Paper other = (Paper) obj;
		if (index == null) {
			if (other.index != null)
				return false;
		} else if (!index.equals(other.index))
			return false;
		return true;
	}

	public void addAttribute(String name, String value) {
		attributes.put(name, value);
	}

	public String getAttribute(String name) {
		return attributes.get(name);
	}


}
