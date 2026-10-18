package com.example.LexHive.booklender.security;

public record AuthenticatedUser(String username, LibraryRole role) {
}
