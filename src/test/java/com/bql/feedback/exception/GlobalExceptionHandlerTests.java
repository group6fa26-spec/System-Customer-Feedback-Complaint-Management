package com.bql.feedback.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returns401ForUnauthorizedAndAuthenticationExceptions() {
        ResponseEntity<ErrorResponse> unauthorized =
                handler.handleUnauthorized(new UnauthorizedException("Sai mật khẩu"));
        assertEquals(HttpStatus.UNAUTHORIZED, unauthorized.getStatusCode());
        assertNotNull(unauthorized.getBody());
        assertEquals("Sai mật khẩu", unauthorized.getBody().message());

        ResponseEntity<ErrorResponse> unauthenticated =
                handler.handleAuthentication(new AuthenticationCredentialsNotFoundException("internal detail"));
        assertEquals(HttpStatus.UNAUTHORIZED, unauthenticated.getStatusCode());
        assertNotNull(unauthenticated.getBody());
        assertEquals("Vui lòng đăng nhập", unauthenticated.getBody().message());
    }

    @Test
    void returns403ForForbiddenAndAccessDeniedExceptions() {
        ResponseEntity<ErrorResponse> forbidden =
                handler.handleForbidden(new ForbiddenException("Không có quyền"));
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
        assertNotNull(forbidden.getBody());
        assertEquals("Không có quyền", forbidden.getBody().message());

        ResponseEntity<ErrorResponse> accessDenied =
                handler.handleAccessDenied(new AccessDeniedException("Access is denied"));
        assertEquals(HttpStatus.FORBIDDEN, accessDenied.getStatusCode());
        assertNotNull(accessDenied.getBody());
        assertEquals("Bạn không có quyền thực hiện thao tác này", accessDenied.getBody().message());
    }

    @Test
    void returns429WithOptionalRetryAfterHeader() {
        ResponseEntity<ErrorResponse> withRetry =
                handler.handleTooManyRequests(new TooManyRequestsException("Thử lại sau", 90));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, withRetry.getStatusCode());
        assertEquals("90", withRetry.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertNotNull(withRetry.getBody());
        assertEquals("Thử lại sau", withRetry.getBody().message());

        ResponseEntity<ErrorResponse> withoutRetry =
                handler.handleTooManyRequests(new TooManyRequestsException("Bị khóa tạm"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, withoutRetry.getStatusCode());
        assertNull(withoutRetry.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
    }
}
