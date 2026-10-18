package com.example.LexHive.booklender.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "lexhive.security")
public class SecurityProperties {

	@Valid
	@NotNull
	private Jwt jwt = new Jwt();

	@NotEmpty
	private List<@Valid Account> users = new ArrayList<>();

	public Jwt getJwt() {
		return jwt;
	}

	public void setJwt(Jwt jwt) {
		this.jwt = jwt;
	}

	public List<Account> getUsers() {
		return users;
	}

	public void setUsers(List<Account> users) {
		this.users = users;
	}

	public static class Jwt {

		@NotBlank
		private String secret;

		@NotNull
		private Duration expiration = Duration.ofHours(1);

		public String getSecret() {
			return secret;
		}

		public void setSecret(String secret) {
			this.secret = secret;
		}

		public Duration getExpiration() {
			return expiration;
		}

		public void setExpiration(Duration expiration) {
			this.expiration = expiration;
		}

	}

	public static class Account {

		@NotBlank
		private String username;

		@NotBlank
		private String password;

		@NotBlank
		@Pattern(regexp = "MEMBER|LIBRARIAN")
		private String role;

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getRole() {
			return role;
		}

		public void setRole(String role) {
			this.role = role;
		}

	}

}
