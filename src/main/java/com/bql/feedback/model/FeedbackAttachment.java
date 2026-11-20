package com.bql.feedback.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** CF05 - Ảnh/bằng chứng. */
@Entity
@Table(name = "feedback_attachments")
public class FeedbackAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long feedbackId;
    private String fileName;
    private String fileUrl;
    private String contentType;
    private long sizeBytes;
    private Long uploadedById;
    private LocalDateTime uploadedAt;

    protected FeedbackAttachment() {
    }

    public FeedbackAttachment(Long id, Long feedbackId, String fileName, String fileUrl,
                              String contentType, long sizeBytes, Long uploadedById,
                              LocalDateTime uploadedAt) {
        this.id = id;
        this.feedbackId = feedbackId;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedById = uploadedById;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long feedbackId) { this.feedbackId = feedbackId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public Long getUploadedById() { return uploadedById; }
    public void setUploadedById(Long uploadedById) { this.uploadedById = uploadedById; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
