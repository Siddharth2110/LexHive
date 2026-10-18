package com.example.LexHive.booklender.security;

import com.example.LexHive.booklender.exception.LibraryException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private static final String DUMMY_PASSWORD_HASH =
			"$2a$10$oSHkb7Vi7E8X.a4tpF4pVeXR0RpcUiSm.DzvaGsGffJnpdb4WTL8K";

	private final LibraryUserDetailsService userDetailsService;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(
			LibraryUserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder,
			JwtService jwtService) {
		this.userDetailsService = userDetailsService;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public LoginResponse login(LoginRequest request) {
		UserDetails user;
		try {
			user = userDetailsService.loadUserByUsername(request.username());
		}
		catch (UsernameNotFoundException ex) {
			passwordEncoder.matches(request.password(), DUMMY_PASSWORD_HASH);
			throw invalidCredentials();
		}
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw invalidCredentials();
		}
		String authority = user.getAuthorities().iterator().next().getAuthority();
		LibraryRole role = LibraryRole.valueOf(authority.replace("ROLE_", ""));
		String accessToken = jwtService.generateToken(user.getUsername(), role);
		return new LoginResponse(accessToken, "Bearer", jwtService.getExpiresInSeconds());
	}

	private static LibraryException invalidCredentials() {
		return new LibraryException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username or password");
	}

}
