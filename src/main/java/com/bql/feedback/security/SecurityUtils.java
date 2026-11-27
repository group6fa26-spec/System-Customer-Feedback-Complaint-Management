package com.bql.feedback.security;

import com.bql.feedback.exception.ForbiddenException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

public final class SecurityUtils {

    private static final Set<String> STAFF_ROLES = Set.of("CS_STAFF", "DEPT_STAFF", "CS_MANAGER");

    private SecurityUtils() {
    }

    public static AuthenticatedUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken
                || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AuthenticationCredentialsNotFoundException("Chưa đăng nhập");
        }
        return user;
    }

    public static Long currentCustomerId() {
        AuthenticatedUser user = currentUser();
        if (!"CUSTOMER".equals(user.role()) || user.customerId() == null) {
            throw new ForbiddenException("Tài khoản không phải cư dân");
        }
        return user.customerId();
    }

    public static Long currentStaffId() {
        AuthenticatedUser user = currentUser();
        if (!isStaff(user) || user.staffId() == null) {
            throw new ForbiddenException("Tài khoản không phải nhân viên");
        }
        return user.staffId();
    }

    public static Long currentActorId() {
        AuthenticatedUser user = currentUser();
        if ("CUSTOMER".equals(user.role()) && user.customerId() != null) {
            return user.customerId();
        }
        if (isStaff(user) && user.staffId() != null) {
            return user.staffId();
        }
        return user.accountId();
    }

    public static String currentRole() {
        return currentUser().role();
    }

    public static boolean isCustomer() {
        return "CUSTOMER".equals(currentRole());
    }

    public static boolean isStaff() {
        return isStaff(currentUser());
    }

    public static boolean isStaff(AuthenticatedUser user) {
        return user != null && user.role() != null && STAFF_ROLES.contains(user.role());
    }
}
