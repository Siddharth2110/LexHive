package com.example.LexHive.booklender.book;

import com.example.LexHive.booklender.exception.LibraryException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookTest {

	@Test
	void newBookStartsWithEveryCopyAvailable() {
		Book book = new Book("Dune", "Frank Herbert", "9780441172719", 4);

		assertEquals(4, book.getTotalCopies());
		assertEquals(4, book.getAvailableCopies());
	}

	@Test
	void borrowingAndReturningACopyKeepsTheCountInsideTheTotal() {
		Book book = new Book("Dune", "Frank Herbert", "9780441172719", 1);

		book.borrowOneCopy();
		assertEquals(0, book.getAvailableCopies());

		book.returnOneCopy();
		assertEquals(1, book.getAvailableCopies());
	}

	@Test
	void refusesToLendWhenNoCopyIsAvailable() {
		Book book = new Book("Dune", "Frank Herbert", "9780441172719", 1);
		book.borrowOneCopy();

		LibraryException exception = assertThrows(LibraryException.class, book::borrowOneCopy);

		assertEquals("NO_COPIES_AVAILABLE", exception.getCode());
	}

	@Test
	void refusesToShrinkTheCatalogBelowTheCopiesOnLoan() {
		Book book = new Book("Dune", "Frank Herbert", "9780441172719", 3);
		book.borrowOneCopy();
		book.borrowOneCopy();

		LibraryException exception = assertThrows(LibraryException.class, () -> book.adjustTotalCopies(1));

		assertEquals("INSUFFICIENT_TOTAL_COPIES", exception.getCode());
		assertEquals(1, book.getAvailableCopies());
	}

	@Test
	void increasingTheTotalAddsTheSameNumberOfAvailableCopies() {
		Book book = new Book("Dune", "Frank Herbert", "9780441172719", 2);
		book.borrowOneCopy();

		book.adjustTotalCopies(5);

		assertEquals(5, book.getTotalCopies());
		assertEquals(4, book.getAvailableCopies());
	}

}
