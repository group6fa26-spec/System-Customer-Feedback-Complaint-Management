package com.bql.feedback.repository;

import com.bql.feedback.model.Department;
import com.bql.feedback.model.Role;
import com.bql.feedback.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    List<Staff> findByRoleAndActiveTrue(Role role);
    List<Staff> findByDepartmentAndActiveTrue(Department department);
    /** Tìm người đang trực (dùng cho sự cố khẩn cấp ngoài giờ). */
    List<Staff> findByDepartmentAndActiveTrueAndOnDutyTrue(Department department);
}
