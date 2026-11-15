package com.bql.feedback.model;

import java.time.LocalDateTime;

/** CF11 - Ghi lại mọi thay đổi trạng thái. from = null khi tạo mới. */
public record StatusHistory(Long id, Long feedbackId, FeedbackStatus from, FeedbackStatus to,
                            Long changedById, Role changedByRole, String note, LocalDateTime changedAt) { }
