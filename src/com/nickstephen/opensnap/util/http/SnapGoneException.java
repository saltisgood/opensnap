package com.nickstephen.opensnap.util.http;

import com.nickstephen.lib.http.NetException;


public class SnapGoneException extends NetException {

	/**
	 * What the hell is this for?
	 */
	private static final long serialVersionUID = -4477050116536084840L;

	public SnapGoneException() {
		super(410);
	}

	public SnapGoneException(String detailMessage) {
		super(detailMessage, 410);
	}

	public SnapGoneException(Throwable throwable) {
		super(throwable, 410);
	}

	public SnapGoneException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable, 410);
	}

}
