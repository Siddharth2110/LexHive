package com.example.LexHive.booklender.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
			SecurityHttpResponses securityHttpResponses) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(securityHttpResponses)
						.accessDeniedHandler(securityHttpResponses))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
						.requestMatchers("/error").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/books").hasRole("LIBRARIAN")
						.requestMatchers(HttpMethod.PUT, "/api/books/**").hasRole("LIBRARIAN")
						.requestMatchers(HttpMethod.DELETE, "/api/books/**").hasRole("LIBRARIAN")
						.requestMatchers(HttpMethod.POST, "/api/loans", "/api/loans/*/return").hasRole("MEMBER")
						.requestMatchers("/api/members/**").hasRole("LIBRARIAN")
						.anyRequest().authenticated())
				.addFilterBefore(new JwtAuthenticationFilter(jwtService, securityHttpResponses),
						UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

}
