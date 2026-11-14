package com.bql.feedback.model;

/** CF03 - Danh mục phản hồi. */
public enum FeedbackType {
    COMPLAINT("Khiếu nại"), SUGGESTION("Góp ý"), PROPOSAL("Đề xuất"), PRAISE("Khen ngợi");
    private final String label;
    FeedbackType(String label) { this.label = label; }
    public String getLabel() { return label; }
    /** Góp ý / khen ngợi có luồng đóng đơn giản hơn khiếu nại (Business Rule 6). */
    public boolean isSimpleFlow() { return this == SUGGESTION || this == PRAISE; }
}
