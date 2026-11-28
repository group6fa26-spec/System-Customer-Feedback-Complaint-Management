package com.bql.feedback.dto;

import com.bql.feedback.model.EscalationReason;

public record EscalateFeedbackRequestDto(EscalationReason reason, String note) {
}
