package com.example.LexHive.booklender.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "lexhive.borrowing")
public class BorrowingProperties {

	@Min(1)
	private int maxActiveLoans = 3;

	@Min(1)
	private int loanDurationDays = 14;

	public int getMaxActiveLoans() {
		return maxActiveLoans;
	}

	public void setMaxActiveLoans(int maxActiveLoans) {
		this.maxActiveLoans = maxActiveLoans;
	}

	public int getLoanDurationDays() {
		return loanDurationDays;
	}

	public void setLoanDurationDays(int loanDurationDays) {
		this.loanDurationDays = loanDurationDays;
	}

}
