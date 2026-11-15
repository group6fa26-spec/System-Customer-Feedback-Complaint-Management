package com.bql.feedback.model;

import java.time.*;

/** Phiếu phản hồi/khiếu nại - thực thể trung tâm (CF04, CF06, CF07, CF09...). */
public class Feedback {

    private Long id;
    private String code; // CF06 - mã tra cứu duy nhất, vd FB-20261114-0001
    private Long customerId;
    private FeedbackType type;
    private Long categoryId; // Danh mục chính thức (sau khi xác nhận)
    private Long autoCategoryId; // Danh mục do hệ thống gợi ý
    private ClassifiedBy classifiedBy;
    private double autoConfidence; // Độ tin cậy gợi ý 0-1
    private String title;
    private String content;
    private String serviceName; // CF04 - dịch vụ liên quan
    private String location; // Vị trí: block/tầng/khu vực
    private String apartmentCode;
    private Priority priority;
    private FeedbackStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime firstResponseAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private LocalDateTime responseDueAt; // Hạn phản hồi theo SLA
    private LocalDateTime resolveDueAt; // Hạn xử lý xong theo SLA
    private boolean escalated;
    private int escalationLevel;
    private int reopenCount; // Đếm reopen để chống reopen lặp lại
    private Long duplicateOfId; // Trỏ tới phiếu gốc nếu trùng
    private int duplicateCount; // Số phiếu trùng đã gộp vào phiếu này
    private boolean afterHours; // Gửi ngoài giờ hành chính
    private boolean missingInfo; // Thiếu thông tin / không phân loại được
    private boolean containsAbusiveLanguage; // Có ngôn từ xúc phạm/đe dọa
    private boolean suspectedFake; // Nghi phản ánh giả, chờ xác minh
    private Long aboutStaffId; // Phản ánh về thái độ nhân viên nào (không giao cho chính người đó xử lý)
    private Long assignedStaffId; // Người đang phụ trách
    private boolean requiresManagerApproval; // Critical: Manager duyệt trước khi gửi kết quả
    private Long approvedByManagerId;
    private LocalDateTime approvedAt;
    private String resolutionSummary; // Giải pháp - bắt buộc khi Resolved
    private LocalDateTime deletedAt; // Xóa mềm - không xóa vật lý khiếu nại đã tiếp nhận

