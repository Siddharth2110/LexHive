package com.example.LexHive.booklender.loan;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
	public ResponseEntity<LoanResponse> borrow(@Valid @RequestBody BorrowRequest request) {
		LoanResponse created = loanService.borrow(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PostMapping("/{id}/return")
	public LoanResponse returnLoan(@PathVariable Long id) {
		return loanService.returnLoan(id);
	}

	@GetMapping("/{id}")
	public LoanResponse get(@PathVariable Long id) {
		return loanService.get(id);
	}

	@GetMapping
	public List<LoanResponse> list(@RequestParam(required = false) Long memberId) {
		return loanService.list(memberId);
	}

}
