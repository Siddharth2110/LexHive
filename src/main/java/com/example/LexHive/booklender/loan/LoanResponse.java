package com.example.LexHive.booklender.loan;

import java.time.Instant;

public record LoanResponse(
		Long id,
		Long bookId,
		Long memberId,
		Instant borrowedAt,
		Instant dueDate,
		Instant returnedAt
) {

	public static LoanResponse from(Loan loan) {
		return new LoanResponse(
				loan.getId(),
				loan.getBook().getId(),
				loan.getMember().getId(),
				loan.getBorrowedAt(),
				loan.getDueDate(),
				loan.getReturnedAt());
	}

}
