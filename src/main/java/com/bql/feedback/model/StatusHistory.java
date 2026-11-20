package com.bql.feedback.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** CF11 - Ghi lại mọi thay đổi trạng thái. from = null khi tạo mới. */
@Entity
@Table(name = "status_histories")
public class StatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long feedbackId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private FeedbackStatus from;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status")
    private FeedbackStatus to;

    private Long changedById;

    @Enumerated(EnumType.STRING)
    private Role changedByRole;

    @Column(length = 1000)
    private String note;

    private LocalDateTime changedAt;

    protected StatusHistory() {
    }

    public StatusHistory(Long id, Long feedbackId, FeedbackStatus from, FeedbackStatus to,
                         Long changedById, Role changedByRole, String note, LocalDateTime changedAt) {
        this.id = id;
        this.feedbackId = feedbackId;
        this.from = from;
        this.to = to;
        this.changedById = changedById;
        this.changedByRole = changedByRole;
        this.note = note;
        this.changedAt = changedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long feedbackId) { this.feedbackId = feedbackId; }
    public FeedbackStatus getFrom() { return from; }
    public void setFrom(FeedbackStatus from) { this.from = from; }
    public FeedbackStatus getTo() { return to; }
    public void setTo(FeedbackStatus to) { this.to = to; }
    public Long getChangedById() { return changedById; }
    public void setChangedById(Long changedById) { this.changedById = changedById; }
    public Role getChangedByRole() { return changedByRole; }
    public void setChangedByRole(Role changedByRole) { this.changedByRole = changedByRole; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
