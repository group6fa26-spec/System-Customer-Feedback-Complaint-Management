package com.bql.feedback.util;

import com.bql.feedback.exception.BusinessException;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ContactValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(0|\\+84)[0-9]{9}$");

    private ContactValidationUtil() {
    }

    public static String normalizeAndValidateEmail(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new BusinessException("Email không được để trống");
        }
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException("Email không đúng định dạng");
        }
        return email;
    }

    public static String normalizeAndValidatePhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new BusinessException("Số điện thoại không được để trống");
        }
        String phone = rawPhone.trim();
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException("Số điện thoại không đúng định dạng");
        }
        if (phone.startsWith("+84")) {
            phone = "0" + phone.substring(3);
        }
        return phone;
    }
}
