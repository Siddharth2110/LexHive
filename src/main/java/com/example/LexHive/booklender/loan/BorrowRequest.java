package com.example.LexHive.booklender.loan;

import jakarta.validation.constraints.NotNull;

public record BorrowRequest(@NotNull Long bookId, @NotNull Long memberId) {
}
