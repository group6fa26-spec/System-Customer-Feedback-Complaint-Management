package com.bql.feedback.model;

/** CF09 - Trạng thái New -> Closed, kèm các trạng thái cho unhappy cases. */
public enum FeedbackStatus {
    NEW, ASSIGNED, IN_PROGRESS,
    WAITING_INFO,      // thiếu thông tin, chờ cư dân bổ sung
    PENDING_APPROVAL,  // Critical: chờ Manager duyệt phương án
    RESOLVED, REOPENED, CLOSED,
    REJECTED,          // spam / phản ánh giả / vi phạm
    MERGED;            // trùng lặp, gộp vào phiếu gốc

    public boolean isFinal() { return this == CLOSED || this == REJECTED || this == MERGED; }

    public boolean canMoveTo(FeedbackStatus t) {
        return switch (this) {
            case NEW -> t == ASSIGNED || t == WAITING_INFO || t == REJECTED || t == MERGED;
            case ASSIGNED -> t == IN_PROGRESS || t == WAITING_INFO;
            case IN_PROGRESS -> t == WAITING_INFO || t == PENDING_APPROVAL || t == RESOLVED;
            case WAITING_INFO -> t == NEW || t == ASSIGNED || t == IN_PROGRESS || t == REJECTED;
            case PENDING_APPROVAL -> t == RESOLVED || t == IN_PROGRESS;
            case RESOLVED -> t == CLOSED || t == REOPENED;
            case REOPENED -> t == ASSIGNED || t == IN_PROGRESS;
            default -> false;
        };
    }
}
