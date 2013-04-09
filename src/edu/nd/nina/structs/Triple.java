package edu.nd.nina.structs;

public class Triple<S1 extends Comparable<S1>, S2 extends Comparable<S2>, S3 extends Comparable<S3>> implements
Comparable<Triple<S1, S2, S3>> {

	
	
	public S1 v1;
	public S2 v2;
	public S3 v3;

	public Triple(S1 i, S2 j, S3 k) {
		v1 = i;
		v2 = j;
		v3 = k;
	}
	
	

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Triple){
			Triple<?,?,?> x = (Triple<?,?,?>) obj;
			if( x.v1.equals(v1) && x.v2.equals(v2)  && x.v3.equals(v3)){
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "<" + v1 + ", " + v2 + "," + v3 + ">";
	}
	
	@Override
	public int compareTo(Triple<S1, S2, S3> o) {
		if(o.v1.compareTo(this.v1) == 0){
			if(o.v2.compareTo(this.v2) == 0){
				return o.v3.compareTo(this.v3);
			}else{
				return o.v2.compareTo(this.v2);
			}
		}else{
			return o.v1.compareTo(this.v1);
		}
	}

}
