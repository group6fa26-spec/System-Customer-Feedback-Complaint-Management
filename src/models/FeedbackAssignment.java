package com.bql.feedback.model;

import java.time.LocalDateTime;

/** CF08 - Phân công. active=false khi đã chuyển cho người khác. */
public record FeedbackAssignment(Long id, Long feedbackId, Long staffId, Department department,
                                 Long assignedById, LocalDateTime assignedAt, LocalDateTime dueAt,
                                 boolean active, String note) { }
