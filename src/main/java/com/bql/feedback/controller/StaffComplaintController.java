package com.bql.feedback.controller;

import com.bql.feedback.dto.AssignFeedbackRequestDto;
import com.bql.feedback.dto.ClassifyFeedbackRequestDto;
import com.bql.feedback.dto.CreateStaffResponseRequestDto;
import com.bql.feedback.dto.EscalateFeedbackRequestDto;
import com.bql.feedback.dto.FeedbackResponseDto;
import com.bql.feedback.dto.FeedbackResponseEntryDto;
import com.bql.feedback.dto.InvestigationNoteRequestDto;
import com.bql.feedback.dto.UpdateFeedbackStatusRequestDto;
import com.bql.feedback.exception.BusinessException;
import com.bql.feedback.model.FeedbackStatus;
import com.bql.feedback.model.Priority;
import com.bql.feedback.security.SecurityUtils;
import com.bql.feedback.service.StaffComplaintService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/staff/complaints")
public class StaffComplaintController {

    private final StaffComplaintService complaintService;

    public StaffComplaintController(StaffComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CS_STAFF', 'CS_MANAGER')")
    public ResponseEntity<Page<FeedbackResponseDto>> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) FeedbackStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = createPageable(page, size);
        return ResponseEntity.ok(complaintService.search(
                SecurityUtils.currentUser(), categoryId, status, priority, fromDate, toDate, keyword, pageable));
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('DEPT_STAFF')")
    public ResponseEntity<Page<FeedbackResponseDto>> getAssigned(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(complaintService.getAssignedComplaints(
                SecurityUtils.currentUser(), createPageable(page, size)));
    }

    @GetMapping("/assigned/{id}")
    @PreAuthorize("hasRole('DEPT_STAFF')")
    public ResponseEntity<FeedbackResponseDto> getAssignedById(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getAssignedComplaint(id, SecurityUtils.currentUser()));
    }

    @PutMapping("/{id}/classification")
    @PreAuthorize("hasRole('CS_STAFF')")
    public ResponseEntity<FeedbackResponseDto> classify(
            @PathVariable Long id,
            @RequestBody ClassifyFeedbackRequestDto request
    ) {
        return ResponseEntity.ok(complaintService.classify(id, request, SecurityUtils.currentUser()));
    }

    @PostMapping("/{id}/assignment")
    @PreAuthorize("hasRole('CS_STAFF')")
    public ResponseEntity<FeedbackResponseDto> assign(
            @PathVariable Long id,
            @RequestBody AssignFeedbackRequestDto request
    ) {
        return ResponseEntity.ok(complaintService.assign(id, request, SecurityUtils.currentUser()));
    }

    @PostMapping("/{id}/responses")
    @PreAuthorize("hasRole('CS_STAFF')")
    public ResponseEntity<FeedbackResponseEntryDto> respond(
            @PathVariable Long id,
            @RequestBody CreateStaffResponseRequestDto request
    ) {
        return ResponseEntity.ok(complaintService.respond(id, request, SecurityUtils.currentUser()));
    }

    @GetMapping("/{id}/responses")
    @PreAuthorize("hasAnyRole('CS_STAFF', 'DEPT_STAFF')")
    public ResponseEntity<List<FeedbackResponseEntryDto>> getResponses(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getResponses(id, SecurityUtils.currentUser()));
    }

    @PostMapping("/assigned/{id}/investigation-notes")
    @PreAuthorize("hasRole('DEPT_STAFF')")
    public ResponseEntity<FeedbackResponseEntryDto> addInvestigationNote(
            @PathVariable Long id,
            @RequestBody InvestigationNoteRequestDto request
    ) {
        return ResponseEntity.ok(complaintService.addInvestigationNote(id, request, SecurityUtils.currentUser()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CS_STAFF', 'DEPT_STAFF')")
    public ResponseEntity<FeedbackResponseDto> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateFeedbackStatusRequestDto request
    ) {
        return ResponseEntity.ok(complaintService.updateStatus(id, request, SecurityUtils.currentUser()));
    }

    @PostMapping("/{id}/escalations")
    @PreAuthorize("hasRole('CS_STAFF')")
    public ResponseEntity<FeedbackResponseDto> escalate(
            @PathVariable Long id,
            @RequestBody EscalateFeedbackRequestDto request
    ) {
        return ResponseEntity.ok(complaintService.escalate(id, request, SecurityUtils.currentUser()));
    }

    private Pageable createPageable(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException("page phải >= 0 và size phải từ 1 đến 100");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
