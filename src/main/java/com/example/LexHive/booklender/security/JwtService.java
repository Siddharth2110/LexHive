package com.example.LexHive.booklender.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

	private final Clock clock;
	private final Duration expiration;
	private final MACSigner signer;
	private final MACVerifier verifier;

	public JwtService(SecurityProperties properties, Clock clock) {
		this.clock = clock;
		Duration configuredExpiration = properties.getJwt().getExpiration();
		if (configuredExpiration == null || configuredExpiration.isZero() || configuredExpiration.isNegative()) {
			throw new IllegalStateException("JWT expiration must be positive");
		}
		this.expiration = configuredExpiration;
		byte[] secret = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
		try {
			this.signer = new MACSigner(secret);
			this.verifier = new MACVerifier(secret);
		}
		catch (JOSEException ex) {
			throw new IllegalStateException("JWT signing key must be at least 32 bytes", ex);
		}
	}

	public long getExpiresInSeconds() {
		return expiration.toSeconds();
	}

	public String generateToken(String username, LibraryRole role) {
		Instant issuedAt = clock.instant();
		return generateToken(username, role, issuedAt, issuedAt.plus(expiration));
	}

	/**
	 * boiler plate code, standard implementation
	 */
	public String generateToken(String username, LibraryRole role, Instant issuedAt, Instant expiresAt) {
		if (username == null || username.isBlank()) {
			throw new IllegalArgumentException("Username is required");
		}
		if (role == null) {
			throw new IllegalArgumentException("Role is required");
		}
		if (issuedAt == null || expiresAt == null || !expiresAt.isAfter(issuedAt)) {
			throw new IllegalArgumentException("Token expiration must be after the issue time");
		}
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(username)
				.claim("role", role.name())
				.issueTime(Date.from(issuedAt))
				.expirationTime(Date.from(expiresAt))
				.build();
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
		try {
			jwt.sign(signer);
		}
		catch (JOSEException ex) {
			throw new IllegalStateException("Could not sign token", ex);
		}
		return jwt.serialize();
	}

	public AuthenticatedUser parse(String token) {
		try {
			SignedJWT jwt = SignedJWT.parse(token);
			if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) {
				throw new InvalidJwtException("Unexpected signing algorithm");
			}
			if (!jwt.verify(verifier)) {
				throw new InvalidJwtException("Invalid token signature");
			}
			JWTClaimsSet claims = jwt.getJWTClaimsSet();
			String username = claims.getSubject();
			Date issuedAt = claims.getIssueTime();
			Date expiresAt = claims.getExpirationTime();
			if (username == null || username.isBlank() || issuedAt == null || expiresAt == null) {
				throw new InvalidJwtException("Token is missing required claims");
			}
			if (!expiresAt.toInstant().isAfter(clock.instant())) {
				throw new InvalidJwtException("Token is expired");
			}
			LibraryRole role = LibraryRole.fromClaim(claims.getStringClaim("role"));
			return new AuthenticatedUser(username, role);
		}
		catch (InvalidJwtException ex) {
			throw ex;
		}
		catch (ParseException | JOSEException ex) {
			throw new InvalidJwtException("Token could not be validated");
		}
	}

}
