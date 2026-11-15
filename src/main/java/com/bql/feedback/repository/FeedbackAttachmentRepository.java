package com.bql.feedback.repository;

import com.bql.feedback.model.FeedbackAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackAttachmentRepository extends JpaRepository<FeedbackAttachment, Long> {
    List<FeedbackAttachment> findByFeedbackId(Long feedbackId);
    long countByFeedbackId(Long feedbackId);   // giới hạn số file mỗi phiếu
}
