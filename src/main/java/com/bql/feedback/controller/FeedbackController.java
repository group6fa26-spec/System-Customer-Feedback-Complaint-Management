package com.bql.feedback.controller;

import com.bql.feedback.dto.FeedbackResponseDto;
import com.bql.feedback.service.FeedbackService;
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

    // GET http://localhost:8080/api/feedbacks
    @GetMapping
    public ResponseEntity<List<FeedbackResponseDto>> getAll() {
        return ResponseEntity.ok(feedbackService.getAllFeedbacks());
    }

    // GET http://localhost:8080/api/feedbacks/1
    @GetMapping("/{id}")
    public ResponseEntity<FeedbackResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.getFeedbackById(id));
    }
}