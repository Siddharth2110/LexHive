package com.example.LexHive.booklender.security;

public enum LibraryRole {

	MEMBER,
	LIBRARIAN;

	public String authority() {
		return "ROLE_" + name();
	}

	public static LibraryRole fromClaim(String claim) {
		if (claim == null || claim.isBlank()) {
			throw new InvalidJwtException("Token role is not recognized");
		}
		try {
			return LibraryRole.valueOf(claim);
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidJwtException("Token role is not recognized");
		}
	}

}
