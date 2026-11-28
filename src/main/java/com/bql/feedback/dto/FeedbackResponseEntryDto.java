package com.bql.feedback.dto;

import java.time.LocalDateTime;

public record FeedbackResponseEntryDto(
        Long id,
        Long feedbackId,
        Long authorId,
        String authorRole,
        String content,
        boolean internalNote,
        boolean solution,
        LocalDateTime createdAt
) {
}
