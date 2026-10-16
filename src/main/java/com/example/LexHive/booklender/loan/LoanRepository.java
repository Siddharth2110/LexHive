package com.example.LexHive.booklender.loan;

import com.example.LexHive.booklender.book.Book;
import com.example.LexHive.booklender.member.Member;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class LoanRepository {

	private static final String LOAN_SELECT = """
			SELECT
				l.id AS loan_id,
				l.borrowed_at,
				l.due_date,
				l.returned_at,
				b.id AS book_id,
				b.title,
				b.author,
				b.isbn,
				b.total_copies,
				b.available_copies,
				m.id AS member_id,
				m.name,
				m.email
			FROM loans l
			JOIN books b ON b.id = l.book_id
			JOIN members m ON m.id = l.member_id
			""";

	private static final RowMapper<Loan> LOAN_ROW = (rs, rowNum) -> Loan.restore(
			rs.getLong("loan_id"),
			book(rs),
			member(rs),
			instant(rs, "borrowed_at"),
			instant(rs, "due_date"),
			instant(rs, "returned_at"));

	private final JdbcTemplate jdbc;

	public LoanRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public Loan save(Loan loan) {
		Long id = jdbc.queryForObject("""
				INSERT INTO loans (book_id, member_id, borrowed_at, due_date)
				VALUES (?, ?, ?, ?)
				RETURNING id
				""",
				Long.class,
				loan.getBook().getId(),
				loan.getMember().getId(),
				toOffset(loan.getBorrowedAt()),
				toOffset(loan.getDueDate()));
		loan.assignId(generatedId(id));
		return loan;
	}

	public void update(Loan loan) {
		jdbc.update(
				"UPDATE loans SET returned_at = ? WHERE id = ?",
				toOffset(loan.getReturnedAt()),
				loan.getId());
	}

	public Optional<Loan> findById(Long id) {
		return one(LOAN_SELECT + " WHERE l.id = ?", id);
	}

	public Optional<Loan> findByIdForUpdate(Long id) {
		return one(LOAN_SELECT + " WHERE l.id = ? FOR UPDATE OF l", id);
	}

	public List<Loan> findAllWithAssociations() {
		return jdbc.query(LOAN_SELECT + " ORDER BY l.borrowed_at DESC, l.id DESC", LOAN_ROW);
	}

	public List<Loan> findByMemberIdWithAssociations(Long memberId) {
		return jdbc.query(
				LOAN_SELECT + " WHERE l.member_id = ? ORDER BY l.borrowed_at DESC, l.id DESC",
				LOAN_ROW,
				memberId);
	}

	public Optional<Long> findBookIdById(Long id) {
		List<Long> ids = jdbc.query(
				"SELECT book_id FROM loans WHERE id = ?",
				(rs, rowNum) -> rs.getLong("book_id"),
				id);
		if (ids.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(ids.get(0));
	}

	public long countByMember_IdAndReturnedAtIsNull(Long memberId) {
		Long count = jdbc.queryForObject("""
				SELECT COUNT(*)
				FROM loans
				WHERE member_id = ? AND returned_at IS NULL
				""",
				Long.class,
				memberId);
		return count == null ? 0 : count;
	}

	public boolean existsByMember_IdAndReturnedAtIsNullAndDueDateBefore(Long memberId, Instant now) {
		return exists("""
				SELECT EXISTS (
					SELECT 1
					FROM loans
					WHERE member_id = ?
						AND returned_at IS NULL
						AND due_date < ?
				)
				""", memberId, toOffset(now));
	}

	public boolean existsByBook_Id(Long bookId) {
		return exists("SELECT EXISTS (SELECT 1 FROM loans WHERE book_id = ?)", bookId);
	}

	public boolean existsByMember_Id(Long memberId) {
		return exists("SELECT EXISTS (SELECT 1 FROM loans WHERE member_id = ?)", memberId);
	}

	private Optional<Loan> one(String sql, Object... args) {
		List<Loan> rows = jdbc.query(sql, LOAN_ROW, args);
		if (rows.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(rows.get(0));
	}

	private boolean exists(String sql, Object... args) {
		Boolean exists = jdbc.queryForObject(sql, Boolean.class, args);
		return Boolean.TRUE.equals(exists);
	}

	private static Book book(ResultSet rs) throws SQLException {
		return Book.restore(
				rs.getLong("book_id"),
				rs.getString("title"),
				rs.getString("author"),
				rs.getString("isbn"),
				rs.getInt("total_copies"),
				rs.getInt("available_copies"));
	}

	private static Member member(ResultSet rs) throws SQLException {
		return Member.restore(
				rs.getLong("member_id"),
				rs.getString("name"),
				rs.getString("email"));
	}

	private static Instant instant(ResultSet rs, String column) throws SQLException {
		OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
		return value == null ? null : value.toInstant();
	}

	private static OffsetDateTime toOffset(Instant instant) {
		return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

	private static Long generatedId(Long id) {
		if (id == null) {
			throw new IllegalStateException("Insert into loans did not return an id");
		}
		return id;
	}

}
