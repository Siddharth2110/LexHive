package com.example.LexHive.booklender.loan;

import com.example.LexHive.booklender.exception.LibraryException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public class LoanUtils {

    public static boolean isLibrarian(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("LIBRARIAN");
    }

    public static Long memberIdFrom(Jwt jwt) {
        Number memberId = jwt.getClaim("memberId");
        if (memberId == null) {
            throw new LibraryException(HttpStatus.FORBIDDEN, "NO_MEMBER_PROFILE", "Your account is not linked to a library member");
        }
        return memberId.longValue();
    }

    public static void checkOwnLoan(Jwt jwt, LoanResponse loan) {
        if (isLibrarian(jwt)) {
            return;
        }
        if (!memberIdFrom(jwt).equals(loan.memberId())) {
            throw new LibraryException(HttpStatus.FORBIDDEN, "NOT_YOUR_LOAN", "Members can only access their own loans");
        }
    }
}
