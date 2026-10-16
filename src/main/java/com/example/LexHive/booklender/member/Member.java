package com.example.LexHive.booklender.member;

public class Member {

	private Long id;
	private String name;
	private String email;

	public Member(String name, String email) {
		this.name = name;
		this.email = email;
	}

	public static Member restore(Long id, String name, String email) {
		Member member = new Member(name, email);
		member.id = id;
		return member;
	}

	void assignId(Long id) {
		this.id = id;
	}

	public void changeDetails(String name, String email) {
		this.name = name;
		this.email = email;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

}
