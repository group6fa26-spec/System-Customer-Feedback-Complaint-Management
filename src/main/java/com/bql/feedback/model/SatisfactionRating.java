package com.bql.feedback.model;

import java.time.LocalDateTime;

/** CF14 - Đánh giá 1-5 sao. trustWeight (0-1) dùng khi tổng hợp để giảm ảnh hưởng đánh giá đáng ngờ. */
public record SatisfactionRating(Long id, Long feedbackId, Long customerId, int stars, String comment,
                                 LocalDateTime createdAt, boolean flaggedSuspicious, double trustWeight) {
    public SatisfactionRating {
        if (stars < 1 || stars > 5) throw new IllegalArgumentException("stars phải từ 1 đến 5");
    }
}
