package com.bql.feedback.repository;

import com.bql.feedback.model.SatisfactionRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SatisfactionRatingRepository extends JpaRepository<SatisfactionRating, Long> {
    Optional<SatisfactionRating> findByFeedbackId(Long feedbackId);
    boolean existsByFeedbackId(Long feedbackId);           // mỗi phiếu đánh giá một lần
    List<SatisfactionRating> findByCustomerId(Long customerId);

    @Query("select avg(r.stars) from SatisfactionRating r where r.createdAt >= :from and r.createdAt < :to")
    Double averageStars(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<SatisfactionRating> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);   // để service tính điểm có trọng số
}
