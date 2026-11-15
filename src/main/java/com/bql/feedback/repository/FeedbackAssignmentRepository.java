package com.bql.feedback.repository;

import com.bql.feedback.model.FeedbackAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedbackAssignmentRepository extends JpaRepository<FeedbackAssignment, Long> {
    List<FeedbackAssignment> findByFeedbackIdOrderByAssignedAtDesc(Long feedbackId);
    Optional<FeedbackAssignment> findByFeedbackIdAndActiveTrue(Long feedbackId);   // người đang phụ trách
    List<FeedbackAssignment> findByStaffIdAndActiveTrue(Long staffId);
}
