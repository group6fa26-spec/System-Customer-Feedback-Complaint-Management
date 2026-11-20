package com.bql.feedback.model;

import jakarta.persistence.*;

/** CF01 - Nhân viên / Manager (bổ sung ngoài danh sách 4.6 vì Assignment cần tham chiếu). */
@Entity
@Table(name = "staff")
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private Department department;

    private boolean active;
    private boolean onDuty; // Đang trực (dùng cho sự cố khẩn cấp ngoài giờ)

    protected Staff() {
    }

    public Staff(Long id, String fullName, Role role, Department department, boolean onDuty) {
        this.id = id;
        this.fullName = fullName;
        this.role = role;
        this.department = department;
        this.onDuty = onDuty;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isOnDuty() { return onDuty; }
    public void setOnDuty(boolean onDuty) { this.onDuty = onDuty; }
}
