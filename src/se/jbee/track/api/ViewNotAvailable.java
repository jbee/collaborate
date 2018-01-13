package se.jbee.track.api;

import java.util.NoSuchElementException;

public final class ViewNotAvailable extends NoSuchElementException {

	public final Params request;
	public final Class<? extends View> response;

	public ViewNotAvailable(Params request, Class<? extends View> response) {
		this.request = request;
		this.response = response;
	}
	
	@Override
	public String toString() {
		return response.getSimpleName()+" from "+request;
	}
}
