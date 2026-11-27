package com.bql.feedback.controller;

import com.bql.feedback.dto.CustomerResponseDto;
import com.bql.feedback.dto.UpdateCustomerProfileRequestDto;
import com.bql.feedback.security.SecurityUtils;
import com.bql.feedback.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // GET http://localhost:8080/api/customers
    @GetMapping
    @PreAuthorize("hasAnyRole('CS_STAFF', 'DEPT_STAFF', 'CS_MANAGER')")
    public ResponseEntity<List<CustomerResponseDto>> getAll() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    // GET http://localhost:8080/api/customers/me (JWT của cư dân)
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerResponseDto> getMyProfile() {
        return ResponseEntity.ok(customerService.getCustomerById(SecurityUtils.currentCustomerId()));
    }

    // PUT http://localhost:8080/api/customers/me (JWT của cư dân)
    @PutMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerResponseDto> updateMyProfile(
            @RequestBody UpdateCustomerProfileRequestDto request
    ) {
        return ResponseEntity.ok(customerService.updateProfile(SecurityUtils.currentCustomerId(), request, true));
    }

    // GET http://localhost:8080/api/customers/1
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CS_STAFF', 'DEPT_STAFF', 'CS_MANAGER')")
    public ResponseEntity<CustomerResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    // PUT http://localhost:8080/api/customers/1 (BQL cập nhật thông tin liên hệ cư dân)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CS_STAFF', 'CS_MANAGER')")
    public ResponseEntity<CustomerResponseDto> updateById(
            @PathVariable Long id,
            @RequestBody UpdateCustomerProfileRequestDto request
    ) {
        return ResponseEntity.ok(customerService.updateProfile(id, request, false));
    }
}
