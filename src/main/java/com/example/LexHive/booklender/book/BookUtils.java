package com.example.LexHive.booklender.book;

import com.example.LexHive.booklender.exception.LibraryException;
import org.springframework.http.HttpStatus;

import java.util.Locale;

public class BookUtils {
    public static LibraryException notFound(Long id) {
        return new LibraryException(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Book " + id + " was not found");
    }

    public static LibraryException duplicateIsbn() {
        return new LibraryException(HttpStatus.CONFLICT, "DUPLICATE_ISBN", "A book with this ISBN already exists");
    }

    public static String normalizeIsbn(String isbn) {
        return isbn.trim().toUpperCase(Locale.ROOT);
    }
}
