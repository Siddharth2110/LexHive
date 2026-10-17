package com.example.LexHive.booklender.security;

import java.time.Instant;
import java.util.List;

public record LoginResponse(
		String accessToken,
		String tokenType,
		Instant expiresAt,
		List<String> roles,
		Long memberId
) {
}
