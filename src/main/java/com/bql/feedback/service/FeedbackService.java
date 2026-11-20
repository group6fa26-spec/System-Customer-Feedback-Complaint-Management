package com.bql.feedback.service;

import com.bql.feedback.dto.FeedbackResponseDto;
import com.bql.feedback.model.Feedback; 
import com.bql.feedback.repository.FeedbackRepository; 
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    // 1. Lấy toàn bộ danh sách phiếu (chưa xóa mềm)
    public List<FeedbackResponseDto> getAllFeedbacks() {
        return feedbackRepository.findByDeletedAtIsNull()
                .stream()
                .map(this::toDto)
                .toList();
    }

    // 2. Lấy chi tiết 1 phiếu theo ID (chưa xóa mềm)
    public FeedbackResponseDto getFeedbackById(Long id) {
        Feedback feedback = feedbackRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu với ID: " + id));
        return toDto(feedback);
    }

    // Hàm chuyển đổi Entity -> DTO
    private FeedbackResponseDto toDto(Feedback f) {
        return new FeedbackResponseDto(
                f.getId(),
                f.getCode(),
                f.getTitle(),
                f.getContent(),
                f.getType() != null ? f.getType().name() : null,
                f.getPriority() != null ? f.getPriority().name() : null,
                f.getStatus() != null ? f.getStatus().name() : null,
                f.getApartmentCode(),
                f.getLocation(),
                f.getCustomerId(),
                f.getCategoryId(),
                f.getAssignedStaffId(),
                f.getCreatedAt()
        );
    }
}