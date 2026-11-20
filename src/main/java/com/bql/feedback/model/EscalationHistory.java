package com.bql.feedback.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** CF12 - Lịch sử chuyển cấp. */
@Entity
@Table(name = "escalation_histories")
public class EscalationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long feedbackId;
    private Long fromStaffId;
    private Long toManagerId;

    @Enumerated(EnumType.STRING)
    private EscalationReason reason;

    @Column(name = "escalation_level")
    private int level;

    @Column(length = 1000)
    private String note;

    private LocalDateTime escalatedAt;
    private boolean automatic;

    protected EscalationHistory() {
    }

    public EscalationHistory(Long id, Long feedbackId, Long fromStaffId, Long toManagerId,
                             EscalationReason reason, int level, String note,
                             LocalDateTime escalatedAt, boolean automatic) {
        this.id = id;
        this.feedbackId = feedbackId;
        this.fromStaffId = fromStaffId;
        this.toManagerId = toManagerId;
        this.reason = reason;
        this.level = level;
        this.note = note;
        this.escalatedAt = escalatedAt;
        this.automatic = automatic;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long feedbackId) { this.feedbackId = feedbackId; }
    public Long getFromStaffId() { return fromStaffId; }
    public void setFromStaffId(Long fromStaffId) { this.fromStaffId = fromStaffId; }
    public Long getToManagerId() { return toManagerId; }
    public void setToManagerId(Long toManagerId) { this.toManagerId = toManagerId; }
    public EscalationReason getReason() { return reason; }
    public void setReason(EscalationReason reason) { this.reason = reason; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getEscalatedAt() { return escalatedAt; }
    public void setEscalatedAt(LocalDateTime escalatedAt) { this.escalatedAt = escalatedAt; }
    public boolean isAutomatic() { return automatic; }
    public void setAutomatic(boolean automatic) { this.automatic = automatic; }
}
