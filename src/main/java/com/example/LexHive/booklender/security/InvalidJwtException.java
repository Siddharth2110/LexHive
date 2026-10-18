package com.example.LexHive.booklender.security;

public class InvalidJwtException extends RuntimeException {

	public InvalidJwtException(String message) {
		super(message);
	}

}
