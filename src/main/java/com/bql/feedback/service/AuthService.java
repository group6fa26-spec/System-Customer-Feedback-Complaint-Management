package com.bql.feedback.service;

import com.bql.feedback.dto.LoginRequestDto;
import com.bql.feedback.dto.LoginResponseDto;
import com.bql.feedback.dto.RegisterRequestDto;
import com.bql.feedback.exception.BusinessException;
import com.bql.feedback.exception.NotFoundException;
import com.bql.feedback.model.Customer;
import com.bql.feedback.model.Role;
import com.bql.feedback.model.Staff;
import com.bql.feedback.model.UserAccount;
import com.bql.feedback.repository.CustomerRepository;
import com.bql.feedback.repository.StaffRepository;
import com.bql.feedback.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;

    public AuthService(
            UserAccountRepository userAccountRepository,
            CustomerRepository customerRepository,
            StaffRepository staffRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.customerRepository = customerRepository;
        this.staffRepository = staffRepository;
    }

    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        if (request == null) {
            throw new BusinessException("Dữ liệu đăng nhập không được rỗng");
        }
        if (request.username() == null || request.username().isBlank()) {
            throw new BusinessException("Tên đăng nhập không được để trống");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("Mật khẩu không được để trống");
        }

        UserAccount account = userAccountRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new BusinessException("Tên đăng nhập hoặc mật khẩu không đúng"));

        if (!Objects.equals(account.getPassword(), request.password())) {
            throw new BusinessException("Tên đăng nhập hoặc mật khẩu không đúng");
        }

        if (!account.isActive()) {
            throw new BusinessException("Tài khoản đã bị vô hiệu hóa");
        }

        String fullName = null;
        String department = null;
        String apartmentCode = null;
        Boolean blocked = null;

        if (account.getRole() == Role.CUSTOMER && account.getCustomerId() != null) {
            Customer customer = customerRepository.findById(account.getCustomerId())
                    .orElseThrow(() -> new NotFoundException(
                            "Không tìm thấy hồ sơ cư dân với ID: " + account.getCustomerId()));
            if (!customer.isActive()) {
                throw new BusinessException("Hồ sơ cư dân đã bị vô hiệu hóa");
            }
            fullName = customer.getFullName();
            apartmentCode = customer.getApartmentCode();
            blocked = customer.isBlocked();
        } else if (account.getRole() != Role.CUSTOMER && account.getStaffId() != null) {
            Staff staff = staffRepository.findById(account.getStaffId())
                    .orElseThrow(() -> new NotFoundException(
                            "Không tìm thấy hồ sơ nhân viên với ID: " + account.getStaffId()));
            if (!staff.isActive()) {
                throw new BusinessException("Hồ sơ nhân viên đã bị vô hiệu hóa");
            }
            fullName = staff.getFullName();
            department = staff.getDepartment() != null ? staff.getDepartment().name() : null;
        }

        LocalDateTime now = LocalDateTime.now();
        account.setLastLoginAt(now);
        UserAccount saved = userAccountRepository.save(account);

        return new LoginResponseDto(
                saved.getId(),
                saved.getUsername(),
                saved.getRole() != null ? saved.getRole().name() : null,
                saved.getCustomerId(),
                saved.getStaffId(),
                fullName,
                department,
                apartmentCode,
                blocked,
                saved.getLastLoginAt()
        );
    }

    // Đăng ký tài khoản cơ bản - chỉ dành cho cư dân (Role.CUSTOMER)
    @Transactional
    public LoginResponseDto registerCustomer(RegisterRequestDto request) {
        if (request == null) {
            throw new BusinessException("Dữ liệu đăng ký không được rỗng");
        }
        String username = requireNonBlank(request.username(), "Tên đăng nhập không được để trống");
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("Mật khẩu không được để trống");
        }
        String fullName = requireNonBlank(request.fullName(), "Họ tên không được để trống");
        String phone = requireNonBlank(request.phone(), "Số điện thoại không được để trống");
        String email = requireNonBlank(request.email(), "Email không được để trống");
        String apartmentCode = requireNonBlank(request.apartmentCode(), "Mã căn hộ không được để trống");

        if (userAccountRepository.existsByUsername(username)) {
            throw new BusinessException("Tên đăng nhập đã tồn tại");
        }
        if (customerRepository.existsByEmail(email)) {
            throw new BusinessException("Email đã được sử dụng");
        }
        if (customerRepository.findByPhone(phone).isPresent()) {
            throw new BusinessException("Số điện thoại đã được sử dụng");
        }

        LocalDateTime now = LocalDateTime.now();
        Customer customer = new Customer(null, fullName, phone, email, apartmentCode, false, now);
        Customer savedCustomer = customerRepository.save(customer);

        UserAccount account = new UserAccount(
                null,
                username,
                request.password(),
                Role.CUSTOMER,
                savedCustomer.getId(),
                null,
                true,
                now
        );
        UserAccount savedAccount = userAccountRepository.save(account);

        return new LoginResponseDto(
                savedAccount.getId(),
                savedAccount.getUsername(),
                savedAccount.getRole().name(),
                savedCustomer.getId(),
                null,
                savedCustomer.getFullName(),
                null,
                savedCustomer.getApartmentCode(),
                savedCustomer.isBlocked(),
                savedAccount.getLastLoginAt()
        );
    }

    private String requireNonBlank(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(errorMessage);
        }
        return value.trim();
    }
}
