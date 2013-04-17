package edu.nd.nina.types.dblp;

import edu.nd.nina.Type;

public class Paper extends Type {
	String index;
	Integer citations;
	
	public Paper(String title) {
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


}
