package com.codemayur.fileutils.exception;

public class StorageException extends RuntimeException {

	private static final long serialVersionUID = -9110481456178196578L;

	public StorageException(String message) {
		super(message);
	}

	public StorageException(String message,
			Throwable cause) {
		super(message,
				cause);
	}
}
