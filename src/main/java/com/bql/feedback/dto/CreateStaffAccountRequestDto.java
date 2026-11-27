package com.bql.feedback.dto;

import com.bql.feedback.model.Department;
import com.bql.feedback.model.Role;

public record CreateStaffAccountRequestDto(
        String username,
        String password,
        String fullName,
        Role role,
        Department department,
        Boolean onDuty
) {
}
