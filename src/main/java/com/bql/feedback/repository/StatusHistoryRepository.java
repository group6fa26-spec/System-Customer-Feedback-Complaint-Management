package com.bql.feedback.repository;

import com.bql.feedback.model.StatusHistory;
import org.springframework.data.repository.Repository;

import java.util.List;

/** Lịch sử đổi trạng thái: chỉ thêm và đọc (CF11). */
public interface StatusHistoryRepository extends Repository<StatusHistory, Long> {
    <S extends StatusHistory> S save(S history);
    List<StatusHistory> findByFeedbackIdOrderByChangedAtAsc(Long feedbackId);
}
