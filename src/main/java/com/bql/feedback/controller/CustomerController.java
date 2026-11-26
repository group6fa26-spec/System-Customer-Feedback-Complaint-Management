package com.bql.feedback.controller;

import com.bql.feedback.dto.CustomerResponseDto;
import com.bql.feedback.dto.UpdateCustomerProfileRequestDto;
import com.bql.feedback.service.CustomerService;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<CustomerResponseDto>> getAll() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    // GET http://localhost:8080/api/customers/me (Header: X-Customer-Id)
    @GetMapping("/me")
    public ResponseEntity<CustomerResponseDto> getMyProfile(
            @RequestHeader("X-Customer-Id") Long customerId
    ) {
        return ResponseEntity.ok(customerService.getCustomerById(customerId));
    }

    // PUT http://localhost:8080/api/customers/me (Header: X-Customer-Id)
    @PutMapping("/me")
    public ResponseEntity<CustomerResponseDto> updateMyProfile(
            @RequestHeader("X-Customer-Id") Long customerId,
            @RequestBody UpdateCustomerProfileRequestDto request
    ) {
        return ResponseEntity.ok(customerService.updateProfile(customerId, request, true));
    }

    // GET http://localhost:8080/api/customers/1
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    // PUT http://localhost:8080/api/customers/1 (BQL cập nhật thông tin liên hệ cư dân)
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> updateById(
            @PathVariable Long id,
            @RequestBody UpdateCustomerProfileRequestDto request
    ) {
        return ResponseEntity.ok(customerService.updateProfile(id, request, false));
    }
}
