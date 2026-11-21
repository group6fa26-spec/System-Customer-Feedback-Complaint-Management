package com.bql.feedback.dto;

public record MergeFeedbackRequestDto(
        Long originalFeedbackId,
        Long staffId,
        String note
) {
}
