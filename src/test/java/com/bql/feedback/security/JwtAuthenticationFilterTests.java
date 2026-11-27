package com.bql.feedback.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTests {

    private static final String TEST_SECRET = "bql-feedback-test-jwt-secret-key-minimum-32-bytes-2026";

    private final JwtService jwtService = new JwtService(TEST_SECRET, 480);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenSetsAuthenticatedUserAndRoleAuthority() throws Exception {
        String token = jwtService.generateToken(2L, "manager@bql.vn", "CS_MANAGER", null, 5L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/accounts/pending");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        AuthenticatedUser principal = assertInstanceOf(AuthenticatedUser.class, auth.getPrincipal());
        assertEquals(2L, principal.accountId());
        assertEquals("manager@bql.vn", principal.username());
        assertEquals("CS_MANAGER", principal.role());
        assertEquals(5L, principal.staffId());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CS_MANAGER".equals(a.getAuthority())));
    }

    @Test
    void invalidTokenClearsSecurityContextAndContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/feedbacks");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void missingOrBlankHeaderLeavesContextEmpty() throws Exception {
        MockHttpServletRequest noHeaderReq = new MockHttpServletRequest("GET", "/api/feedbacks");
        MockFilterChain chain1 = new MockFilterChain();
        filter.doFilter(noHeaderReq, new MockHttpServletResponse(), chain1);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain1.getRequest());

        MockHttpServletRequest blankBearerReq = new MockHttpServletRequest("GET", "/api/feedbacks");
        blankBearerReq.addHeader(HttpHeaders.AUTHORIZATION, "Bearer    ");
        MockFilterChain chain2 = new MockFilterChain();
        filter.doFilter(blankBearerReq, new MockHttpServletResponse(), chain2);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain2.getRequest());
    }
}
