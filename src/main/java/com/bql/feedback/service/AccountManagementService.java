package com.bql.feedback.service;

import com.bql.feedback.dto.AccountManagementResponseDto;
import com.bql.feedback.dto.CreateStaffAccountRequestDto;
import com.bql.feedback.dto.ResetPasswordRequestDto;
import com.bql.feedback.dto.UpdateAccountRequestDto;
import com.bql.feedback.exception.BusinessException;
import com.bql.feedback.exception.ForbiddenException;
import com.bql.feedback.exception.NotFoundException;
import com.bql.feedback.model.AccountStatus;
import com.bql.feedback.model.Customer;
import com.bql.feedback.model.Department;
import com.bql.feedback.model.Role;
import com.bql.feedback.model.Staff;
import com.bql.feedback.model.UserAccount;
import com.bql.feedback.repository.CustomerRepository;
import com.bql.feedback.repository.StaffRepository;
import com.bql.feedback.repository.UserAccountRepository;
import com.bql.feedback.util.ContactValidationUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AccountManagementService {

    public static final int MIN_PASSWORD_LENGTH = AuthService.MIN_PASSWORD_LENGTH;
    private static final int GENERATED_PASSWORD_LENGTH = 12;
    private static final String UPPER_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER_CHARS = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGIT_CHARS = "23456789";
    private static final String SPECIAL_CHARS = "@#$%!&*";
    private static final String ALL_PASSWORD_CHARS = UPPER_CHARS + LOWER_CHARS + DIGIT_CHARS + SPECIAL_CHARS;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserAccountRepository userAccountRepository;
    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountManagementService(
            UserAccountRepository userAccountRepository,
            StaffRepository staffRepository,
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.staffRepository = staffRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 1. Danh sách tài khoản (truy vấn theo lô tránh N+1)
    public List<AccountManagementResponseDto> listAccounts(
            Long managerStaffId,
            Role roleFilter,
            AccountStatus statusFilter,
            boolean includeDeleted,
            String keyword
    ) {
        verifyCsManager(managerStaffId);

        List<UserAccount> accounts = userAccountRepository.findAll().stream()
                .filter(a -> includeDeleted || !a.isDeleted())
                .filter(a -> roleFilter == null || a.getRole() == roleFilter)
                .filter(a -> statusFilter == null || a.getAccountStatus() == statusFilter)
                .sorted(Comparator.comparing(UserAccount::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        if (accounts.isEmpty()) {
            return List.of();
        }

        List<Long> staffIds = accounts.stream()
                .map(UserAccount::getStaffId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Staff> staffMap = staffIds.isEmpty()
                ? Map.of()
                : staffRepository.findAllById(staffIds).stream()
                        .collect(Collectors.toMap(Staff::getId, Function.identity()));

        List<Long> customerIds = accounts.stream()
                .map(UserAccount::getCustomerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Customer> customerMap = customerIds.isEmpty()
                ? Map.of()
                : customerRepository.findAllById(customerIds).stream()
                        .collect(Collectors.toMap(Customer::getId, Function.identity()));

        String normalizedKeyword = (keyword == null || keyword.isBlank())
                ? null
                : keyword.trim().toLowerCase(Locale.ROOT);

        return accounts.stream()
                .map(account -> toDto(
                        account,
                        account.getStaffId() != null ? staffMap.get(account.getStaffId()) : null,
                        account.getCustomerId() != null ? customerMap.get(account.getCustomerId()) : null,
                        null
                ))
                .filter(dto -> matchesKeyword(dto, normalizedKeyword))
                .toList();
    }

    // 2. Chi tiết 1 tài khoản
    public AccountManagementResponseDto getAccountById(Long managerStaffId, Long accountId) {
        verifyCsManager(managerStaffId);
        UserAccount account = loadAccount(accountId);
        return toDtoForAccount(account, null);
    }

    // 3. Tạo tài khoản nhân viên (chỉ cho phép CS_STAFF hoặc DEPT_STAFF, mặc định mustChangePassword = true)
    @Transactional
    public AccountManagementResponseDto createStaffAccount(
            Long managerStaffId,
            CreateStaffAccountRequestDto request
    ) {
        verifyCsManager(managerStaffId);
        if (request == null) {
            throw new BusinessException("Dữ liệu tạo tài khoản nhân viên không được rỗng");
        }

        String username = requireNonBlank(request.username(), "Tên đăng nhập không được để trống");
        String fullName = requireNonBlank(request.fullName(), "Họ tên nhân viên không được để trống");
        Role role = request.role();
        validateManagedStaffRole(role);
        Department department = resolveAndValidateStaffDepartment(role, request.department());

        UserAccount existing = userAccountRepository.findByUsername(username).orElse(null);
        if (existing != null) {
            if (Boolean.TRUE.equals(existing.getDeleted())) {
                throw new BusinessException("Tên đăng nhập đã thuộc về tài khoản bị xóa mềm, hãy khôi phục thay vì tạo mới");
            }
            throw new BusinessException("Tên đăng nhập đã tồn tại");
        }

        PasswordChoice passwordChoice = resolveTemporaryPassword(request.password());

        Staff staff = staffRepository.save(new Staff(
                null,
                fullName,
                role,
                department,
                Boolean.TRUE.equals(request.onDuty())
        ));

        UserAccount account = new UserAccount(
                null,
                username,
                passwordEncoder.encode(passwordChoice.rawPassword()),
                role,
                null,
                staff.getId(),
                true,
                LocalDateTime.now()
        );
        account.setAccountStatus(AccountStatus.APPROVED);
        account.setMustChangePassword(true);
        account.setDeleted(false);
        UserAccount savedAccount = userAccountRepository.save(account);

        return toDto(savedAccount, staff, null, passwordChoice.returnedOncePassword());
    }

    // 4. Chỉnh sửa thông tin tài khoản (nhân viên hoặc cư dân; không thao tác lên CS_MANAGER)
    @Transactional
    public AccountManagementResponseDto updateAccount(
            Long managerStaffId,
            Long accountId,
            UpdateAccountRequestDto request
    ) {
        verifyCsManager(managerStaffId);
        if (request == null) {
            throw new BusinessException("Dữ liệu cập nhật tài khoản không được rỗng");
        }

        UserAccount account = loadManagedAccount(accountId);
        if (Boolean.TRUE.equals(account.getDeleted())) {
            throw new BusinessException("Không thể chỉnh sửa tài khoản đã bị xóa mềm, hãy khôi phục trước");
        }

        if (account.getRole() == Role.CS_STAFF || account.getRole() == Role.DEPT_STAFF) {
            Staff staff = loadLinkedStaff(account);

            if (request.fullName() != null) {
                staff.setFullName(requireNonBlank(request.fullName(), "Họ tên nhân viên không được để trống"));
            }

            Role targetRole = request.role() != null ? request.role() : account.getRole();
            validateManagedStaffRole(targetRole);

            Department candidateDepartment;
            if (request.department() != null) {
                candidateDepartment = request.department();
            } else if (targetRole == Role.CS_STAFF) {
                candidateDepartment = Department.CUSTOMER_SERVICE;
            } else {
                candidateDepartment = staff.getDepartment();
            }
            Department validatedDepartment = resolveAndValidateStaffDepartment(targetRole, candidateDepartment);

            staff.setRole(targetRole);
            staff.setDepartment(validatedDepartment);
            if (request.onDuty() != null) {
                staff.setOnDuty(request.onDuty());
            }
            account.setRole(targetRole);

            Staff savedStaff = staffRepository.save(staff);
            UserAccount savedAccount = userAccountRepository.save(account);
            return toDto(savedAccount, savedStaff, null, null);
        }

        if (account.getRole() == Role.CUSTOMER) {
            if (request.role() != null && request.role() != Role.CUSTOMER) {
                throw new BusinessException("Không thể đổi vai trò của tài khoản cư dân");
            }
            Customer customer = loadLinkedCustomer(account);

            if (request.fullName() != null) {
                customer.setFullName(requireNonBlank(request.fullName(), "Họ tên cư dân không được để trống"));
            }
            if (request.phone() != null) {
                String normalizedPhone = ContactValidationUtil.normalizeAndValidatePhone(request.phone());
                customerRepository.findByPhone(normalizedPhone)
                        .filter(other -> !other.getId().equals(customer.getId()))
                        .ifPresent(other -> {
                            throw new BusinessException("Số điện thoại đã được sử dụng");
                        });
                customer.setPhone(normalizedPhone);
            }
            if (request.email() != null) {
                String normalizedEmail = ContactValidationUtil.normalizeAndValidateEmail(request.email());
                customerRepository.findByEmailIgnoreCase(normalizedEmail)
                        .filter(other -> !other.getId().equals(customer.getId()))
                        .ifPresent(other -> {
                            throw new BusinessException("Email đã được sử dụng");
                        });
                customer.setEmail(normalizedEmail);
            }
            if (request.apartmentCode() != null) {
                customer.setApartmentCode(requireNonBlank(request.apartmentCode(), "Mã căn hộ không được để trống"));
            }
            if (request.verifiedResident() != null) {
                customer.setVerifiedResident(request.verifiedResident());
            }

            Customer savedCustomer = customerRepository.save(customer);
            return toDto(account, null, savedCustomer, null);
        }

        throw new BusinessException("Vai trò tài khoản không được hỗ trợ cập nhật");
    }

    // 5. Block (khóa đăng nhập quản trị; tách riêng khỏi customer.blocked chống spam)
    @Transactional
    public AccountManagementResponseDto blockAccount(Long managerStaffId, Long accountId) {
        verifyCsManager(managerStaffId);
        UserAccount account = loadManagedAccount(accountId);
        if (Boolean.TRUE.equals(account.getDeleted())) {
            throw new BusinessException("Tài khoản đã bị xóa mềm, không thể khóa");
        }

        account.setActive(false);
        account.setAccountStatus(AccountStatus.DISABLED);
        UserAccount savedAccount = userAccountRepository.save(account);

        Staff staff = null;
        Customer customer = null;
        if (account.getRole() == Role.CS_STAFF || account.getRole() == Role.DEPT_STAFF) {
            if (account.getStaffId() != null) {
                staff = staffRepository.findById(account.getStaffId()).orElse(null);
                if (staff != null) {
                    staff.setActive(false);
                    staff = staffRepository.save(staff);
                }
            }
        } else if (account.getRole() == Role.CUSTOMER && account.getCustomerId() != null) {
            // Không đụng customer.blocked (cờ chống spam độc lập)
            customer = customerRepository.findById(account.getCustomerId()).orElse(null);
        }

        return toDto(savedAccount, staff, customer, null);
    }

    // 6. Unblock (mở khóa đăng nhập quản trị; không đụng customer.blocked)
    @Transactional
    public AccountManagementResponseDto unblockAccount(Long managerStaffId, Long accountId) {
        verifyCsManager(managerStaffId);
        UserAccount account = loadManagedAccount(accountId);
        if (Boolean.TRUE.equals(account.getDeleted())) {
            throw new BusinessException("Tài khoản đã bị xóa mềm, hãy khôi phục thay vì mở khóa");
        }
        if (account.getAccountStatus() == AccountStatus.PENDING || account.getAccountStatus() == AccountStatus.REJECTED) {
            throw new BusinessException("Tài khoản đang ở trạng thái " + account.getAccountStatus()
                    + ", không thể mở khóa bằng thao tác này");
        }

        account.setActive(true);
        account.setAccountStatus(AccountStatus.APPROVED);
        UserAccount savedAccount = userAccountRepository.save(account);

        Staff staff = null;
        Customer customer = null;
        if (account.getRole() == Role.CS_STAFF || account.getRole() == Role.DEPT_STAFF) {
            if (account.getStaffId() != null) {
                staff = staffRepository.findById(account.getStaffId()).orElse(null);
                if (staff != null) {
                    staff.setActive(true);
                    staff = staffRepository.save(staff);
                }
            }
        } else if (account.getRole() == Role.CUSTOMER && account.getCustomerId() != null) {
            // Giữ nguyên customer.blocked để không xóa án spam
            customer = customerRepository.findById(account.getCustomerId()).orElse(null);
        }

        return toDto(savedAccount, staff, customer, null);
    }

    // 7. Cấp lại mật khẩu tạm (nhập tay >= 8 ký tự hoặc bỏ trống để BE tự sinh 1 lần) + bật mustChangePassword = true
    @Transactional
    public AccountManagementResponseDto resetPassword(
            Long managerStaffId,
            Long accountId,
            ResetPasswordRequestDto request
    ) {
        verifyCsManager(managerStaffId);
        UserAccount account = loadManagedAccount(accountId);
        if (Boolean.TRUE.equals(account.getDeleted())) {
            throw new BusinessException("Không thể đặt lại mật khẩu cho tài khoản đã bị xóa mềm");
        }

        String requestedPassword = request != null ? request.newPassword() : null;
        PasswordChoice passwordChoice = resolveTemporaryPassword(requestedPassword);

        account.setPassword(passwordEncoder.encode(passwordChoice.rawPassword()));
        account.setMustChangePassword(true);
        UserAccount savedAccount = userAccountRepository.save(account);

        return toDtoForAccount(savedAccount, passwordChoice.returnedOncePassword());
    }

    // 8. Xóa mềm tài khoản (bảo toàn khóa ngoại lịch sử)
    @Transactional
    public AccountManagementResponseDto softDeleteAccount(Long managerStaffId, Long accountId) {
        verifyCsManager(managerStaffId);
        UserAccount account = loadManagedAccount(accountId);
        if (Boolean.TRUE.equals(account.getDeleted())) {
            throw new BusinessException("Tài khoản đã bị xóa mềm trước đó");
        }

        account.setDeleted(true);
        account.setDeletedAt(LocalDateTime.now());
        account.setActive(false);
        account.setAccountStatus(AccountStatus.DISABLED);
        UserAccount savedAccount = userAccountRepository.save(account);

        Staff staff = null;
        Customer customer = null;
        if (account.getStaffId() != null) {
            staff = staffRepository.findById(account.getStaffId()).orElse(null);
            if (staff != null) {
                staff.setActive(false);
                staff = staffRepository.save(staff);
            }
        }
        if (account.getCustomerId() != null) {
            customer = customerRepository.findById(account.getCustomerId()).orElse(null);
            if (customer != null) {
                customer.setActive(false);
                customer = customerRepository.save(customer);
            }
        }

        return toDto(savedAccount, staff, customer, null);
    }

    // 9. Khôi phục tài khoản đã xóa mềm
    @Transactional
    public AccountManagementResponseDto restoreAccount(Long managerStaffId, Long accountId) {
        verifyCsManager(managerStaffId);
        UserAccount account = loadManagedAccount(accountId);
        if (!Boolean.TRUE.equals(account.getDeleted())) {
            throw new BusinessException("Tài khoản chưa bị xóa mềm");
        }

        account.setDeleted(false);
        account.setDeletedAt(null);

        Staff staff = null;
        Customer customer = null;
        if (account.getRole() == Role.CUSTOMER && account.getCustomerId() != null) {
            customer = customerRepository.findById(account.getCustomerId()).orElse(null);
            if (customer != null) {
                customer.setActive(true);
                customer = customerRepository.save(customer);
            }
            if (customer != null && !customer.isVerifiedResident()) {
                account.setAccountStatus(AccountStatus.PENDING);
                account.setActive(false);
            } else {
                account.setAccountStatus(AccountStatus.APPROVED);
                account.setActive(true);
            }
        } else {
            account.setAccountStatus(AccountStatus.APPROVED);
            account.setActive(true);
            if (account.getStaffId() != null) {
                staff = staffRepository.findById(account.getStaffId()).orElse(null);
                if (staff != null) {
                    staff.setActive(true);
                    staff = staffRepository.save(staff);
                }
            }
        }

        UserAccount savedAccount = userAccountRepository.save(account);
        return toDto(savedAccount, staff, customer, null);
    }

    private void verifyCsManager(Long staffId) {
        if (staffId == null) {
            throw new ForbiddenException("Thiếu thông tin định danh quản lý");
        }
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ForbiddenException("Không có quyền truy cập chức năng quản lý tài khoản"));
        if (!staff.isActive() || staff.getRole() != Role.CS_MANAGER) {
            throw new ForbiddenException("Chỉ CS_MANAGER đang hoạt động mới có quyền quản lý tài khoản");
        }
    }

    private UserAccount loadAccount(Long accountId) {
        return userAccountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản với ID: " + accountId));
    }

    private UserAccount loadManagedAccount(Long accountId) {
        UserAccount account = loadAccount(accountId);
        if (account.getRole() == Role.CS_MANAGER) {
            throw new BusinessException("Không thể thực hiện thao tác quản trị lên tài khoản CS_MANAGER duy nhất");
        }
        return account;
    }

    private Staff loadLinkedStaff(UserAccount account) {
        if (account.getStaffId() == null) {
            throw new NotFoundException("Tài khoản nhân viên không gắn hồ sơ nhân sự");
        }
        return staffRepository.findById(account.getStaffId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hồ sơ nhân viên với ID: " + account.getStaffId()));
    }

    private Customer loadLinkedCustomer(UserAccount account) {
        if (account.getCustomerId() == null) {
            throw new NotFoundException("Tài khoản cư dân không gắn hồ sơ cư dân");
        }
        return customerRepository.findById(account.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hồ sơ cư dân với ID: " + account.getCustomerId()));
    }

    private void validateManagedStaffRole(Role role) {
        if (role == Role.CS_MANAGER) {
            throw new BusinessException("Không được phép tạo mới hoặc gán vai trò CS_MANAGER");
        }
        if (role != Role.CS_STAFF && role != Role.DEPT_STAFF) {
            throw new BusinessException("Vai trò nhân viên chỉ chấp nhận CS_STAFF hoặc DEPT_STAFF");
        }
    }

    private Department resolveAndValidateStaffDepartment(Role role, Department department) {
        if (role == Role.CS_STAFF) {
            if (department == null) {
                return Department.CUSTOMER_SERVICE;
            }
            if (department != Department.CUSTOMER_SERVICE) {
                throw new BusinessException("Nhân viên CSKH (CS_STAFF) phải thuộc phòng CUSTOMER_SERVICE");
            }
            return department;
        }
        if (department == null || department == Department.CUSTOMER_SERVICE) {
            throw new BusinessException("Nhân viên bộ phận (DEPT_STAFF) phải thuộc một phòng ban chuyên môn");
        }
        return department;
    }

    private PasswordChoice resolveTemporaryPassword(String suppliedPassword) {
        if (suppliedPassword == null || suppliedPassword.isBlank()) {
            String generated = generateTemporaryPassword();
            return new PasswordChoice(generated, generated);
        }
        if (suppliedPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException("Mật khẩu phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự");
        }
        return new PasswordChoice(suppliedPassword, null);
    }

    private String generateTemporaryPassword() {
        char[] chars = new char[GENERATED_PASSWORD_LENGTH];
        chars[0] = UPPER_CHARS.charAt(SECURE_RANDOM.nextInt(UPPER_CHARS.length()));
        chars[1] = LOWER_CHARS.charAt(SECURE_RANDOM.nextInt(LOWER_CHARS.length()));
        chars[2] = DIGIT_CHARS.charAt(SECURE_RANDOM.nextInt(DIGIT_CHARS.length()));
        chars[3] = SPECIAL_CHARS.charAt(SECURE_RANDOM.nextInt(SPECIAL_CHARS.length()));
        for (int i = 4; i < GENERATED_PASSWORD_LENGTH; i++) {
            chars[i] = ALL_PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(ALL_PASSWORD_CHARS.length()));
        }
        for (int i = chars.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    private AccountManagementResponseDto toDtoForAccount(UserAccount account, String temporaryPassword) {
        Staff staff = account.getStaffId() != null
                ? staffRepository.findById(account.getStaffId()).orElse(null)
                : null;
        Customer customer = account.getCustomerId() != null
                ? customerRepository.findById(account.getCustomerId()).orElse(null)
                : null;
        return toDto(account, staff, customer, temporaryPassword);
    }

    private AccountManagementResponseDto toDto(
            UserAccount account,
            Staff staff,
            Customer customer,
            String temporaryPassword
    ) {
        String fullName = staff != null
                ? staff.getFullName()
                : (customer != null ? customer.getFullName() : null);

        return new AccountManagementResponseDto(
                account.getId(),
                account.getUsername(),
                account.getRole() != null ? account.getRole().name() : null,
                account.getAccountStatus().name(),
                account.isActive(),
                account.isMustChangePassword(),
                Boolean.TRUE.equals(account.getDeleted()),
                account.getDeletedAt(),
                account.getStaffId(),
                account.getCustomerId(),
                fullName,
                staff != null && staff.getDepartment() != null ? staff.getDepartment().name() : null,
                staff != null ? staff.isOnDuty() : null,
                staff != null ? staff.isActive() : null,
                customer != null ? customer.getPhone() : null,
                customer != null ? customer.getEmail() : null,
                customer != null ? customer.getApartmentCode() : null,
                customer != null ? customer.getResidentType().name() : null,
                customer != null ? customer.isVerifiedResident() : null,
                customer != null ? customer.isActive() : null,
                customer != null ? customer.isBlocked() : null,
                account.getCreatedAt(),
                account.getLastLoginAt(),
                temporaryPassword
        );
    }

    private boolean matchesKeyword(AccountManagementResponseDto dto, String keyword) {
        if (keyword == null) {
            return true;
        }
        return containsIgnoreCase(dto.username(), keyword)
                || containsIgnoreCase(dto.fullName(), keyword)
                || containsIgnoreCase(dto.email(), keyword)
                || containsIgnoreCase(dto.phone(), keyword)
                || containsIgnoreCase(dto.apartmentCode(), keyword);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String requireNonBlank(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(errorMessage);
        }
        return value.trim();
    }

    private record PasswordChoice(String rawPassword, String returnedOncePassword) {
    }
}
