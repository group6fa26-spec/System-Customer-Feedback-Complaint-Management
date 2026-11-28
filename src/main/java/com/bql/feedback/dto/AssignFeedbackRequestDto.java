package com.bql.feedback.dto;

import java.time.LocalDateTime;

public record AssignFeedbackRequestDto(Long staffId, LocalDateTime dueAt, String note) {
}
