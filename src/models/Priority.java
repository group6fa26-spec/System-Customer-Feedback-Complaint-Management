package com.bql.feedback.model;

/** CF07 - Mức độ ưu tiên và SLA (phút). LOW là mức bổ sung cho góp ý/khen ngợi (giả định). */
public enum Priority {
    LOW("Thấp", 1440, 4320),
    NORMAL("Thường", 240, 2880),
    HIGH("Cao", 30, 480),
    CRITICAL("Khẩn cấp", 5, 120);

    private final String label;
    private final int responseMinutes;
    private final int resolveMinutes;

    Priority(String label, int responseMinutes, int resolveMinutes) {
        this.label = label; this.responseMinutes = responseMinutes; this.resolveMinutes = resolveMinutes;
    }
    public String getLabel() { return label; }
    public int getResponseMinutes() { return responseMinutes; }
    public int getResolveMinutes() { return resolveMinutes; }
}
