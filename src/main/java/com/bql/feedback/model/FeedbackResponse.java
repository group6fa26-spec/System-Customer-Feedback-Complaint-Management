package com.bql.feedback.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** CF10/CF11 - Trao đổi với cư dân. internalNote=true thì cư dân không thấy. */
@Entity
@Table(name = "feedback_responses")
public class FeedbackResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long feedbackId;
    private Long authorId;

    @Enumerated(EnumType.STRING)
    private Role authorRole;

    @Column(length = 4000)
    private String content;

    private boolean internalNote;
    private boolean solution;
    private LocalDateTime createdAt;

    protected FeedbackResponse() {
    }

    public FeedbackResponse(Long id, Long feedbackId, Long authorId, Role authorRole,
                            String content, boolean internalNote, boolean solution,
                            LocalDateTime createdAt) {
        this.id = id;
        this.feedbackId = feedbackId;
        this.authorId = authorId;
        this.authorRole = authorRole;
        this.content = content;
        this.internalNote = internalNote;
        this.solution = solution;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long feedbackId) { this.feedbackId = feedbackId; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public Role getAuthorRole() { return authorRole; }
    public void setAuthorRole(Role authorRole) { this.authorRole = authorRole; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isInternalNote() { return internalNote; }
    public void setInternalNote(boolean internalNote) { this.internalNote = internalNote; }
    public boolean isSolution() { return solution; }
    public void setSolution(boolean solution) { this.solution = solution; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
