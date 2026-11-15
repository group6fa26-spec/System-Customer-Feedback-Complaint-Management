package com.bql.feedback.repository;

import com.bql.feedback.model.FeedbackCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedbackCategoryRepository extends JpaRepository<FeedbackCategory, Long> {
    Optional<FeedbackCategory> findByCode(String code);
    List<FeedbackCategory> findByActiveTrue();   // danh sách cho form gửi phiếu và AutoClassifier
}
