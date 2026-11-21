package com.bql.feedback.service;

import com.bql.feedback.dto.FeedbackCategoryResponseDto;
import com.bql.feedback.exception.NotFoundException;
import com.bql.feedback.model.FeedbackCategory;
import com.bql.feedback.repository.FeedbackCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedbackCategoryService {

    private final FeedbackCategoryRepository feedbackCategoryRepository;

    public FeedbackCategoryService(FeedbackCategoryRepository feedbackCategoryRepository) {
        this.feedbackCategoryRepository = feedbackCategoryRepository;
    }

    // 1. Lấy toàn bộ danh sách danh mục
    public List<FeedbackCategoryResponseDto> getAllCategories() {
        return feedbackCategoryRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    // 2. Lấy chi tiết 1 danh mục theo ID
    public FeedbackCategoryResponseDto getCategoryById(Long id) {
        FeedbackCategory category = feedbackCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục với ID: " + id));
        return toDto(category);
    }

    // Hàm chuyển đổi Entity -> DTO
    private FeedbackCategoryResponseDto toDto(FeedbackCategory c) {
        return new FeedbackCategoryResponseDto(
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getGroup() != null ? c.getGroup().name() : null,
                c.getDefaultPriority() != null ? c.getDefaultPriority().name() : null,
                c.getKeywords() != null ? List.copyOf(c.getKeywords()) : List.of(),
                c.isActive()
        );
    }
}
