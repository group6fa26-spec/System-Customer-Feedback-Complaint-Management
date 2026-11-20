package com.bql.feedback.dto;

import java.util.List;

public record FeedbackCategoryResponseDto(
        Long id,
        String code,
        String name,
        String group,
        String defaultPriority,
        List<String> keywords,
        boolean active
) {
}
