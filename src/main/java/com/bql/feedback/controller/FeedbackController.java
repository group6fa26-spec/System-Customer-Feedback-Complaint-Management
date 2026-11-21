package com.bql.feedback.controller;

import com.bql.feedback.dto.CreateFeedbackRequestDto;
import com.bql.feedback.dto.FeedbackResponseDto;
import com.bql.feedback.dto.MergeFeedbackRequestDto;
import com.bql.feedback.service.FeedbackService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    // POST http://localhost:8080/api/feedbacks
    @PostMapping
    public ResponseEntity<FeedbackResponseDto> create(@RequestBody CreateFeedbackRequestDto request) {
        FeedbackService.CreateFeedbackResult result = feedbackService.createFeedback(request);
        HttpStatus status = result.idempotentHit() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result.feedback());
    }

    // GET http://localhost:8080/api/feedbacks
    @GetMapping
    public ResponseEntity<List<FeedbackResponseDto>> getAll() {
        return ResponseEntity.ok(feedbackService.getAllFeedbacks());
    }

    // GET http://localhost:8080/api/feedbacks/mine (Header: X-Customer-Id)
    @GetMapping("/mine")
    public ResponseEntity<Page<FeedbackResponseDto>> getMine(
            @RequestHeader("X-Customer-Id") Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(feedbackService.getMyFeedbacks(customerId, pageable));
    }

    // GET http://localhost:8080/api/feedbacks/mine/1 (Header: X-Customer-Id)
    @GetMapping("/mine/{id}")
    public ResponseEntity<FeedbackResponseDto> getMineById(
            @PathVariable Long id,
            @RequestHeader("X-Customer-Id") Long customerId
    ) {
        return ResponseEntity.ok(feedbackService.getMyFeedbackById(id, customerId));
    }

    // GET http://localhost:8080/api/feedbacks/code/FB-20261121-0001
    @GetMapping("/code/{code}")
    public ResponseEntity<FeedbackResponseDto> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(feedbackService.getFeedbackByCode(code));
    }

    // GET http://localhost:8080/api/feedbacks/1
    @GetMapping("/{id}")
    public ResponseEntity<FeedbackResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.getFeedbackById(id));
    }

    // POST http://localhost:8080/api/feedbacks/2/merge (CS duyệt gộp phiếu trùng vào phiếu gốc)
    @PostMapping("/{id}/merge")
    public ResponseEntity<FeedbackResponseDto> mergeDuplicate(
            @PathVariable Long id,
            @RequestBody MergeFeedbackRequestDto request
    ) {
        return ResponseEntity.ok(feedbackService.mergeDuplicateFeedback(id, request));
    }
}
