package com.example.LexHive.booklender.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberRequest(
		@NotBlank @Size(max = 255) String name,
		@NotBlank @Email @Size(max = 255) String email
) {
}
