package com.bql.feedback.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** CF14 - Đánh giá 1-5 sao. trustWeight (0-1) dùng khi tổng hợp để giảm ảnh hưởng đánh giá đáng ngờ. */
@Entity
@Table(name = "satisfaction_ratings")
public class SatisfactionRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long feedbackId;

    private Long customerId;
    private int stars;

    @Column(length = 2000)
    private String comment;

    private LocalDateTime createdAt;
    private boolean flaggedSuspicious;
    private double trustWeight;

    protected SatisfactionRating() {
    }

    public SatisfactionRating(Long id, Long feedbackId, Long customerId, int stars, String comment,
                              LocalDateTime createdAt, boolean flaggedSuspicious, double trustWeight) {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("stars phải từ 1 đến 5");
        }
        this.id = id;
        this.feedbackId = feedbackId;
        this.customerId = customerId;
        this.stars = stars;
        this.comment = comment;
        this.createdAt = createdAt;
        this.flaggedSuspicious = flaggedSuspicious;
        this.trustWeight = trustWeight;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long feedbackId) { this.feedbackId = feedbackId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public int getStars() { return stars; }
    public void setStars(int stars) {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("stars phải từ 1 đến 5");
        }
        this.stars = stars;
    }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isFlaggedSuspicious() { return flaggedSuspicious; }
    public void setFlaggedSuspicious(boolean flaggedSuspicious) { this.flaggedSuspicious = flaggedSuspicious; }
    public double getTrustWeight() { return trustWeight; }
    public void setTrustWeight(double trustWeight) { this.trustWeight = trustWeight; }
}
