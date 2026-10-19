package com.example.LexHive.booklender.member;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(path = "/api/members", produces = MediaType.APPLICATION_JSON_VALUE)
public class MemberController {

	private final MemberService memberService;

	public MemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MemberResponse> create(@Valid @RequestBody CreateMemberRequest request) {
		MemberResponse created = memberService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@GetMapping
	public List<MemberResponse> list() {
		return memberService.list();
	}

	@GetMapping("/{id}")
	public MemberResponse get(@PathVariable Long id) {
		return memberService.get(id);
	}

	@PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public MemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
		return memberService.update(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		memberService.delete(id);
		return ResponseEntity.noContent().build();
	}

}
