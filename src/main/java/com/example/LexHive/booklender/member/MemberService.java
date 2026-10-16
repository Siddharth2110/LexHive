package com.example.LexHive.booklender.member;

import com.example.LexHive.booklender.exception.LibraryException;
import com.example.LexHive.booklender.loan.LoanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class MemberService {

	private final MemberRepository memberRepository;
	private final LoanRepository loanRepository;

	public MemberService(MemberRepository memberRepository, LoanRepository loanRepository) {
		this.memberRepository = memberRepository;
		this.loanRepository = loanRepository;
	}

	@Transactional
	public MemberResponse create(MemberRequest request) {
		String email = normalizeEmail(request.email());
		if (memberRepository.existsByEmail(email)) {
			throw duplicateEmail();
		}
		Member member = new Member(request.name().trim(), email);
		return MemberResponse.from(memberRepository.save(member));
	}

	@Transactional(readOnly = true)
	public List<MemberResponse> list() {
		return memberRepository.findAllByOrderByIdAsc().stream().map(MemberResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public MemberResponse get(Long id) {
		return MemberResponse.from(findMember(id));
	}

	@Transactional
	public MemberResponse update(Long id, MemberRequest request) {
		Member member = memberRepository.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
		String email = normalizeEmail(request.email());
		if (!email.equals(member.getEmail()) && memberRepository.existsByEmailAndIdNot(email, id)) {
			throw duplicateEmail();
		}
		member.changeDetails(request.name().trim(), email);
		memberRepository.update(member);
		return MemberResponse.from(member);
	}

	@Transactional
	public void delete(Long id) {
		Member member = memberRepository.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
		if (loanRepository.existsByMember_Id(id)) {
			throw new LibraryException(
					HttpStatus.CONFLICT,
					"MEMBER_IN_USE",
					"Member " + id + " has loan history and cannot be deleted");
		}
		memberRepository.delete(member);
	}

	private Member findMember(Long id) {
		return memberRepository.findById(id).orElseThrow(() -> notFound(id));
	}

	private static LibraryException notFound(Long id) {
		return new LibraryException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "Member " + id + " was not found");
	}

	private static LibraryException duplicateEmail() {
		return new LibraryException(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "A member with this email already exists");
	}

	static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
