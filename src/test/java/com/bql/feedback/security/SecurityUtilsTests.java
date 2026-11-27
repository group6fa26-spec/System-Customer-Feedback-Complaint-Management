package com.bql.feedback.security;

import com.bql.feedback.exception.ForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityUtilsTests {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unauthenticatedOrAnonymousThrowsAuthenticationCredentialsNotFound() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, SecurityUtils::currentUser);

        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);
        assertThrows(AuthenticationCredentialsNotFoundException.class, SecurityUtils::currentUser);

        UsernamePasswordAuthenticationToken unauthenticatedToken =
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(1L, "an", "CUSTOMER", 10L, null), null);
        SecurityContextHolder.getContext().setAuthentication(unauthenticatedToken);
        assertThrows(AuthenticationCredentialsNotFoundException.class, SecurityUtils::currentUser);
    }

    @Test
    void customerPrincipalReturnsCustomerIdAndBlocksStaffId() {
        AuthenticatedUser customer = new AuthenticatedUser(1L, "an@gmail.com", "CUSTOMER", 10L, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        customer, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        assertEquals(customer, SecurityUtils.currentUser());
        assertEquals(10L, SecurityUtils.currentCustomerId());
        assertEquals(10L, SecurityUtils.currentActorId());
        assertEquals("CUSTOMER", SecurityUtils.currentRole());
        assertTrue(SecurityUtils.isCustomer());
        assertFalse(SecurityUtils.isStaff());
        assertThrows(ForbiddenException.class, SecurityUtils::currentStaffId);
    }

    @Test
    void staffPrincipalReturnsStaffIdAndBlocksCustomerId() {
        AuthenticatedUser manager = new AuthenticatedUser(2L, "manager@bql.vn", "CS_MANAGER", null, 5L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        manager, null, List.of(new SimpleGrantedAuthority("ROLE_CS_MANAGER"))));

        assertEquals(manager, SecurityUtils.currentUser());
        assertEquals(5L, SecurityUtils.currentStaffId());
        assertEquals(5L, SecurityUtils.currentActorId());
        assertEquals("CS_MANAGER", SecurityUtils.currentRole());
        assertFalse(SecurityUtils.isCustomer());
        assertTrue(SecurityUtils.isStaff());
        assertThrows(ForbiddenException.class, SecurityUtils::currentCustomerId);
    }
}
