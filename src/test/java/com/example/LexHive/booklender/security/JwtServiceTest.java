package com.example.LexHive.booklender.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

	private static final String SECRET = "dev-only-lexhive-jwt-signing-key-32";
	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

	@Test
	void issuedTokenCarriesUsernameAndRole() {
		JwtService jwtService = jwtService(Clock.fixed(NOW, ZoneOffset.UTC));

		String token = jwtService.generateToken("alice", LibraryRole.MEMBER);
		AuthenticatedUser user = jwtService.parse(token);

		assertEquals("alice", user.username());
		assertEquals(LibraryRole.MEMBER, user.role());
		assertEquals(3600, jwtService.getExpiresInSeconds());
	}

	@Test
	void rejectsExpiredToken() {
		JwtService jwtService = jwtService(Clock.fixed(NOW, ZoneOffset.UTC));
		String token = jwtService.generateToken(
				"alice",
				LibraryRole.MEMBER,
				NOW.minus(Duration.ofHours(2)),
				NOW.minus(Duration.ofHours(1)));

		assertThrows(InvalidJwtException.class, () -> jwtService.parse(token));
	}

	@Test
	void rejectsMalformedToken() {
		JwtService jwtService = jwtService(Clock.fixed(NOW, ZoneOffset.UTC));

		assertThrows(InvalidJwtException.class, () -> jwtService.parse("not-a-token"));
	}

	@Test
	void rejectsTokenSignedWithAnotherKey() {
		JwtService jwtService = jwtService(Clock.fixed(NOW, ZoneOffset.UTC));
		JwtService otherKey = jwtService(Clock.fixed(NOW, ZoneOffset.UTC), "another-dev-only-signing-key-32b");

		String token = otherKey.generateToken("alice", LibraryRole.MEMBER);

		assertThrows(InvalidJwtException.class, () -> jwtService.parse(token));
	}

	@Test
	void rejectsTamperedToken() {
		JwtService jwtService = jwtService(Clock.fixed(NOW, ZoneOffset.UTC));
		String token = jwtService.generateToken("alice", LibraryRole.MEMBER);
		String tampered = token.substring(0, token.length() - 2) + "aa";

		assertThrows(InvalidJwtException.class, () -> jwtService.parse(tampered));
	}

	@Test
	void rejectsTokenWhoseRoleWasNotIssuedByTheServer() throws Exception {
		JwtService jwtService = jwtService(Clock.fixed(NOW, ZoneOffset.UTC));
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject("alice")
				.claim("role", "ADMIN")
				.issueTime(Date.from(NOW))
				.expirationTime(Date.from(NOW.plus(Duration.ofHours(1))))
				.build();
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
		jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));

		assertThrows(InvalidJwtException.class, () -> jwtService.parse(jwt.serialize()));
	}

	private static JwtService jwtService(Clock clock) {
		return jwtService(clock, SECRET);
	}

	private static JwtService jwtService(Clock clock, String secret) {
		SecurityProperties properties = new SecurityProperties();
		SecurityProperties.Jwt jwt = new SecurityProperties.Jwt();
		jwt.setSecret(secret);
		jwt.setExpiration(Duration.ofHours(1));
		properties.setJwt(jwt);
		return new JwtService(properties, clock);
	}

}
