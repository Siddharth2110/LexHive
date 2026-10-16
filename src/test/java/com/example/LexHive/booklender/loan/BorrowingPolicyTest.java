package com.example.LexHive.booklender.loan;

import com.example.LexHive.booklender.config.BorrowingProperties;
import com.example.LexHive.booklender.exception.LibraryException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BorrowingPolicyTest {

	@Test
	void allowsBorrowWhenUnderTheLimitAndNothingIsOverdue() {
		BorrowingPolicy policy = policy(3, 14);

		assertDoesNotThrow(() -> policy.checkCanBorrow(2, false));
	}

	@Test
	void rejectsBorrowWhenActiveLoansReachTheConfiguredLimit() {
		BorrowingPolicy policy = policy(3, 14);

		LibraryException exception = assertThrows(LibraryException.class, () -> policy.checkCanBorrow(3, false));

		assertEquals("MAX_ACTIVE_LOANS", exception.getCode());
	}

	@Test
	void rejectsBorrowWhenTheMemberHasAnOverdueLoan() {
		BorrowingPolicy policy = policy(3, 14);

		LibraryException exception = assertThrows(LibraryException.class, () -> policy.checkCanBorrow(1, true));

		assertEquals("OVERDUE_LOANS", exception.getCode());
	}

	@Test
	void reportsTheLoanLimitBeforeTheOverdueRuleWhenBothApply() {
		BorrowingPolicy policy = policy(2, 14);

		LibraryException exception = assertThrows(LibraryException.class, () -> policy.checkCanBorrow(2, true));

		assertEquals("MAX_ACTIVE_LOANS", exception.getCode());
	}

	@Test
	void setsTheDueDateFromTheConfiguredDuration() {
		BorrowingPolicy policy = policy(3, 14);
		Instant borrowedAt = Instant.parse("2026-10-16T06:00:00Z");

		assertEquals(borrowedAt.plus(14, ChronoUnit.DAYS), policy.dueDateFrom(borrowedAt));
	}

	private static BorrowingPolicy policy(int maxActiveLoans, int loanDurationDays) {
		BorrowingProperties properties = new BorrowingProperties();
		properties.setMaxActiveLoans(maxActiveLoans);
		properties.setLoanDurationDays(loanDurationDays);
		return new BorrowingPolicy(properties);
	}

}
