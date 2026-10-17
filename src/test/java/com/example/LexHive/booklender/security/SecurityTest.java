package com.example.LexHive.booklender.security;

import com.example.LexHive.booklender.book.BookController;
import com.example.LexHive.booklender.book.BookService;
import com.example.LexHive.booklender.config.ClockConfig;
import com.example.LexHive.booklender.loan.LoanController;
import com.example.LexHive.booklender.loan.LoanResponse;
import com.example.LexHive.booklender.loan.LoanService;
import com.example.LexHive.booklender.member.MemberController;
import com.example.LexHive.booklender.member.MemberRepository;
import com.example.LexHive.booklender.member.MemberService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, BookController.class, MemberController.class, LoanController.class})
@Import({SecurityConfig.class, AuthService.class, ClockConfig.class})
class SecurityTest {

	private static final long SIDDHARTH_ID = 1L;
	private static final long VARSHNEY_ID = 2L;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BookService bookService;

	@MockitoBean
	private MemberService memberService;

	@MockitoBean
	private LoanService loanService;

	@MockitoBean
	private MemberRepository memberRepository;

	@BeforeEach
	void linkLoginsToMembers() {
		when(memberRepository.findIdByEmail("siddharth@lexhive.com")).thenReturn(Optional.of(SIDDHARTH_ID));
		when(memberRepository.findIdByEmail("varshney@lexhive.com")).thenReturn(Optional.of(VARSHNEY_ID));
	}

	@Test
	void requestWithoutTokenIsRejected() throws Exception {
		mockMvc.perform(get("/api/books"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void wrongPasswordDoesNotGetAToken() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"siddharth@lexhive.com\",\"password\":\"wrong\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void memberCanReadBooksButCannotManageMembers() throws Exception {
		when(bookService.list()).thenReturn(List.of());
		String token = loginAs("siddharth@lexhive.com", "siddharth123");

		mockMvc.perform(get("/api/books").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
	}

	@Test
	void librarianCanManageMembers() throws Exception {
		when(memberService.list()).thenReturn(List.of());
		String token = loginAs("librarian", "librarian123");

		mockMvc.perform(get("/api/members").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void tamperedTokenIsRejected() throws Exception {
		String token = loginAs("siddharth@lexhive.com", "siddharth123");

		mockMvc.perform(get("/api/books").header("Authorization", "Bearer " + token + "x"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void loginPutsTheMemberIdInTheResponse() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"siddharth@lexhive.com\",\"password\":\"siddharth123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.memberId").value(SIDDHARTH_ID));
	}

	@Test
	void memberAlwaysBorrowsForThemselvesEvenIfAnotherMemberIdIsSent() throws Exception {
		when(loanService.borrow(5L, SIDDHARTH_ID)).thenReturn(loanOf(10L, SIDDHARTH_ID));
		String token = loginAs("siddharth@lexhive.com", "siddharth123");

		mockMvc.perform(post("/api/loans").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"bookId\":5,\"memberId\":" + VARSHNEY_ID + "}"))
				.andExpect(status().isCreated());

		verify(loanService).borrow(5L, SIDDHARTH_ID);
		verify(loanService, never()).borrow(5L, VARSHNEY_ID);
	}

	@Test
	void librarianMustSayWhichMemberIsBorrowing() throws Exception {
		when(loanService.borrow(5L, VARSHNEY_ID)).thenReturn(loanOf(10L, VARSHNEY_ID));
		String token = loginAs("librarian", "librarian123");

		mockMvc.perform(post("/api/loans").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"bookId\":5,\"memberId\":" + VARSHNEY_ID + "}"))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/loans").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"bookId\":5}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void memberCannotSeeOrReturnSomeoneElsesLoan() throws Exception {
		when(loanService.get(20L)).thenReturn(loanOf(20L, VARSHNEY_ID));
		String token = loginAs("siddharth@lexhive.com", "siddharth123");

		mockMvc.perform(get("/api/loans/20").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/loans/20/return").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
		verify(loanService, never()).returnLoan(20L);
	}

	@Test
	void memberOnlySeesTheirOwnLoanList() throws Exception {
		when(loanService.list(SIDDHARTH_ID)).thenReturn(List.of(loanOf(10L, SIDDHARTH_ID)));
		String token = loginAs("siddharth@lexhive.com", "siddharth123");

		mockMvc.perform(get("/api/loans").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/loans").param("memberId", String.valueOf(VARSHNEY_ID))
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		verify(loanService, times(2)).list(SIDDHARTH_ID);
		verify(loanService, never()).list(VARSHNEY_ID);
	}

	@Test
	void librarianCanReturnAnyonesLoan() throws Exception {
		when(loanService.get(20L)).thenReturn(loanOf(20L, VARSHNEY_ID));
		when(loanService.returnLoan(20L)).thenReturn(loanOf(20L, VARSHNEY_ID));
		String token = loginAs("librarian", "librarian123");

		mockMvc.perform(post("/api/loans/20/return").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	private static LoanResponse loanOf(long loanId, long memberId) {
		Instant now = Instant.now();
		return new LoanResponse(loanId, 5L, memberId, now, now.plusSeconds(86400), null);
	}

	private String loginAs(String username, String password) throws Exception {
		String body = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

}
