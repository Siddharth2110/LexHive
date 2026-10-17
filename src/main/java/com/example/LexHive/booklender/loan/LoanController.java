package com.example.LexHive.booklender.loan;

import com.example.LexHive.booklender.exception.LibraryException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(path = "/api/loans", produces = MediaType.APPLICATION_JSON_VALUE)
public class LoanController {

	private final LoanService loanService;

	public LoanController(LoanService loanService) {
		this.loanService = loanService;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<LoanResponse> borrow(@Valid @RequestBody BorrowRequest request, @AuthenticationPrincipal Jwt jwt) {
		Long memberId;
		if (LoanUtils.isLibrarian(jwt)) {
			if (request.memberId() == null) {
				throw new LibraryException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "memberId must not be null");
			}
			memberId = request.memberId();
		} else {
			memberId = LoanUtils.memberIdFrom(jwt);
		}

		LoanResponse created = loanService.borrow(request.bookId(), memberId);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PostMapping("/{id}/return")
	public LoanResponse returnLoan(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
		LoanUtils.checkOwnLoan(jwt, loanService.get(id));
		return loanService.returnLoan(id);
	}

	@GetMapping("/{id}")
	public LoanResponse get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
		LoanResponse loan = loanService.get(id);
		LoanUtils.checkOwnLoan(jwt, loan);
		return loan;
	}

	@GetMapping
	public List<LoanResponse> list(@RequestParam(required = false) Long memberId, @AuthenticationPrincipal Jwt jwt) {
		if (LoanUtils.isLibrarian(jwt)) {
			return loanService.list(memberId);
		}
		return loanService.list(LoanUtils.memberIdFrom(jwt));
	}
	

}
