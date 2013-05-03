package edu.nd.nina.alg;

import java.util.ArrayList;

import edu.nd.nina.Type;

public class MetaPath {
	private Type start = null;
	private ArrayList<Class<? extends Type>> metaPath = null;
	
	public MetaPath(Type start){
		this.start = start;
		metaPath = new ArrayList<Class<? extends Type>>();
	}
	
	public void addToPath(Class<? extends Type> claz){
		metaPath.add(claz);
	}
	
	public Class<? extends Type> get(int i){
		return metaPath.get(i);
	}
	
	public Type getStart(){
		return start;
	}
	
	public boolean matches(MetaPath mp, Class<? extends Type> n) {
		MetaPath path = new MetaPath(mp.start);
		path.metaPath = (ArrayList)mp.metaPath.clone();
		path.addToPath(n);
		
		if(!start.equals(path.start)){
			return false;
		}
		
		for(int i=0, j=0; j<metaPath.size(); i++, j++){
			if(path.metaPath.size() <= i){
				break;
			}
			Class<? extends Type> x = path.get(i);
			Class<? extends Type> z = metaPath.get(j);
			if(x.isAssignableFrom(z)){
				 //good
			}else{
				return false; //doesn't match
			}
		}
		return true;
	}

	public boolean matchesFully(MetaPath path) {
		if(!start.equals(path.start)){
			return false;
		}
		
		int j=0;
		for(int i=0; j<metaPath.size(); i++, j++){
			if(path.metaPath.size() <= i){
				break;
			}
			Class<? extends Type> x = path.get(i);
			Class<? extends Type> z = metaPath.get(j);
			if(x.isAssignableFrom(z)){
				 //good
			}else{
				return false; //doesn't match
			}
		}
		if(j==metaPath.size()){
			return true;
		}
		return false;
	}

	public int size() {
		return metaPath.size();
	}

	public void removeLast() {
		metaPath.remove(metaPath.size()-1);
	}

	public void setStart(Type start) {
		this.start = start;
	}
	
}
