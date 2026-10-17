package com.example.LexHive.booklender.security;

import com.example.LexHive.booklender.exception.LibraryException;
import com.example.LexHive.booklender.member.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {

	private static final Map<String, UserDetails> USERS = Map.of(
			"librarian", new UserDetails("librarian", "librarian123", "LIBRARIAN"),
			"siddharth@lexhive.com", new UserDetails("siddharth@lexhive.com", "siddharth123", "MEMBER"),
			"varshney@lexhive.com", new UserDetails("varshney@lexhive.com", "varshney123", "MEMBER")
	);

	private static final String DUMMY_PASSWORD = "dummy-password-for-unknown-users";

	private final JwtEncoder jwtEncoder;
	private final MemberRepository memberRepository;
	private final Clock clock;
	private final long tokenExpiryMinutes;

	public AuthService(JwtEncoder jwtEncoder,
					   MemberRepository memberRepository,
					   Clock clock,
					   @Value("${lexhive.security.token-expiry-minutes}") long tokenExpiryMinutes) {
		this.jwtEncoder = jwtEncoder;
		this.memberRepository = memberRepository;
		this.clock = clock;
		this.tokenExpiryMinutes = tokenExpiryMinutes;
	}

	public LoginResponse login(LoginRequest request) {
		UserDetails user = USERS.get(request.username().toLowerCase(Locale.ROOT));
		if(user == null){
			// this needs to be written to avoid hacker to segregate wrong username from the right ones from response time
			passwordMatches(request.password(), DUMMY_PASSWORD);
			throw new LibraryException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Username or password is incorrect");
		}
		if (!passwordMatches(request.password(), user.getPassword())) {
			throw new LibraryException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Username or password is incorrect");
		}

		Long memberId = null;
		if (user.getRole().equals("MEMBER")) {
			memberId = memberIdOf(user.getUsername());
		}

		Instant now = clock.instant();
		Instant expiresAt = now.plus(tokenExpiryMinutes, ChronoUnit.MINUTES);
		String token = createToken(user, memberId, now, expiresAt);

		return new LoginResponse(token, "Bearer", expiresAt, List.of(user.getRole()), memberId);
	}

	// MessageDigest.isEqual takes the same time whether the first or last character differs,
	// so response times don't leak how much of the password was right.
	private static boolean passwordMatches(String typed, String expected) {
		return MessageDigest.isEqual(
				typed.getBytes(StandardCharsets.UTF_8),
				expected.getBytes(StandardCharsets.UTF_8));
	}

	private Long memberIdOf(String email) {
		return memberRepository.findIdByEmail(email)
				.orElseThrow(() -> new LibraryException(HttpStatus.FORBIDDEN, "NO_MEMBER_PROFILE",
						"Your account is not linked to a library member"));
	}

	private String createToken(UserDetails user, Long memberId, Instant issuedAt, Instant expiresAt) {
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.issuer("lexhive")
				.subject(user.getUsername())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.claim("roles", List.of(user.getRole()));
		if (memberId != null) {
			claims.claim("memberId", memberId);
		}

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
	}

}
