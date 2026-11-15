package com.bql.feedback.repository;

import com.bql.feedback.model.EscalationHistory;
import com.bql.feedback.model.EscalationReason;
import org.springframework.data.repository.Repository;

import java.util.List;

/** Lịch sử chuyển cấp: chỉ thêm và đọc. */
public interface EscalationHistoryRepository extends Repository<EscalationHistory, Long> {
    <S extends EscalationHistory> S save(S history);
    List<EscalationHistory> findByFeedbackIdOrderByEscalatedAtDesc(Long feedbackId);
    boolean existsByFeedbackIdAndReason(Long feedbackId, EscalationReason reason);   // tránh chuyển cấp lặp
    long countByFeedbackId(Long feedbackId);
}
