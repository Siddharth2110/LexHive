package com.example.LexHive.booklender.security;

import com.example.LexHive.booklender.exception.LibraryException;
import com.example.LexHive.booklender.member.MemberCredentials;
import com.example.LexHive.booklender.member.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

	private static final String LIBRARIAN_USERNAME = "librarian";
	private static final String LIBRARIAN_PASSWORD = "librarian123";
	private static final String DUMMY_PASSWORD = "dummy-password-for-unknown-users";

	private final JwtEncoder jwtEncoder;
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;
	private final long tokenExpiryMinutes;
	private final String librarianPasswordHash;
	private final String dummyPasswordHash;

	public AuthService(JwtEncoder jwtEncoder,
					   MemberRepository memberRepository,
					   PasswordEncoder passwordEncoder,
					   Clock clock,
					   @Value("${lexhive.security.token-expiry-minutes}") long tokenExpiryMinutes) {
		this.jwtEncoder = jwtEncoder;
		this.memberRepository = memberRepository;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
		this.tokenExpiryMinutes = tokenExpiryMinutes;
		this.librarianPasswordHash = passwordEncoder.encode(LIBRARIAN_PASSWORD);
		this.dummyPasswordHash = passwordEncoder.encode(DUMMY_PASSWORD);
	}

	public LoginResponse login(LoginRequest request) {
		String username = request.username().trim().toLowerCase(Locale.ROOT);

		if (username.equals(LIBRARIAN_USERNAME)) {
			if (!passwordEncoder.matches(request.password(), librarianPasswordHash)) {
				throw invalidCredentials();
			}
			return issueToken(username, "LIBRARIAN", null);
		}

		Optional<MemberCredentials> credentials = memberRepository.findCredentialsByEmail(username);
		if (credentials.isEmpty()) {
			// Run a hash check anyway so unknown usernames take as long as wrong passwords
			// and can't be told apart by response time.
			passwordEncoder.matches(request.password(), dummyPasswordHash);
			throw invalidCredentials();
		}
		if (!passwordEncoder.matches(request.password(), credentials.get().passwordHash())) {
			throw invalidCredentials();
		}
		return issueToken(username, "MEMBER", credentials.get().memberId());
	}

	private LoginResponse issueToken(String username, String role, Long memberId) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(tokenExpiryMinutes, ChronoUnit.MINUTES);
		String token = createToken(username, role, memberId, now, expiresAt);
		return new LoginResponse(token, "Bearer", expiresAt, List.of(role), memberId);
	}

	private static LibraryException invalidCredentials() {
		return new LibraryException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Username or password is incorrect");
	}

	private String createToken(String username, String role, Long memberId, Instant issuedAt, Instant expiresAt) {
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.issuer("lexhive")
				.subject(username)
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.claim("roles", List.of(role));
		if (memberId != null) {
			claims.claim("memberId", memberId);
		}

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
	}

}
