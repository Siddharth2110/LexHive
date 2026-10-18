package com.example.LexHive.booklender.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;
	private final SecurityHttpResponses securityHttpResponses;

	public JwtAuthenticationFilter(JwtService jwtService, SecurityHttpResponses securityHttpResponses) {
		this.jwtService = jwtService;
		this.securityHttpResponses = securityHttpResponses;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			String header = request.getHeader(HttpHeaders.AUTHORIZATION);
			if (header != null && !header.isBlank()) {
				String token = bearerToken(header);
				if (token == null) {
					securityHttpResponses.write(response, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid or expired token");
					return;
				}
				try {
					AuthenticatedUser user = jwtService.parse(token);
					UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
							user.username(),
							null,
							List.of(new SimpleGrantedAuthority(user.role().authority())));
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
				catch (InvalidJwtException ex) {
					securityHttpResponses.write(response, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid or expired token");
					return;
				}
			}
			filterChain.doFilter(request, response);
		}
		finally {
			SecurityContextHolder.clearContext();
		}
	}

	private static String bearerToken(String header) {
		if (header.length() <= BEARER_PREFIX.length()
				|| !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			return null;
		}
		String token = header.substring(BEARER_PREFIX.length()).trim();
		return token.isEmpty() ? null : token;
	}

}
