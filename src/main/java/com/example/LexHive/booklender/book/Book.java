package com.example.LexHive.booklender.book;

import com.example.LexHive.booklender.exception.LibraryException;
import org.springframework.http.HttpStatus;

public class Book {

	private Long id;
	private String title;
	private String author;
	private String isbn;
	private int totalCopies;
	private int availableCopies;

	public Book(String title, String author, String isbn, int totalCopies) {
		this.title = title;
		this.author = author;
		this.isbn = isbn;
		this.totalCopies = totalCopies;
		this.availableCopies = totalCopies;
	}

	public static Book restore(
			Long id,
			String title,
			String author,
			String isbn,
			int totalCopies,
			int availableCopies) {
		Book book = new Book(title, author, isbn, totalCopies);
		book.id = id;
		book.availableCopies = availableCopies;
		return book;
	}

	void assignId(Long id) {
		this.id = id;
	}

	public void changeDetails(String title, String author, String isbn) {
		this.title = title;
		this.author = author;
		this.isbn = isbn;
	}

	public void adjustTotalCopies(int newTotal) {
		int onLoan = this.totalCopies - this.availableCopies;
		if (newTotal < onLoan) {
			String copies = onLoan == 1 ? "copy is" : "copies are";
			throw new LibraryException(
					HttpStatus.CONFLICT,
					"INSUFFICIENT_TOTAL_COPIES",
					"Cannot set total copies to " + newTotal + " while " + onLoan + " " + copies + " on loan");
		}
		this.availableCopies = newTotal - onLoan;
		this.totalCopies = newTotal;
	}

	public void borrowOneCopy() {
		if (availableCopies < 1) {
			throw new LibraryException(
					HttpStatus.CONFLICT,
					"NO_COPIES_AVAILABLE",
					"No copies of this book are available");
		}
		availableCopies--;
	}

	public void returnOneCopy() {
		if (availableCopies >= totalCopies) {
			throw new LibraryException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					"COPY_COUNT_INCONSISTENT",
					"All copies of book " + id + " are already marked available");
		}
		availableCopies++;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}

	public String getIsbn() {
		return isbn;
	}

	public int getTotalCopies() {
		return totalCopies;
	}

	public int getAvailableCopies() {
		return availableCopies;
	}

}
