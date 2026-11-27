package com.bql.feedback.security;

import com.bql.feedback.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Chặn toàn bộ API nghiệp vụ khi JWT có cờ mustChangePassword = true,
 * chỉ cho phép gọi /api/auth/change-password (và các route auth công khai / OPTIONS).
 * Không gắn @Component để tránh Spring Boot đăng ký lặp ngoài SecurityFilterChain.
 */
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private static final String CHANGE_PASSWORD_PATH = "/api/auth/change-password";
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.isAuthenticated()
                && auth.getPrincipal() instanceof AuthenticatedUser user
                && user.mustChangePassword()
                && !isAllowedWhenPasswordChangeRequired(request)) {
            writeForbiddenResponse(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAllowedWhenPasswordChangeRequired(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        if (path == null || !path.startsWith("/api/")) {
            return true;
        }
        return CHANGE_PASSWORD_PATH.equals(path)
                || LOGIN_PATH.equals(path)
                || REGISTER_PATH.equals(path);
    }

    private void writeForbiddenResponse(HttpServletResponse response) throws IOException {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.FORBIDDEN,
                "Bạn phải đổi mật khẩu trước khi sử dụng hệ thống"
        );
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        String json = "{\"timestamp\":\"" + body.timestamp()
                + "\",\"status\":" + body.status()
                + ",\"error\":\"" + body.error()
                + "\",\"message\":\"" + body.message() + "\"}";
        response.getWriter().write(json);
    }
}
