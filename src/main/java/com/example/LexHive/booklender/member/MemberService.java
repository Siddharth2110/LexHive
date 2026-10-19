package com.example.LexHive.booklender.member;

import com.example.LexHive.booklender.exception.LibraryException;
import com.example.LexHive.booklender.loan.LoanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class MemberService {

	private final MemberRepository memberRepository;
	private final LoanRepository loanRepository;
	private final PasswordEncoder passwordEncoder;

	public MemberService(MemberRepository memberRepository,
						 LoanRepository loanRepository,
						 PasswordEncoder passwordEncoder) {
		this.memberRepository = memberRepository;
		this.loanRepository = loanRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public MemberResponse create(CreateMemberRequest request) {
		String email = MemberUtils.normalizeEmail(request.email());
		if (memberRepository.existsByEmail(email)) {
			throw MemberUtils.duplicateEmail();
		}
		Member member = new Member(request.name().trim(), email);
		String passwordHash = passwordEncoder.encode(request.password());
		return MemberResponse.from(memberRepository.save(member, passwordHash));
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
		Member member = memberRepository.findByIdForUpdate(id).orElseThrow(() -> MemberUtils.notFound(id));
		String email = MemberUtils.normalizeEmail(request.email());
		if (!email.equals(member.getEmail()) && memberRepository.existsByEmailAndIdNot(email, id)) {
			throw MemberUtils.duplicateEmail();
		}
		member.changeDetails(request.name().trim(), email);
		memberRepository.update(member);
		return MemberResponse.from(member);
	}

	@Transactional
	public void delete(Long id) {
		Member member = memberRepository.findByIdForUpdate(id).orElseThrow(() -> MemberUtils.notFound(id));
		if (loanRepository.existsByMember_Id(id)) {
			throw new LibraryException(
					HttpStatus.CONFLICT,
					"MEMBER_IN_USE",
					"Member " + id + " has loan history and cannot be deleted");
		}
		memberRepository.delete(member);
	}

	private Member findMember(Long id) {
		return memberRepository.findById(id).orElseThrow(() -> MemberUtils.notFound(id));
	}

}
