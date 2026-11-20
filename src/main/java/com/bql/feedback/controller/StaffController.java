package com.bql.feedback.controller;

import com.bql.feedback.dto.StaffResponseDto;
import com.bql.feedback.service.StaffService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    // GET http://localhost:8080/api/staff
    @GetMapping
    public ResponseEntity<List<StaffResponseDto>> getAll() {
        return ResponseEntity.ok(staffService.getAllStaff());
    }

    // GET http://localhost:8080/api/staff/1
    @GetMapping("/{id}")
    public ResponseEntity<StaffResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.getStaffById(id));
    }
}
