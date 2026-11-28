package com.bql.feedback.service;

import com.bql.feedback.dto.AssignFeedbackRequestDto;
import com.bql.feedback.dto.ClassifyFeedbackRequestDto;
import com.bql.feedback.dto.CreateStaffResponseRequestDto;
import com.bql.feedback.dto.EscalateFeedbackRequestDto;
import com.bql.feedback.dto.FeedbackResponseDto;
import com.bql.feedback.dto.FeedbackResponseEntryDto;
import com.bql.feedback.dto.InvestigationNoteRequestDto;
import com.bql.feedback.dto.UpdateFeedbackStatusRequestDto;
import com.bql.feedback.exception.BusinessException;
import com.bql.feedback.exception.ForbiddenException;
import com.bql.feedback.exception.NotFoundException;
import com.bql.feedback.model.ClassifiedBy;
import com.bql.feedback.model.EscalationHistory;
import com.bql.feedback.model.Feedback;
import com.bql.feedback.model.FeedbackAssignment;
import com.bql.feedback.model.FeedbackCategory;
import com.bql.feedback.model.FeedbackResponse;
import com.bql.feedback.model.FeedbackStatus;
import com.bql.feedback.model.Priority;
import com.bql.feedback.model.Role;
import com.bql.feedback.model.Staff;
import com.bql.feedback.model.StatusHistory;
import com.bql.feedback.repository.EscalationHistoryRepository;
import com.bql.feedback.repository.FeedbackAssignmentRepository;
import com.bql.feedback.repository.FeedbackCategoryRepository;
import com.bql.feedback.repository.FeedbackRepository;
import com.bql.feedback.repository.FeedbackResponseRepository;
import com.bql.feedback.repository.StaffRepository;
import com.bql.feedback.repository.StatusHistoryRepository;
import com.bql.feedback.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class StaffComplaintService {

    private static final int MAX_NOTE_LENGTH = 1000;
    private static final int MAX_RESPONSE_LENGTH = 4000;

    private final FeedbackRepository feedbackRepository;
    private final FeedbackService feedbackService;
    private final FeedbackCategoryRepository categoryRepository;
    private final StaffRepository staffRepository;
    private final FeedbackAssignmentRepository assignmentRepository;
    private final FeedbackResponseRepository responseRepository;
    private final EscalationHistoryRepository escalationRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public StaffComplaintService(
            FeedbackRepository feedbackRepository,
            FeedbackService feedbackService,
            FeedbackCategoryRepository categoryRepository,
            StaffRepository staffRepository,
            FeedbackAssignmentRepository assignmentRepository,
            FeedbackResponseRepository responseRepository,
            EscalationHistoryRepository escalationRepository,
            StatusHistoryRepository statusHistoryRepository
    ) {
        this.feedbackRepository = feedbackRepository;
        this.feedbackService = feedbackService;
        this.categoryRepository = categoryRepository;
        this.staffRepository = staffRepository;
        this.assignmentRepository = assignmentRepository;
        this.responseRepository = responseRepository;
        this.escalationRepository = escalationRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    @Transactional(readOnly = true)
    public Page<FeedbackResponseDto> search(
            AuthenticatedUser actor,
            Long categoryId,
            FeedbackStatus status,
            Priority priority,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String keyword,
            Pageable pageable
    ) {
        requireRole(actor, Role.CS_STAFF, Role.CS_MANAGER);
        if (fromDate != null && toDate != null && !fromDate.isBefore(toDate)) {
            throw new BusinessException("fromDate phải nhỏ hơn toDate");
        }
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return feedbackRepository.search(null, categoryId, status, priority, fromDate, toDate,
                        normalizedKeyword, pageable)
                .map(feedbackService::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<FeedbackResponseDto> getAssignedComplaints(AuthenticatedUser actor, Pageable pageable) {
        requireRole(actor, Role.DEPT_STAFF);
        List<FeedbackResponseDto> assigned = assignmentRepository
                .findByStaffIdAndActiveTrue(actor.staffId()).stream()
                .sorted(Comparator.comparing(
                        FeedbackAssignment::getAssignedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(assignment -> feedbackRepository.findByIdAndDeletedAtIsNull(assignment.getFeedbackId())
                        .orElse(null))
                .filter(Objects::nonNull)
                .map(feedbackService::toResponseDto)
                .toList();

        int start = (int) Math.min(pageable.getOffset(), assigned.size());
        int end = Math.min(start + pageable.getPageSize(), assigned.size());
        return new PageImpl<>(assigned.subList(start, end), pageable, assigned.size());
    }

    @Transactional(readOnly = true)
    public FeedbackResponseDto getAssignedComplaint(Long feedbackId, AuthenticatedUser actor) {
        requireAssignedDepartmentStaff(feedbackId, actor);
        return feedbackService.toResponseDto(findFeedback(feedbackId));
    }

    @Transactional
    public FeedbackResponseDto classify(
            Long feedbackId,
            ClassifyFeedbackRequestDto request,
            AuthenticatedUser actor
    ) {
        requireRole(actor, Role.CS_STAFF);
        if (request == null || request.categoryId() == null || request.priority() == null) {
            throw new BusinessException("categoryId và priority là bắt buộc");
        }
        FeedbackCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục: " + request.categoryId()));
        if (!category.isActive()) {
            throw new BusinessException("Không thể phân loại theo danh mục đã ngừng hoạt động");
        }

        Feedback feedback = findFeedback(feedbackId);
        ensureOpen(feedback);
        feedback.setCategoryId(category.getId());
        feedback.setClassifiedBy(ClassifiedBy.MANUAL);
        feedback.applyPriority(request.priority());
        return feedbackService.toResponseDto(feedbackRepository.save(feedback));
    }

    @Transactional
    public FeedbackResponseDto assign(
            Long feedbackId,
            AssignFeedbackRequestDto request,
            AuthenticatedUser actor
    ) {
        requireRole(actor, Role.CS_STAFF);
        if (request == null || request.staffId() == null) {
            throw new BusinessException("staffId là bắt buộc");
        }
        validateNote(request.note());
        Feedback feedback = findFeedback(feedbackId);
        ensureOpen(feedback);

        Staff assignee = staffRepository.findById(request.staffId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên: " + request.staffId()));
        if (!assignee.isActive() || assignee.getRole() != Role.DEPT_STAFF) {
            throw new BusinessException("Chỉ có thể phân công cho nhân viên bộ phận đang hoạt động");
        }
        if (Objects.equals(feedback.getAboutStaffId(), assignee.getId())) {
            throw new BusinessException("Không thể phân công khiếu nại cho nhân viên bị phản ánh");
        }

        assignmentRepository.findByFeedbackIdAndActiveTrue(feedbackId).ifPresent(current -> {
            current.setActive(false);
            assignmentRepository.save(current);
        });

        LocalDateTime now = LocalDateTime.now();
        FeedbackStatus previousStatus = feedback.getStatus();
        if (previousStatus == FeedbackStatus.NEW) {
            feedback.changeStatus(FeedbackStatus.ASSIGNED, now);
            statusHistoryRepository.save(new StatusHistory(
                    null, feedbackId, previousStatus, feedback.getStatus(), actor.staffId(),
                    Role.CS_STAFF, "Phân công xử lý", now));
        }
        feedback.setAssignedStaffId(assignee.getId());
        Feedback saved = feedbackRepository.save(feedback);
        assignmentRepository.save(new FeedbackAssignment(
                null, feedbackId, assignee.getId(), assignee.getDepartment(), actor.staffId(),
                now, request.dueAt(), true, trimToNull(request.note())));
        return feedbackService.toResponseDto(saved);
    }

    @Transactional
    public FeedbackResponseEntryDto respond(
            Long feedbackId,
            CreateStaffResponseRequestDto request,
            AuthenticatedUser actor
    ) {
        requireRole(actor, Role.CS_STAFF);
        validateContent(request == null ? null : request.content());
        Feedback feedback = findFeedback(feedbackId);
        ensureOpen(feedback);
        if (request.solution() && feedback.isRequiresManagerApproval() && feedback.getApprovedAt() == null) {
            throw new BusinessException("Phương án xử lý Critical cần Manager duyệt trước");
        }

        LocalDateTime now = LocalDateTime.now();
        if (request.solution()) {
            feedback.setResolutionSummary(request.content().trim());
        }
        feedback.markFirstResponse(now);
        feedbackRepository.save(feedback);
        FeedbackResponse response = responseRepository.save(new FeedbackResponse(
                null, feedbackId, actor.staffId(), Role.CS_STAFF, request.content().trim(),
                false, request.solution(), now));
        return toDto(response);
    }

    @Transactional
    public FeedbackResponseDto updateStatus(
            Long feedbackId,
            UpdateFeedbackStatusRequestDto request,
            AuthenticatedUser actor
    ) {
        requireRole(actor, Role.CS_STAFF, Role.DEPT_STAFF);
        if (request == null || request.status() == null) {
            throw new BusinessException("status là bắt buộc");
        }
        if (Role.DEPT_STAFF.name().equals(actor.role())) {
            requireAssignedDepartmentStaff(feedbackId, actor);
            if (!List.of(FeedbackStatus.IN_PROGRESS, FeedbackStatus.WAITING_INFO,
                    FeedbackStatus.PENDING_APPROVAL, FeedbackStatus.RESOLVED).contains(request.status())) {
                throw new ForbiddenException("Nhân viên bộ phận không được chuyển sang trạng thái này");
            }
        }
        validateNote(request.note());
        if (request.resolutionSummary() != null) {
            validateContent(request.resolutionSummary());
        }

        Feedback feedback = findFeedback(feedbackId);
        FeedbackStatus previousStatus = feedback.getStatus();
        if (request.resolutionSummary() != null) {
            feedback.setResolutionSummary(trimToNull(request.resolutionSummary()));
        }
        LocalDateTime now = LocalDateTime.now();
        feedback.changeStatus(request.status(), now);
        Feedback saved = feedbackRepository.save(feedback);
        statusHistoryRepository.save(new StatusHistory(
                null, feedbackId, previousStatus, saved.getStatus(), actor.staffId(),
                Role.valueOf(actor.role()), trimToNull(request.note()), now));

        if (request.status() == FeedbackStatus.RESOLVED || request.status().isFinal()) {
            assignmentRepository.findByFeedbackIdAndActiveTrue(feedbackId).ifPresent(assignment -> {
                assignment.setActive(false);
                assignmentRepository.save(assignment);
            });
        }
        return feedbackService.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponseEntryDto> getResponses(Long feedbackId, AuthenticatedUser actor) {
        if (actor == null || actor.role() == null) {
            throw new ForbiddenException("Bạn không có quyền thực hiện thao tác này");
        }
        if (Role.DEPT_STAFF.name().equals(actor.role())) {
            requireAssignedDepartmentStaff(feedbackId, actor);
        } else {
            requireRole(actor, Role.CS_STAFF);
        }
        findFeedback(feedbackId);
        return responseRepository.findByFeedbackIdOrderByCreatedAtAsc(feedbackId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public FeedbackResponseEntryDto addInvestigationNote(
            Long feedbackId,
            InvestigationNoteRequestDto request,
            AuthenticatedUser actor
    ) {
        requireAssignedDepartmentStaff(feedbackId, actor);
        validateContent(request == null ? null : request.content());
        FeedbackResponse response = responseRepository.save(new FeedbackResponse(
                null, feedbackId, actor.staffId(), Role.DEPT_STAFF, request.content().trim(),
                true, false, LocalDateTime.now()));
        return toDto(response);
    }

    @Transactional
    public FeedbackResponseDto escalate(
            Long feedbackId,
            EscalateFeedbackRequestDto request,
            AuthenticatedUser actor
    ) {
        requireRole(actor, Role.CS_STAFF);
        if (request == null || request.reason() == null) {
            throw new BusinessException("reason là bắt buộc");
        }
        validateNote(request.note());
        Feedback feedback = findFeedback(feedbackId);
        ensureOpen(feedback);
        if (escalationRepository.existsByFeedbackIdAndReason(feedbackId, request.reason())) {
            throw new BusinessException("Phiếu đã được chuyển cấp với lý do này");
        }
        Staff manager = staffRepository.findByRoleAndActiveTrue(Role.CS_MANAGER).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("Không có Manager đang hoạt động để nhận chuyển cấp"));

        int level = Math.toIntExact(escalationRepository.countByFeedbackId(feedbackId) + 1);
        LocalDateTime now = LocalDateTime.now();
        feedback.setEscalated(true);
        feedback.setEscalationLevel(level);
        feedbackRepository.save(feedback);
        escalationRepository.save(new EscalationHistory(
                null, feedbackId, actor.staffId(), manager.getId(), request.reason(), level,
                trimToNull(request.note()), now, false));
        return feedbackService.toResponseDto(feedback);
    }

    private Feedback requireAssignedDepartmentStaff(Long feedbackId, AuthenticatedUser actor) {
        requireRole(actor, Role.DEPT_STAFF);
        if (actor.staffId() == null) {
            throw new ForbiddenException("Tài khoản không có mã nhân viên");
        }
        FeedbackAssignment assignment = assignmentRepository.findByFeedbackIdAndActiveTrue(feedbackId)
                .orElseThrow(() -> new ForbiddenException("Phiếu chưa được phân công cho bạn"));
        if (!Objects.equals(assignment.getStaffId(), actor.staffId())) {
            throw new ForbiddenException("Bạn chỉ có thể xử lý phiếu được phân công cho mình");
        }
        return findFeedback(feedbackId);
    }

    private void requireRole(AuthenticatedUser actor, Role... allowedRoles) {
        if (actor == null || actor.role() == null || actor.staffId() == null
                || List.of(allowedRoles).stream().noneMatch(role -> role.name().equals(actor.role()))) {
            throw new ForbiddenException("Bạn không có quyền thực hiện thao tác này");
        }
    }

    private Feedback findFeedback(Long feedbackId) {
        return feedbackRepository.findByIdAndDeletedAtIsNull(feedbackId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu với ID: " + feedbackId));
    }

    private void ensureOpen(Feedback feedback) {
        if (feedback.getStatus().isFinal() || feedback.getStatus() == FeedbackStatus.RESOLVED) {
            throw new BusinessException("Không thể thay đổi phiếu đã hoàn tất xử lý");
        }
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("Nội dung không được để trống");
        }
        if (content.length() > MAX_RESPONSE_LENGTH) {
            throw new BusinessException("Nội dung không được vượt quá 4000 ký tự");
        }
    }

    private void validateNote(String note) {
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            throw new BusinessException("Ghi chú không được vượt quá 1000 ký tự");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private FeedbackResponseEntryDto toDto(FeedbackResponse response) {
        return new FeedbackResponseEntryDto(
                response.getId(), response.getFeedbackId(), response.getAuthorId(),
                response.getAuthorRole() == null ? null : response.getAuthorRole().name(),
                response.getContent(), response.isInternalNote(), response.isSolution(), response.getCreatedAt());
    }
}
