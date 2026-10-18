package com.example.LexHive.booklender.security;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
