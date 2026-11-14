package com.bql.feedback.model;

import java.time.LocalDateTime;

/** CF05 - Ảnh/bằng chứng. */
public record FeedbackAttachment(Long id, Long feedbackId, String fileName, String fileUrl,
                                 String contentType, long sizeBytes, Long uploadedById,
                                 LocalDateTime uploadedAt) { }
