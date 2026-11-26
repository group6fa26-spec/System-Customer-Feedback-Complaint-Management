package com.bql.feedback.service;

import com.bql.feedback.dto.CustomerResponseDto;
import com.bql.feedback.dto.RentalContractResponseDto;
import com.bql.feedback.dto.UpdateCustomerProfileRequestDto;
import com.bql.feedback.exception.BusinessException;
import com.bql.feedback.exception.NotFoundException;
import com.bql.feedback.model.Customer;
import com.bql.feedback.repository.CustomerRepository;
import com.bql.feedback.repository.RentalContractRepository;
import com.bql.feedback.util.ContactValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final RentalContractRepository rentalContractRepository;

    public CustomerService(CustomerRepository customerRepository,
                           RentalContractRepository rentalContractRepository) {
        this.customerRepository = customerRepository;
        this.rentalContractRepository = rentalContractRepository;
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
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cư dân với ID: " + id));
        return toDto(customer);
    }

    // 3. Cập nhật thông tin liên hệ hồ sơ cư dân (giữ nguyên apartmentCode & verifiedResident)
    @Transactional
    public CustomerResponseDto updateProfile(Long customerId,
                                             UpdateCustomerProfileRequestDto request,
                                             boolean isSelfUpdate) {
        if (request == null) {
            throw new BusinessException("Dữ liệu cập nhật hồ sơ không được rỗng");
        }
        String fullName = requireNonBlank(request.fullName(), "Họ tên không được để trống");
        String phone = ContactValidationUtil.normalizeAndValidatePhone(request.phone());
        String email = ContactValidationUtil.normalizeAndValidateEmail(request.email());

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cư dân với ID: " + customerId));

        if (!customer.isActive()) {
            throw new BusinessException("Hồ sơ cư dân đã bị vô hiệu hóa");
        }
        if (isSelfUpdate && customer.isBlocked()) {
            throw new BusinessException("Tài khoản đang bị khóa do vi phạm, không thể tự cập nhật hồ sơ");
        }

        customerRepository.findByEmailIgnoreCase(email)
                .filter(existing -> !existing.getId().equals(customerId))
                .ifPresent(existing -> {
                    throw new BusinessException("Email đã được sử dụng");
                });

        customerRepository.findByPhone(phone)
                .filter(existing -> !existing.getId().equals(customerId))
                .ifPresent(existing -> {
                    throw new BusinessException("Số điện thoại đã được sử dụng");
                });

        customer.setFullName(fullName);
        customer.setPhone(phone);
        customer.setEmail(email);

        Customer saved = customerRepository.save(customer);
        return toDto(saved);
    }

    private String requireNonBlank(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(errorMessage);
        }
        return value.trim();
    }

    // Hàm chuyển đổi Entity -> DTO
    private CustomerResponseDto toDto(Customer c) {
        return new CustomerResponseDto(
                c.getId(),
                c.getFullName(),
                c.getPhone(),
                c.getEmail(),
                c.getApartmentCode(),
                c.getResidentType().name(),
                rentalContractRepository.findByTenantCustomerIdOrderByStartDateDesc(c.getId()).stream()
                    .map(contract -> new RentalContractResponseDto(
                        contract.getId(),
                        contract.getApartmentCode(),
                        contract.getStartDate(),
                        contract.getEndDate(),
                        contract.getStatus().name()))
                    .toList(),
                c.isVerifiedResident(),
                c.isActive(),
                c.isBlocked(),
                c.getSpamStrikes(),
                c.getRatingTrustScore(),
                c.getCreatedAt()
        );
    }
}
