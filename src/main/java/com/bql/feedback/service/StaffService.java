package com.bql.feedback.service;

import com.bql.feedback.dto.StaffResponseDto;
import com.bql.feedback.exception.NotFoundException;
import com.bql.feedback.model.Staff;
import com.bql.feedback.repository.StaffRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StaffService {

    private final StaffRepository staffRepository;

    public StaffService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    // 1. Lấy toàn bộ danh sách nhân viên
    public List<StaffResponseDto> getAllStaff() {
        return staffRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    // 2. Lấy chi tiết 1 nhân viên theo ID
    public StaffResponseDto getStaffById(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên với ID: " + id));
        return toDto(staff);
    }

    // Hàm chuyển đổi Entity -> DTO
    private StaffResponseDto toDto(Staff s) {
        return new StaffResponseDto(
                s.getId(),
                s.getFullName(),
                s.getRole() != null ? s.getRole().name() : null,
                s.getDepartment() != null ? s.getDepartment().name() : null,
                s.isActive(),
                s.isOnDuty()
        );
    }
}
