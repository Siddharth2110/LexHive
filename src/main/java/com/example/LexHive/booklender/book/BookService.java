package com.example.LexHive.booklender.book;

import com.example.LexHive.booklender.exception.LibraryException;
import com.example.LexHive.booklender.loan.LoanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class BookService {

	private final BookRepository bookRepository;
	private final LoanRepository loanRepository;

	public BookService(BookRepository bookRepository, LoanRepository loanRepository) {
		this.bookRepository = bookRepository;
		this.loanRepository = loanRepository;
	}

	@Transactional
	public BookResponse create(BookRequest request) {
		String isbn = normalizeIsbn(request.isbn());
		if (bookRepository.existsByIsbn(isbn)) {
			throw duplicateIsbn();
		}
		Book book = new Book(request.title().trim(), request.author().trim(), isbn, request.totalCopies());
		return BookResponse.from(bookRepository.save(book));
	}

	@Transactional(readOnly = true)
	public List<BookResponse> list() {
		return bookRepository.findAllByOrderByIdAsc().stream().map(BookResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public BookResponse get(Long id) {
		return BookResponse.from(findBook(id));
	}

	@Transactional
	public BookResponse update(Long id, BookRequest request) {
		Book book = bookRepository.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
		String isbn = normalizeIsbn(request.isbn());
		if (!isbn.equals(book.getIsbn()) && bookRepository.existsByIsbnAndIdNot(isbn, id)) {
			throw duplicateIsbn();
		}
		book.changeDetails(request.title().trim(), request.author().trim(), isbn);
		book.adjustTotalCopies(request.totalCopies());
		bookRepository.update(book);
		return BookResponse.from(book);
	}

	@Transactional
	public void delete(Long id) {
		Book book = bookRepository.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
		if (loanRepository.existsByBook_Id(id)) {
			throw new LibraryException(
					HttpStatus.CONFLICT,
					"BOOK_IN_USE",
					"Book " + id + " has loan history and cannot be deleted");
		}
		bookRepository.delete(book);
	}

	private Book findBook(Long id) {
		return bookRepository.findById(id).orElseThrow(() -> notFound(id));
	}

	private static LibraryException notFound(Long id) {
		return new LibraryException(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Book " + id + " was not found");
	}

	private static LibraryException duplicateIsbn() {
		return new LibraryException(HttpStatus.CONFLICT, "DUPLICATE_ISBN", "A book with this ISBN already exists");
	}

	private static String normalizeIsbn(String isbn) {
		return isbn.trim().toUpperCase(Locale.ROOT);
	}

}
