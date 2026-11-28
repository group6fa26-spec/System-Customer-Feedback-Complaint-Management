package com.bql.feedback.dto;

import com.bql.feedback.model.Priority;

public record ClassifyFeedbackRequestDto(Long categoryId, Priority priority) {
}
