package com.example.LexHive.booklender.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class LibraryUserDetailsService implements UserDetailsService {

	private final Map<String, UserDetails> users;

	public LibraryUserDetailsService(SecurityProperties properties) {
		Map<String, UserDetails> loaded = new LinkedHashMap<>();
		for (SecurityProperties.Account account : properties.getUsers()) {
			LibraryRole role = LibraryRole.valueOf(account.getRole());
			UserDetails user = User.withUsername(account.getUsername())
					.password(account.getPassword())
					.roles(role.name())
					.build();
			if (loaded.put(account.getUsername(), user) != null) {
				throw new IllegalStateException("Duplicate security username: " + account.getUsername());
			}
		}
		this.users = Map.copyOf(loaded);
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserDetails user = users.get(username);
		if (user == null) {
			throw new UsernameNotFoundException("User not found");
		}
		return user;
	}

}
