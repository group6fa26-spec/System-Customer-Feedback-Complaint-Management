package com.bql.feedback.dto;

import com.bql.feedback.model.Department;
import com.bql.feedback.model.Role;

public record UpdateAccountRequestDto(
        String fullName,
        Role role,
        Department department,
        Boolean onDuty,
        String phone,
        String email,
        String apartmentCode,
        Boolean verifiedResident
) {
}
