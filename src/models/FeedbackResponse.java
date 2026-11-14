package com.bql.feedback.model;

import java.time.LocalDateTime;

/** CF10/CF11 - Trao đổi với cư dân. internalNote=true thì cư dân không thấy. */
public record FeedbackResponse(Long id, Long feedbackId, Long authorId, Role authorRole,
                               String content, boolean internalNote, boolean solution,
                               LocalDateTime createdAt) { }
