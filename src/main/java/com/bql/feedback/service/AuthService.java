package com.bql.feedback.service;

import com.bql.feedback.dto.LoginRequestDto;
import com.bql.feedback.dto.LoginResponseDto;
import com.bql.feedback.dto.RegisterRequestDto;
import com.bql.feedback.dto.RentalContractResponseDto;
import com.bql.feedback.exception.BusinessException;
import com.bql.feedback.exception.NotFoundException;
import com.bql.feedback.exception.UnauthorizedException;
import com.bql.feedback.model.AccountStatus;
import com.bql.feedback.model.Customer;
import com.bql.feedback.model.RentalContract;
import com.bql.feedback.model.RentalContractStatus;
import com.bql.feedback.model.ResidentType;
import com.bql.feedback.model.Role;
import com.bql.feedback.model.Staff;
import com.bql.feedback.model.UserAccount;
import com.bql.feedback.repository.CustomerRepository;
import com.bql.feedback.repository.RentalContractRepository;
import com.bql.feedback.repository.StaffRepository;
import com.bql.feedback.repository.UserAccountRepository;
import com.bql.feedback.security.JwtService;
import com.bql.feedback.security.LoginAttemptService;
import com.bql.feedback.util.ContactValidationUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Tên đăng nhập hoặc mật khẩu không đúng";

    private final UserAccountRepository userAccountRepository;
    private final CustomerRepository customerRepository;
    private final RentalContractRepository rentalContractRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(
            UserAccountRepository userAccountRepository,
            CustomerRepository customerRepository,
            RentalContractRepository rentalContractRepository,
            StaffRepository staffRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LoginAttemptService loginAttemptService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.customerRepository = customerRepository;
        this.rentalContractRepository = rentalContractRepository;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
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

        String username = request.username().trim();
        loginAttemptService.checkAllowed(username);

        UserAccount account = userAccountRepository.findByUsername(username).orElse(null);
        if (account == null
                || account.getPassword() == null
                || !passwordEncoder.matches(request.password(), account.getPassword())) {
            loginAttemptService.recordFailure(username);
            throw new UnauthorizedException(INVALID_CREDENTIALS_MESSAGE);
        }

        if (account.getAccountStatus() == AccountStatus.PENDING) {
            throw new BusinessException("Tài khoản đang chờ Ban quản lý duyệt");
        }
        if (account.getAccountStatus() == AccountStatus.REJECTED) {
            throw new BusinessException("Tài khoản đăng ký đã bị từ chối");
        }
        if (!account.isActive() || account.getAccountStatus() == AccountStatus.DISABLED) {
            throw new BusinessException("Tài khoản đã bị vô hiệu hóa");
        }

        String fullName = null;
        String department = null;
        String apartmentCode = null;
        Boolean blocked = null;
        String residentType = null;
        List<RentalContractResponseDto> rentalContracts = List.of();

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
            residentType = customer.getResidentType().name();
            rentalContracts = getRentalContractDtos(customer.getId());
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

        loginAttemptService.recordSuccess(username);

        LocalDateTime now = LocalDateTime.now();
        account.setLastLoginAt(now);
        UserAccount saved = userAccountRepository.save(account);

        String roleName = saved.getRole() != null ? saved.getRole().name() : null;
        String token = jwtService.generateToken(
                saved.getId(),
                saved.getUsername(),
                roleName,
                saved.getCustomerId(),
                saved.getStaffId()
        );

        return new LoginResponseDto(
                saved.getId(),
                saved.getUsername(),
                roleName,
                saved.getCustomerId(),
                saved.getStaffId(),
                fullName,
                department,
                apartmentCode,
                blocked,
                saved.getLastLoginAt(),
                residentType,
                saved.getAccountStatus().name(),
                rentalContracts,
                token
        );
    }

    // Đăng ký tài khoản chủ căn hộ hoặc người thuê (mã hóa BCrypt, token = null do đang PENDING).
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
        String phone = ContactValidationUtil.normalizeAndValidatePhone(request.phone());
        String email = ContactValidationUtil.normalizeAndValidateEmail(request.email());
        String apartmentCode = requireNonBlank(request.apartmentCode(), "Mã căn hộ không được để trống");
        ResidentType residentType = request.residentType() != null ? request.residentType() : ResidentType.OWNER;
        var rentalContractStartDate = request.rentalContractStartDate();
        var rentalContractEndDate = request.rentalContractEndDate();

        if (residentType == ResidentType.TENANT
                && (rentalContractStartDate == null || rentalContractEndDate == null)) {
            throw new BusinessException("Người thuê cần cung cấp ngày bắt đầu và kết thúc hợp đồng");
        }
        if (residentType == ResidentType.TENANT && rentalContractEndDate.isBefore(rentalContractStartDate)) {
            throw new BusinessException("Ngày kết thúc hợp đồng phải sau hoặc bằng ngày bắt đầu");
        }
        if (residentType == ResidentType.OWNER
                && (rentalContractStartDate != null || rentalContractEndDate != null)) {
            throw new BusinessException("Chủ căn hộ không cần cung cấp thời hạn hợp đồng thuê");
        }

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
        customer.setResidentType(residentType);
        Customer savedCustomer = customerRepository.save(customer);

        if (residentType == ResidentType.TENANT) {
            rentalContractRepository.save(new RentalContract(
                    null,
                    savedCustomer.getId(),
                    apartmentCode,
                    rentalContractStartDate,
                    rentalContractEndDate,
                    RentalContractStatus.PENDING,
                    now
            ));
        }

        UserAccount account = new UserAccount(
                null,
                username,
                passwordEncoder.encode(request.password()),
                Role.CUSTOMER,
                savedCustomer.getId(),
                null,
                false,
                now
        );
        account.setAccountStatus(AccountStatus.PENDING);
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
                savedAccount.getLastLoginAt(),
                savedCustomer.getResidentType().name(),
                savedAccount.getAccountStatus().name(),
                getRentalContractDtos(savedCustomer.getId()),
                null
        );
    }

    private List<RentalContractResponseDto> getRentalContractDtos(Long customerId) {
        return rentalContractRepository.findByTenantCustomerIdOrderByStartDateDesc(customerId).stream()
                .map(contract -> new RentalContractResponseDto(
                        contract.getId(),
                        contract.getApartmentCode(),
                        contract.getStartDate(),
                        contract.getEndDate(),
                        contract.getStatus().name()))
                .toList();
    }

    private String requireNonBlank(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(errorMessage);
        }
        return value.trim();
    }
}
