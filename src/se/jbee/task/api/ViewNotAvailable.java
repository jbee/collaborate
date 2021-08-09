package se.jbee.task.api;

import java.util.NoSuchElementException;

public final class ViewNotAvailable extends NoSuchElementException {

	public final Params request;
	public final Class<? extends View> response;

	public ViewNotAvailable(Params request, Class<? extends View> response) {
		super(response.getSimpleName()+" from "+request);
		this.request = request;
		this.response = response;
	}

}
