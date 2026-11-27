package com.bql.feedback.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MustChangePasswordFilterTests {

    private final MustChangePasswordFilter filter = new MustChangePasswordFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksApiRequestsWhenMustChangePasswordIsTrue() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(10L, "staff01", "CS_STAFF", null, 3L, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_CS_STAFF"))));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/feedbacks");
        request.setServletPath("/api/feedbacks");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
        assertTrue(response.getContentAsString().contains("đổi mật khẩu"));
    }

    @Test
    void allowsChangePasswordAndOptionsWhenMustChangePasswordIsTrue() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(10L, "staff01", "CS_STAFF", null, 3L, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_CS_STAFF"))));

        MockHttpServletRequest changeReq = new MockHttpServletRequest("POST", "/api/auth/change-password");
        changeReq.setServletPath("/api/auth/change-password");
        MockFilterChain chain1 = new MockFilterChain();
        filter.doFilter(changeReq, new MockHttpServletResponse(), chain1);
        assertNotNull(chain1.getRequest());

        MockHttpServletRequest optionsReq = new MockHttpServletRequest("OPTIONS", "/api/feedbacks");
        optionsReq.setServletPath("/api/feedbacks");
        MockFilterChain chain2 = new MockFilterChain();
        filter.doFilter(optionsReq, new MockHttpServletResponse(), chain2);
        assertNotNull(chain2.getRequest());
    }

    @Test
    void allowsAllApisWhenMustChangePasswordIsFalse() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(10L, "staff01", "CS_STAFF", null, 3L, false);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_CS_STAFF"))));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/feedbacks");
        request.setServletPath("/api/feedbacks");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest());
    }
}
