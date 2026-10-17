package com.example.LexHive.booklender.member;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MemberRepository {

	private static final RowMapper<Member> MEMBER_ROW = (rs, rowNum) -> Member.restore(
			rs.getLong("id"),
			rs.getString("name"),
			rs.getString("email"));

	private final JdbcTemplate jdbc;

	public MemberRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public Member save(Member member) {
		Long id = jdbc.queryForObject("""
				INSERT INTO members (name, email)
				VALUES (?, ?)
				RETURNING id
				""",
				Long.class,
				member.getName(),
				member.getEmail());
		member.assignId(generatedId(id));
		return member;
	}

	public void update(Member member) {
		jdbc.update("""
				UPDATE members
				SET name = ?, email = ?
				WHERE id = ?
				""",
				member.getName(),
				member.getEmail(),
				member.getId());
	}

	public void delete(Member member) {
		jdbc.update("DELETE FROM members WHERE id = ?", member.getId());
	}

	public Optional<Member> findById(Long id) {
		return one("""
				SELECT id, name, email
				FROM members
				WHERE id = ?
				""", id);
	}

	public Optional<Member> findByIdForUpdate(Long id) {
		return one("""
				SELECT id, name, email
				FROM members
				WHERE id = ?
				FOR UPDATE
				""", id);
	}

	public List<Member> findAllByOrderByIdAsc() {
		return jdbc.query("""
				SELECT id, name, email
				FROM members
				ORDER BY id
				""", MEMBER_ROW);
	}

	public Optional<Long> findIdByEmail(String email) {
		List<Long> ids = jdbc.queryForList("SELECT id FROM members WHERE email = ?", Long.class, email);
		return ids.stream().findFirst();
	}

	public boolean existsById(Long id) {
		return exists("SELECT EXISTS (SELECT 1 FROM members WHERE id = ?)", id);
	}

	public boolean existsByEmail(String email) {
		return exists("SELECT EXISTS (SELECT 1 FROM members WHERE email = ?)", email);
	}

	public boolean existsByEmailAndIdNot(String email, Long id) {
		return exists("SELECT EXISTS (SELECT 1 FROM members WHERE email = ? AND id <> ?)", email, id);
	}

	private Optional<Member> one(String sql, Object... args) {
		List<Member> rows = jdbc.query(sql, MEMBER_ROW, args);
		if (rows.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(rows.get(0));
	}

	private boolean exists(String sql, Object... args) {
		Boolean exists = jdbc.queryForObject(sql, Boolean.class, args);
		return Boolean.TRUE.equals(exists);
	}

	private static Long generatedId(Long id) {
		if (id == null) {
			throw new IllegalStateException("Insert into members did not return an id");
		}
		return id;
	}

}
