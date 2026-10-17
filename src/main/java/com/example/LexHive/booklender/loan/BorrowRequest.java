package com.example.LexHive.booklender.loan;

import jakarta.validation.constraints.NotNull;

/**
 * memberId is only read for librarians. Members always borrow for themselves,
 * using the memberId from their token.
 */
public record BorrowRequest(@NotNull Long bookId, Long memberId) {
}
