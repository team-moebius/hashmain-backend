package com.moebius.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DataNotFoundException extends ResponseStatusException {
	public DataNotFoundException(String message) {
		super(HttpStatus.NOT_FOUND, message);
	}
}
