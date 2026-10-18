package com.example.LexHive.booklender.security;

import com.example.LexHive.booklender.book.BookController;
import com.example.LexHive.booklender.book.BookResponse;
import com.example.LexHive.booklender.book.BookService;
import com.example.LexHive.booklender.config.ClockConfig;
import com.example.LexHive.booklender.loan.LoanController;
import com.example.LexHive.booklender.loan.LoanResponse;
import com.example.LexHive.booklender.loan.LoanService;
import com.example.LexHive.booklender.member.MemberController;
import com.example.LexHive.booklender.member.MemberResponse;
import com.example.LexHive.booklender.member.MemberService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
		AuthController.class,
		BookController.class,
		LoanController.class,
		MemberController.class
})
@Import({
		SecurityConfig.class,
		JwtService.class,
		LibraryUserDetailsService.class,
		AuthService.class,
		SecurityHttpResponses.class,
		ClockConfig.class,
		JacksonAutoConfiguration.class
})
@EnableConfigurationProperties(SecurityProperties.class)
class SecurityAccessTest {

	private static final String BOOK_JSON = """
			{"title":"Dune","author":"Frank Herbert","isbn":"9780441172719","totalCopies":1}
			""";

	private static final String MEMBER_JSON = """
			{"name":"Ada Lovelace","email":"ada@example.com"}
			""";

	private static final String BORROW_JSON = """
			{"bookId":1,"memberId":1}
			""";

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JwtService jwtService;

	@MockitoBean
	private BookService bookService;

	@MockitoBean
	private LoanService loanService;

	@MockitoBean
	private MemberService memberService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		lenient().when(bookService.list()).thenReturn(List.of());
		lenient().when(bookService.get(anyLong())).thenReturn(book());
		lenient().when(bookService.create(any())).thenReturn(book());
		lenient().when(bookService.update(anyLong(), any())).thenReturn(book());
		lenient().when(memberService.list()).thenReturn(List.of());
		lenient().when(memberService.create(any())).thenReturn(new MemberResponse(1L, "Ada Lovelace", "ada@example.com"));
		lenient().when(loanService.borrow(any())).thenReturn(loan());
		lenient().when(loanService.returnLoan(anyLong())).thenReturn(loan());
		lenient().when(loanService.list(nullable(Long.class))).thenReturn(List.of());
		lenient().when(loanService.get(anyLong())).thenReturn(loan());
	}

	@Test
	void loginWithoutATokenReturnsAUsableJwt() throws Exception {
		MvcResult result = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"alice","password":"password123"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(3600))
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andReturn();

		String accessToken = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");

		mockMvc.perform(get("/api/books")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk());
	}

	@Test
	void invalidCredentialsAreRejected() throws Exception {
		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"alice","password":"wrong-password"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));

		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"nobody","password":"password123"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	@Test
	void protectedEndpointsRejectMissingAndInvalidTokens() throws Exception {
		mockMvc.perform(get("/api/books"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

		mockMvc.perform(get("/api/books").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_TOKEN"));

		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token")
						.content("""
								{"username":"alice","password":"password123"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_TOKEN"));

		String expired = jwtService.generateToken(
				"alice",
				LibraryRole.MEMBER,
				Instant.parse("2020-01-01T00:00:00Z"),
				Instant.parse("2020-01-01T01:00:00Z"));
		mockMvc.perform(get("/api/books").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
	}

	@Test
	void memberCanViewBooksBorrowAndReturnButCannotManageTheCatalogOrMembers() throws Exception {
		String token = token(LibraryRole.MEMBER);

		mockMvc.perform(get("/api/books").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/books/1").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/loans").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/loans")
						.header(HttpHeaders.AUTHORIZATION, token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(BORROW_JSON))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/loans/1/return").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/books")
						.header(HttpHeaders.AUTHORIZATION, token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(BOOK_JSON))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));
		mockMvc.perform(put("/api/books/1")
						.header(HttpHeaders.AUTHORIZATION, token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(BOOK_JSON))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/books/1").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/members")
						.header(HttpHeaders.AUTHORIZATION, token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(MEMBER_JSON))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/members").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isForbidden());

		verify(bookService, never()).create(any());
		verify(bookService, never()).update(anyLong(), any());
		verify(bookService, never()).delete(anyLong());
		verify(memberService, never()).create(any());
	}

	@Test
	void librarianCanManageBooksAndMembersButCannotBorrowOrReturn() throws Exception {
		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"librarian","password":"librarian123"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"));

		String token = token(LibraryRole.LIBRARIAN);

		mockMvc.perform(get("/api/books").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/books")
						.header(HttpHeaders.AUTHORIZATION, token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(BOOK_JSON))
				.andExpect(status().isCreated());
		mockMvc.perform(put("/api/books/1")
						.header(HttpHeaders.AUTHORIZATION, token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(BOOK_JSON))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/books/1").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/members")
						.header(HttpHeaders.AUTHORIZATION, token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(MEMBER_JSON))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/api/members").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/loans/1").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/loans")
						.header(HttpHeaders.AUTHORIZATION, token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(BORROW_JSON))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/loans/1/return").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isForbidden());

		verify(loanService, never()).borrow(any());
		verify(loanService, never()).returnLoan(anyLong());
	}

	private String token(LibraryRole role) {
		String username = role == LibraryRole.MEMBER ? "alice" : "librarian";
		return "Bearer " + jwtService.generateToken(username, role);
	}

	private static BookResponse book() {
		return new BookResponse(1L, "Dune", "Frank Herbert", "9780441172719", 1, 1);
	}

	private static LoanResponse loan() {
		return new LoanResponse(
				1L,
				1L,
				1L,
				Instant.parse("2026-01-01T00:00:00Z"),
				Instant.parse("2026-01-15T00:00:00Z"),
				null);
	}

}
