package com.bql.feedback.dto;

import java.time.LocalDateTime;

public record FeedbackAttachmentResponseDto(
        Long id,
        Long feedbackId,
        String fileName,
        String fileUrl,
        String contentType,
        long sizeBytes,
        Long uploadedById,
        LocalDateTime uploadedAt
) {
}
