package com.moebius.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DuplicateDataException extends ResponseStatusException {
	public DuplicateDataException(String message) {
		super(HttpStatus.BAD_REQUEST, message);
	}
}
