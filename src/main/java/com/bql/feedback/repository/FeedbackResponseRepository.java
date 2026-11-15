package com.bql.feedback.repository;

import com.bql.feedback.model.FeedbackResponse;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** Trao đổi là bằng chứng nên chỉ thêm và đọc, không sửa/xóa. */
public interface FeedbackResponseRepository extends Repository<FeedbackResponse, Long> {
    <S extends FeedbackResponse> S save(S response);
    Optional<FeedbackResponse> findById(Long id);

    /** Cho nhân viên: toàn bộ kể cả ghi chú nội bộ. */
    List<FeedbackResponse> findByFeedbackIdOrderByCreatedAtAsc(Long feedbackId);
    /** Cho cư dân: ẩn ghi chú nội bộ. */
    List<FeedbackResponse> findByFeedbackIdAndInternalNoteFalseOrderByCreatedAtAsc(Long feedbackId);
    Optional<FeedbackResponse> findFirstByFeedbackIdAndSolutionTrueOrderByCreatedAtDesc(Long feedbackId);
}
