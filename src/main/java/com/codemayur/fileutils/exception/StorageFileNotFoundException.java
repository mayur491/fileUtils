package com.codemayur.fileutils.exception;

public class StorageFileNotFoundException extends StorageException {

	private static final long serialVersionUID = 5043630458755091216L;

	public StorageFileNotFoundException(String message) {
		super(message);
	}

	public StorageFileNotFoundException(String message,
			Throwable cause) {
		super(message,
				cause);
	}
}
