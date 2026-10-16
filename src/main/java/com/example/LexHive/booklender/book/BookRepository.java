package com.example.LexHive.booklender.book;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class BookRepository {

	private static final RowMapper<Book> BOOK_ROW = (rs, rowNum) -> Book.restore(
			rs.getLong("id"),
			rs.getString("title"),
			rs.getString("author"),
			rs.getString("isbn"),
			rs.getInt("total_copies"),
			rs.getInt("available_copies"));

	private final JdbcTemplate jdbc;

	public BookRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public Book save(Book book) {
		Long id = jdbc.queryForObject("""
				INSERT INTO books (title, author, isbn, total_copies, available_copies)
				VALUES (?, ?, ?, ?, ?)
				RETURNING id
				""",
				Long.class,
				book.getTitle(),
				book.getAuthor(),
				book.getIsbn(),
				book.getTotalCopies(),
				book.getAvailableCopies());
		book.assignId(generatedId(id));
		return book;
	}

	public void update(Book book) {
		jdbc.update("""
				UPDATE books
				SET title = ?, author = ?, isbn = ?, total_copies = ?, available_copies = ?
				WHERE id = ?
				""",
				book.getTitle(),
				book.getAuthor(),
				book.getIsbn(),
				book.getTotalCopies(),
				book.getAvailableCopies(),
				book.getId());
	}

	public void delete(Book book) {
		jdbc.update("DELETE FROM books WHERE id = ?", book.getId());
	}

	public Optional<Book> findById(Long id) {
		return one("""
				SELECT id, title, author, isbn, total_copies, available_copies
				FROM books
				WHERE id = ?
				""", id);
	}

	public Optional<Book> findByIdForUpdate(Long id) {
		return one("""
				SELECT id, title, author, isbn, total_copies, available_copies
				FROM books
				WHERE id = ?
				FOR UPDATE
				""", id);
	}

	public List<Book> findAllByOrderByIdAsc() {
		return jdbc.query("""
				SELECT id, title, author, isbn, total_copies, available_copies
				FROM books
				ORDER BY id
				""", BOOK_ROW);
	}

	public boolean existsByIsbn(String isbn) {
		return exists("SELECT EXISTS (SELECT 1 FROM books WHERE isbn = ?)", isbn);
	}

	public boolean existsByIsbnAndIdNot(String isbn, Long id) {
		return exists("SELECT EXISTS (SELECT 1 FROM books WHERE isbn = ? AND id <> ?)", isbn, id);
	}

	private Optional<Book> one(String sql, Object... args) {
		List<Book> rows = jdbc.query(sql, BOOK_ROW, args);
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
			throw new IllegalStateException("Insert into books did not return an id");
		}
		return id;
	}

}
