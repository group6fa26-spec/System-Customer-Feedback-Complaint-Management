package com.bql.feedback.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** CF08 - Phân công. active=false khi đã chuyển cho người khác. */
@Entity
@Table(name = "feedback_assignments")
public class FeedbackAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long feedbackId;
    private Long staffId;

    @Enumerated(EnumType.STRING)
    private Department department;

    private Long assignedById;
    private LocalDateTime assignedAt;
    private LocalDateTime dueAt;
    private boolean active;

    @Column(length = 1000)
    private String note;

    protected FeedbackAssignment() {
    }

    public FeedbackAssignment(Long id, Long feedbackId, Long staffId, Department department,
                              Long assignedById, LocalDateTime assignedAt, LocalDateTime dueAt,
                              boolean active, String note) {
        this.id = id;
        this.feedbackId = feedbackId;
        this.staffId = staffId;
        this.department = department;
        this.assignedById = assignedById;
        this.assignedAt = assignedAt;
        this.dueAt = dueAt;
        this.active = active;
        this.note = note;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long feedbackId) { this.feedbackId = feedbackId; }
    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Long getAssignedById() { return assignedById; }
    public void setAssignedById(Long assignedById) { this.assignedById = assignedById; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
