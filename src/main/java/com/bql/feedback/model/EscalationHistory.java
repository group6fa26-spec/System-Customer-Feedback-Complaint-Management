package com.bql.feedback.model;

import java.time.LocalDateTime;

/** CF12 - Lịch sử chuyển cấp. */
public record EscalationHistory(Long id, Long feedbackId, Long fromStaffId, Long toManagerId,
                                EscalationReason reason, int level, String note,
                                LocalDateTime escalatedAt, boolean automatic) { }
