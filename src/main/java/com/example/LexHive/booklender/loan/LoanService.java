package com.example.LexHive.booklender.loan;

import com.example.LexHive.booklender.book.Book;
import com.example.LexHive.booklender.book.BookRepository;
import com.example.LexHive.booklender.exception.LibraryException;
import com.example.LexHive.booklender.member.Member;
import com.example.LexHive.booklender.member.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class LoanService {

	private static final Logger log = LoggerFactory.getLogger(LoanService.class);

	private final LoanRepository loanRepository;
	private final BookRepository bookRepository;
	private final MemberRepository memberRepository;
	private final BorrowingPolicy borrowingPolicy;
	private final Clock clock;

	public LoanService(
			LoanRepository loanRepository,
			BookRepository bookRepository,
			MemberRepository memberRepository,
			BorrowingPolicy borrowingPolicy,
			Clock clock) {
		this.loanRepository = loanRepository;
		this.bookRepository = bookRepository;
		this.memberRepository = memberRepository;
		this.borrowingPolicy = borrowingPolicy;
		this.clock = clock;
	}

	@Transactional
	public LoanResponse borrow(Long bookId, Long memberId) {
		Member member = memberRepository.findByIdForUpdate(memberId)
				.orElseThrow(() -> memberNotFound(memberId));
		Instant now = clock.instant();
		long activeLoans = loanRepository.countByMember_IdAndReturnedAtIsNull(member.getId());
		boolean overdue = loanRepository.existsByMember_IdAndReturnedAtIsNullAndDueDateBefore(member.getId(), now);
		borrowingPolicy.checkCanBorrow(activeLoans, overdue);

		Book book = bookRepository.findByIdForUpdate(bookId)
				.orElseThrow(() -> bookNotFound(bookId));
		book.borrowOneCopy();
		bookRepository.update(book);

		Loan loan = loanRepository.save(new Loan(book, member, now, borrowingPolicy.dueDateFrom(now)));
		log.info("Member {} borrowed book {} as loan {}", member.getId(), book.getId(), loan.getId());
		return LoanResponse.from(loan);
	}

	@Transactional
	public LoanResponse returnLoan(Long loanId) {
		Long bookId = loanRepository.findBookIdById(loanId).orElseThrow(() -> loanNotFound(loanId));
		Book book = bookRepository.findByIdForUpdate(bookId).orElseThrow(() -> bookNotFound(bookId));
		Loan loan = loanRepository.findByIdForUpdate(loanId).orElseThrow(() -> loanNotFound(loanId));
		loan.markReturned(clock.instant());
		book.returnOneCopy();
		loanRepository.update(loan);
		bookRepository.update(book);
		log.info("Loan {} returned for book {}", loan.getId(), book.getId());
		return LoanResponse.from(loan);
	}

	@Transactional(readOnly = true)
	public LoanResponse get(Long id) {
		return LoanResponse.from(loanRepository.findById(id).orElseThrow(() -> loanNotFound(id)));
	}

	@Transactional(readOnly = true)
	public List<LoanResponse> list(Long memberId) {
		if (memberId == null) {
			return loanRepository.findAllWithAssociations().stream().map(LoanResponse::from).toList();
		}
		if (!memberRepository.existsById(memberId)) {
			throw memberNotFound(memberId);
		}
		return loanRepository.findByMemberIdWithAssociations(memberId).stream().map(LoanResponse::from).toList();
	}

	private static LibraryException bookNotFound(Long id) {
		return new LibraryException(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Book " + id + " was not found");
	}

	private static LibraryException memberNotFound(Long id) {
		return new LibraryException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Member " + id + " was not found");
	}

	private static LibraryException loanNotFound(Long id) {
		return new LibraryException(HttpStatus.NOT_FOUND, "LOAN_NOT_FOUND", "Loan " + id + " was not found");
	}

}