    public Feedback(String code, Long customerId, FeedbackType type, String title, String content,
                     LocalDateTime createdAt) {
        this.code = code; this.customerId = customerId; this.type = type;
        this.title = title; this.content = content;
        this.createdAt = createdAt; this.updatedAt = createdAt;
        this.status = FeedbackStatus.NEW; this.priority = Priority.NORMAL;
        this.afterHours = isOutsideOfficeHours(createdAt);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public FeedbackType getType() { return type; }
    public void setType(FeedbackType type) { this.type = type; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getAutoCategoryId() { return autoCategoryId; }
    public void setAutoCategoryId(Long autoCategoryId) { this.autoCategoryId = autoCategoryId; }
    public ClassifiedBy getClassifiedBy() { return classifiedBy; }
    public void setClassifiedBy(ClassifiedBy classifiedBy) { this.classifiedBy = classifiedBy; }
    public double getAutoConfidence() { return autoConfidence; }
    public void setAutoConfidence(double autoConfidence) { this.autoConfidence = autoConfidence; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getApartmentCode() { return apartmentCode; }
    public void setApartmentCode(String apartmentCode) { this.apartmentCode = apartmentCode; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public FeedbackStatus getStatus() { return status; }
    public void setStatus(FeedbackStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getFirstResponseAt() { return firstResponseAt; }
    public void setFirstResponseAt(LocalDateTime firstResponseAt) { this.firstResponseAt = firstResponseAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public LocalDateTime getResponseDueAt() { return responseDueAt; }
    public void setResponseDueAt(LocalDateTime responseDueAt) { this.responseDueAt = responseDueAt; }
    public LocalDateTime getResolveDueAt() { return resolveDueAt; }
    public void setResolveDueAt(LocalDateTime resolveDueAt) { this.resolveDueAt = resolveDueAt; }
    public boolean isEscalated() { return escalated; }
    public void setEscalated(boolean escalated) { this.escalated = escalated; }
    public int getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(int escalationLevel) { this.escalationLevel = escalationLevel; }
    public int getReopenCount() { return reopenCount; }
    public void setReopenCount(int reopenCount) { this.reopenCount = reopenCount; }
    public Long getDuplicateOfId() { return duplicateOfId; }
    public void setDuplicateOfId(Long duplicateOfId) { this.duplicateOfId = duplicateOfId; }
    public int getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; }
    public boolean isAfterHours() { return afterHours; }
    public void setAfterHours(boolean afterHours) { this.afterHours = afterHours; }
    public boolean isMissingInfo() { return missingInfo; }
    public void setMissingInfo(boolean missingInfo) { this.missingInfo = missingInfo; }
    public boolean isContainsAbusiveLanguage() { return containsAbusiveLanguage; }
    public void setContainsAbusiveLanguage(boolean containsAbusiveLanguage) { this.containsAbusiveLanguage = containsAbusiveLanguage; }
    public boolean isSuspectedFake() { return suspectedFake; }
    public void setSuspectedFake(boolean suspectedFake) { this.suspectedFake = suspectedFake; }
    public Long getAboutStaffId() { return aboutStaffId; }
    public void setAboutStaffId(Long aboutStaffId) { this.aboutStaffId = aboutStaffId; }
    public Long getAssignedStaffId() { return assignedStaffId; }
    public void setAssignedStaffId(Long assignedStaffId) { this.assignedStaffId = assignedStaffId; }
    public boolean isRequiresManagerApproval() { return requiresManagerApproval; }
    public void setRequiresManagerApproval(boolean requiresManagerApproval) { this.requiresManagerApproval = requiresManagerApproval; }
    public Long getApprovedByManagerId() { return approvedByManagerId; }
    public void setApprovedByManagerId(Long approvedByManagerId) { this.approvedByManagerId = approvedByManagerId; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getResolutionSummary() { return resolutionSummary; }
    public void setResolutionSummary(String resolutionSummary) { this.resolutionSummary = resolutionSummary; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    // ---- Giả định: giờ hành chính BQL 08:00-17:00 (chỉnh lại theo thực tế) ----
    public static final LocalTime OFFICE_START = LocalTime.of(8, 0);
    public static final LocalTime OFFICE_END = LocalTime.of(17, 0);
    public static final int MAX_REOPEN = 3;

    public static boolean isOutsideOfficeHours(LocalDateTime t) {
        LocalTime time = t.toLocalTime();
        return time.isBefore(OFFICE_START) || !time.isBefore(OFFICE_END);
    }

    /** Gán mức ưu tiên và tính hạn SLA (SLA chạy 24/7, không dừng ngoài giờ). */
    public void applyPriority(Priority p) {
        this.priority = p;
        this.responseDueAt = createdAt.plusMinutes(p.getResponseMinutes());
        this.resolveDueAt = createdAt.plusMinutes(p.getResolveMinutes());
        this.requiresManagerApproval = (p == Priority.CRITICAL && type == FeedbackType.COMPLAINT);
    }

    public boolean isResponseOverdue(LocalDateTime now) {
        if (responseDueAt == null) return false;
        return firstResponseAt == null ? now.isAfter(responseDueAt) : firstResponseAt.isAfter(responseDueAt);
    }

    public boolean isResolveOverdue(LocalDateTime now) {
        if (resolveDueAt == null || status.isFinal()) return false;
        return resolvedAt == null ? now.isAfter(resolveDueAt) : resolvedAt.isAfter(resolveDueAt);
    }

    public boolean isOverdue(LocalDateTime now) { return isResponseOverdue(now) || isResolveOverdue(now); }

    public void markFirstResponse(LocalDateTime at) { if (firstResponseAt == null) firstResponseAt = at; }

    public void approveByManager(Long managerId, LocalDateTime at) {
        this.approvedByManagerId = managerId; this.approvedAt = at;
    }

    /** Chuyển trạng thái, kiểm tra Business Rules 3, 4, 6. */
    public void changeStatus(FeedbackStatus to, LocalDateTime at) {
        boolean simpleClose = type.isSimpleFlow() && to == FeedbackStatus.CLOSED && !status.isFinal();
        if (!status.canMoveTo(to) && !simpleClose)
            throw new IllegalStateException("Không thể chuyển " + status + " -> " + to);
        if (to == FeedbackStatus.RESOLVED) {
            if (resolutionSummary == null || resolutionSummary.isBlank())
                throw new IllegalStateException("Resolved phải có giải pháp");
            if (requiresManagerApproval && approvedAt == null)
                throw new IllegalStateException("Khiếu nại Critical cần Manager duyệt trước");
            resolvedAt = at;
        }
        if (to == FeedbackStatus.REOPENED) {
            if (reopenCount >= MAX_REOPEN) throw new IllegalStateException("Reopen quá " + MAX_REOPEN + " lần, cần Manager xử lý");
            reopenCount++; resolvedAt = null;
        }
        if (to == FeedbackStatus.CLOSED) closedAt = at;
        this.status = to;
        this.updatedAt = at;
    }

    /** Business Rule 5: chỉ đánh giá sau khi có kết quả xử lý. */
    public boolean canBeRated() {
        return (status == FeedbackStatus.RESOLVED || status == FeedbackStatus.CLOSED)
                && resolutionSummary != null && !resolutionSummary.isBlank();
    }

    /** Phát hiện trùng: cùng danh mục + cùng vị trí + gần nhau về thời gian + phiếu gốc còn mở. */
    public boolean isPossibleDuplicateOf(Feedback o, Duration window) {
        return o != this && categoryId != null && categoryId.equals(o.categoryId)
                && location != null && location.equalsIgnoreCase(o.location)
                && !o.status.isFinal()
                && Duration.between(o.createdAt, createdAt).abs().compareTo(window) <= 0;
    }

    public void markDuplicateOf(Feedback original, LocalDateTime at) {
        this.duplicateOfId = original.id;
        original.duplicateCount++;
        changeStatus(FeedbackStatus.MERGED, at);
    }

    /** Business Rule 2: xóa mềm. */
    public void softDelete(LocalDateTime at) { this.deletedAt = at; }
    public boolean isDeleted() { return deletedAt != null; }
}
