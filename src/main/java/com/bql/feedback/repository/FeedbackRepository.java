package com.bql.feedback.repository;

import com.bql.feedback.model.Feedback;
import com.bql.feedback.model.FeedbackStatus;
import com.bql.feedback.model.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Cố ý KHÔNG extends JpaRepository để không có deleteById/delete:
 * Business Rule "không xóa vật lý khiếu nại đã tiếp nhận". "Xóa" = Feedback.softDelete() rồi save().
 * Mọi truy vấn đọc đều loại bỏ phiếu đã xóa mềm (deletedAt is null).
 */
public interface FeedbackRepository extends Repository<Feedback, Long> {

    // ---------- Create / Update ----------
    <S extends Feedback> S save(S feedback);

    // ---------- Read ----------
    Optional<Feedback> findByIdAndDeletedAtIsNull(Long id);
    Optional<Feedback> findByCodeAndDeletedAtIsNull(String code);

    /** Cư dân chỉ xem phiếu của mình: ép điều kiện chủ phiếu ngay ở tầng dữ liệu (CF13). */
    Optional<Feedback> findByIdAndCustomerIdAndDeletedAtIsNull(Long id, Long customerId);
    Page<Feedback> findByCustomerIdAndDeletedAtIsNull(Long customerId, Pageable pageable);

    /** Hàng đợi của nhân viên. */
    Page<Feedback> findByAssignedStaffIdAndStatusInAndDeletedAtIsNull(Long staffId, Collection<FeedbackStatus> statuses, Pageable pageable);

    /** Sinh mã phản hồi dạng FB-yyyyMMdd-0001: đếm số phiếu trong ngày (tính cả phiếu đã xóa mềm). */
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    /** CF17 - lọc/tìm kiếm. Tham số nào null thì bỏ qua điều kiện đó. */
    @Query("""
        select f from Feedback f
        where f.deletedAt is null
          and (:customerId is null or f.customerId = :customerId)
          and (:categoryId is null or f.categoryId = :categoryId)
          and (:status is null or f.status = :status)
          and (:priority is null or f.priority = :priority)
          and (:fromDate is null or f.createdAt >= :fromDate)
          and (:toDate is null or f.createdAt < :toDate)
          and (:keyword is null
               or lower(f.title) like lower(concat('%', :keyword, '%'))
               or lower(f.code) like lower(concat('%', :keyword, '%')))
        """)
    Page<Feedback> search(@Param("customerId") Long customerId,
                          @Param("categoryId") Long categoryId,
                          @Param("status") FeedbackStatus status,
                          @Param("priority") Priority priority,
                          @Param("fromDate") LocalDateTime fromDate,
                          @Param("toDate") LocalDateTime toDate,
                          @Param("keyword") String keyword,
                          Pageable pageable);

    // ---------- Phục vụ nghiệp vụ ----------

    /** Phiếu quá hạn phản hồi hoặc quá hạn xử lý mà chưa chuyển cấp (dùng cho quét escalation). */
    @Query("""
        select f from Feedback f
        where f.deletedAt is null
          and f.status not in :finalStatuses
          and f.escalated = false
          and ((f.firstResponseAt is null and f.responseDueAt < :now)
            or (f.resolvedAt is null and f.resolveDueAt < :now))
        """)
    List<Feedback> findOverdueNotEscalated(@Param("now") LocalDateTime now,
                                           @Param("finalStatuses") Collection<FeedbackStatus> finalStatuses);

    /** Ứng viên trùng lặp: cùng danh mục, cùng vị trí, tạo gần đây, phiếu gốc còn mở. */
    @Query("""
        select f from Feedback f
        where f.deletedAt is null
          and f.categoryId = :categoryId
          and lower(f.location) = lower(:location)
          and f.createdAt >= :since
          and f.status not in :finalStatuses
        order by f.createdAt asc
        """)
    List<Feedback> findDuplicateCandidates(@Param("categoryId") Long categoryId,
                                           @Param("location") String location,
                                           @Param("since") LocalDateTime since,
                                           @Param("finalStatuses") Collection<FeedbackStatus> finalStatuses);

    // ---------- Dashboard / báo cáo (CF15, CF16) ----------
    interface StatusCount { FeedbackStatus getStatus(); long getTotal(); }
    interface PriorityCount { Priority getPriority(); long getTotal(); }

    @Query("select f.status as status, count(f) as total from Feedback f where f.deletedAt is null group by f.status")
    List<StatusCount> countGroupByStatus();

    @Query("select f.priority as priority, count(f) as total from Feedback f where f.deletedAt is null group by f.priority")
    List<PriorityCount> countGroupByPriority();

    /** Lấy các phiếu đã giải quyết trong kỳ để service tính thời gian xử lý trung bình, tỷ lệ đúng hạn. */
    List<Feedback> findByResolvedAtBetweenAndDeletedAtIsNull(LocalDateTime from, LocalDateTime to);
}
