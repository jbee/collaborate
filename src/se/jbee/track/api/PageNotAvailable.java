package se.jbee.track.api;

import java.util.NoSuchElementException;

public final class PageNotAvailable extends NoSuchElementException {

	public final Params request;
	public final Class<? extends Page> response;

	public PageNotAvailable(Params request, Class<? extends Page> response) {
		this.request = request;
		this.response = response;
	}
	
	@Override
	public String toString() {
		return response.getSimpleName()+" from "+request;
	}
}
