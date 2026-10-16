package com.example.LexHive.booklender.loan;

import com.example.LexHive.booklender.book.Book;
import com.example.LexHive.booklender.exception.LibraryException;
import com.example.LexHive.booklender.member.Member;
import org.springframework.http.HttpStatus;

import java.time.Instant;

public class Loan {

	private Long id;
	private Book book;
	private Member member;
	private Instant borrowedAt;
	private Instant dueDate;
	private Instant returnedAt;

	public Loan(Book book, Member member, Instant borrowedAt, Instant dueDate) {
		this.book = book;
		this.member = member;
		this.borrowedAt = borrowedAt;
		this.dueDate = dueDate;
	}

	public static Loan restore(
			Long id,
			Book book,
			Member member,
			Instant borrowedAt,
			Instant dueDate,
			Instant returnedAt) {
		Loan loan = new Loan(book, member, borrowedAt, dueDate);
		loan.id = id;
		loan.returnedAt = returnedAt;
		return loan;
	}

	void assignId(Long id) {
		this.id = id;
	}

	public void markReturned(Instant returnedAt) {
		if (this.returnedAt != null) {
			throw new LibraryException(
					HttpStatus.CONFLICT,
					"LOAN_ALREADY_RETURNED",
					"Loan " + id + " was already returned");
		}
		this.returnedAt = returnedAt;
	}

	public Long getId() {
		return id;
	}

	public Book getBook() {
		return book;
	}

	public Member getMember() {
		return member;
	}

	public Instant getBorrowedAt() {
		return borrowedAt;
	}

	public Instant getDueDate() {
		return dueDate;
	}

	public Instant getReturnedAt() {
		return returnedAt;
	}

}
