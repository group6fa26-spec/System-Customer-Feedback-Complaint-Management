package com.bql.feedback.model;

import java.util.List;

/** Danh mục sự cố. keywords dùng cho phân loại tự động. */
public record FeedbackCategory(Long id, String code, String name, IssueGroup group,
                               Priority defaultPriority, List<String> keywords, boolean active) { }
