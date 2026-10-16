package com.example.LexHive.booklender.loan;

import com.example.LexHive.booklender.config.BorrowingProperties;
import com.example.LexHive.booklender.exception.LibraryException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class BorrowingPolicy {

	private final BorrowingProperties properties;

	public BorrowingPolicy(BorrowingProperties properties) {
		if (properties.getMaxActiveLoans() < 1) {
			throw new IllegalStateException("lexhive.borrowing.max-active-loans must be at least 1");
		}
		if (properties.getLoanDurationDays() < 1) {
			throw new IllegalStateException("lexhive.borrowing.loan-duration-days must be at least 1");
		}
		this.properties = properties;
	}

	public void checkCanBorrow(long activeLoanCount, boolean hasOverdueLoan) {
		int maxActiveLoans = properties.getMaxActiveLoans();
		if (activeLoanCount >= maxActiveLoans) {
			throw new LibraryException(
					HttpStatus.CONFLICT,
					"MAX_ACTIVE_LOANS",
					"Member already has " + activeLoanCount + " active loans; the limit is " + maxActiveLoans);
		}
		if (hasOverdueLoan) {
			throw new LibraryException(
					HttpStatus.CONFLICT,
					"OVERDUE_LOANS",
					"Member has an overdue loan and cannot borrow until it is returned");
		}
	}

	public Instant dueDateFrom(Instant borrowedAt) {
		return borrowedAt.plus(properties.getLoanDurationDays(), ChronoUnit.DAYS);
	}

}
