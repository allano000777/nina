package edu.nd.nina.types.kddcup2013;

import java.net.URI;

import edu.nd.nina.Type;

public class VenueWebPage extends Type{
	
	URI url;	
	
	public VenueWebPage(){
		this.url = null;
	}
	
	public VenueWebPage(URI url){
		this.url = url;
	}
	
	@Override
	public String toString() {
		return url.toASCIIString();
	}

	@Override
	public String getUniqueIdentifier() {
		return String.valueOf(url)  + "<VenueWebPage>";
	}

}
