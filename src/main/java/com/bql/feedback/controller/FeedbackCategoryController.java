package com.bql.feedback.controller;

import com.bql.feedback.dto.FeedbackCategoryResponseDto;
import com.bql.feedback.service.FeedbackCategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@PreAuthorize("hasAnyRole('CUSTOMER', 'CS_STAFF', 'DEPT_STAFF', 'CS_MANAGER')")
public class FeedbackCategoryController {

    private final FeedbackCategoryService feedbackCategoryService;

    public FeedbackCategoryController(FeedbackCategoryService feedbackCategoryService) {
        this.feedbackCategoryService = feedbackCategoryService;
    }

    // GET http://localhost:8080/api/categories
    @GetMapping
    public ResponseEntity<List<FeedbackCategoryResponseDto>> getAll() {
        return ResponseEntity.ok(feedbackCategoryService.getAllCategories());
    }

    // GET http://localhost:8080/api/categories/1
    @GetMapping("/{id}")
    public ResponseEntity<FeedbackCategoryResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackCategoryService.getCategoryById(id));
    }
}
