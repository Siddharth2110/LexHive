package com.example.LexHive.booklender.exception;

public record ApiError(int status, String error, String message) {
}
