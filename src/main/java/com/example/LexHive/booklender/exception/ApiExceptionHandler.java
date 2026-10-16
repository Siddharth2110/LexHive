package com.example.LexHive.booklender.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(LibraryException.class)
	public ResponseEntity<ApiError> handleLibrary(LibraryException exception) {
		if (exception.getStatus().is5xxServerError()) {
			log.error("Library failure {}: {}", exception.getCode(), exception.getMessage(), exception);
		}
		return error(exception.getStatus(), exception.getCode(), exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
				.collect(Collectors.joining(", "));
		if (message.isBlank()) {
			message = "Request validation failed";
		}
		return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException exception) {
		return error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is missing or is not valid JSON");
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
		return error(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "Parameter '" + exception.getName() + "' is invalid");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException exception) {
		String detail = exception.getMostSpecificCause().getMessage();
		if (detail != null && detail.contains("uq_books_isbn")) {
			return error(HttpStatus.CONFLICT, "DUPLICATE_ISBN", "A book with this ISBN already exists");
		}
		if (detail != null && detail.contains("uq_members_email")) {
			return error(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "A member with this email already exists");
		}
		if (detail != null && (detail.contains("fk_loans_book") || detail.contains("fk_loans_member"))) {
			return error(HttpStatus.CONFLICT, "IN_USE", "The record is referenced by a loan and cannot be removed");
		}
		log.warn("Unhandled data integrity violation", exception);
		return error(HttpStatus.CONFLICT, "DATA_CONFLICT", "The request conflicts with existing data");
	}

	private static ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
		return ResponseEntity.status(status).body(new ApiError(status.value(), code, message));
	}

}
