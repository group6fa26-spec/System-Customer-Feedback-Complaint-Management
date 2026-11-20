package com.bql.feedback.service;

import com.bql.feedback.dto.CustomerResponseDto;
import com.bql.feedback.model.Customer;
import com.bql.feedback.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // 1. Lấy toàn bộ danh sách cư dân
    public List<CustomerResponseDto> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    // 2. Lấy chi tiết 1 cư dân theo ID
    public CustomerResponseDto getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cư dân với ID: " + id));
        return toDto(customer);
    }

    // Hàm chuyển đổi Entity -> DTO
    private CustomerResponseDto toDto(Customer c) {
        return new CustomerResponseDto(
                c.getId(),
                c.getFullName(),
                c.getPhone(),
                c.getEmail(),
                c.getApartmentCode(),
                c.isVerifiedResident(),
                c.isActive(),
                c.isBlocked(),
                c.getSpamStrikes(),
                c.getRatingTrustScore(),
                c.getCreatedAt()
        );
    }
}
