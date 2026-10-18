package com.example.LexHive.booklender.member;

import com.example.LexHive.booklender.exception.LibraryException;
import org.springframework.http.HttpStatus;

import java.util.Locale;

public class MemberUtils {
    public static LibraryException notFound(Long id) {
        return new LibraryException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Member " + id + " was not found");
    }

    public static LibraryException duplicateEmail() {
        return new LibraryException(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "A member with this email already exists");
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
