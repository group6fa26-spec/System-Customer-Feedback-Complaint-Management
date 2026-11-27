package com.bql.feedback.controller;

import com.bql.feedback.dto.CreateFeedbackRequestDto;
import com.bql.feedback.dto.FeedbackResponseDto;
import com.bql.feedback.dto.MergeFeedbackRequestDto;
import com.bql.feedback.security.SecurityUtils;
import com.bql.feedback.service.FeedbackService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    // POST http://localhost:8080/api/feedbacks (customerId lấy từ JWT, không tin body)
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<FeedbackResponseDto> create(@RequestBody CreateFeedbackRequestDto request) {
        FeedbackService.CreateFeedbackResult result =
                feedbackService.createFeedback(SecurityUtils.currentCustomerId(), request);
        HttpStatus status = result.idempotentHit() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result.feedback());
    }

    // GET http://localhost:8080/api/feedbacks
    @GetMapping
    @PreAuthorize("hasAnyRole('CS_STAFF', 'DEPT_STAFF', 'CS_MANAGER')")
    public ResponseEntity<List<FeedbackResponseDto>> getAll() {
        return ResponseEntity.ok(feedbackService.getAllFeedbacks());
    }

    // GET http://localhost:8080/api/feedbacks/mine (JWT của cư dân)
    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<FeedbackResponseDto>> getMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(feedbackService.getMyFeedbacks(SecurityUtils.currentCustomerId(), pageable));
    }

    // GET http://localhost:8080/api/feedbacks/mine/1 (JWT của cư dân)
    @GetMapping("/mine/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<FeedbackResponseDto> getMineById(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.getMyFeedbackById(id, SecurityUtils.currentCustomerId()));
    }

    // GET http://localhost:8080/api/feedbacks/code/FB-20261121-0001 (CUSTOMER chỉ xem được phiếu của mình)
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'CS_STAFF', 'DEPT_STAFF', 'CS_MANAGER')")
    public ResponseEntity<FeedbackResponseDto> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(feedbackService.getFeedbackByCode(code, SecurityUtils.currentUser()));
    }

    // GET http://localhost:8080/api/feedbacks/1
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CS_STAFF', 'DEPT_STAFF', 'CS_MANAGER')")
    public ResponseEntity<FeedbackResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.getFeedbackById(id));
    }

    // POST http://localhost:8080/api/feedbacks/2/merge (CS duyệt gộp phiếu trùng, staffId lấy từ JWT)
    @PostMapping("/{id}/merge")
    @PreAuthorize("hasAnyRole('CS_STAFF', 'CS_MANAGER')")
    public ResponseEntity<FeedbackResponseDto> mergeDuplicate(
            @PathVariable Long id,
            @RequestBody MergeFeedbackRequestDto request
    ) {
        return ResponseEntity.ok(
                feedbackService.mergeDuplicateFeedback(id, SecurityUtils.currentStaffId(), request));
    }
}
