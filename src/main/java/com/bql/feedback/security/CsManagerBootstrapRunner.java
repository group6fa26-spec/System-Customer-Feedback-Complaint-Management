package com.bql.feedback.security;

import com.bql.feedback.model.AccountStatus;
import com.bql.feedback.model.Department;
import com.bql.feedback.model.Role;
import com.bql.feedback.model.Staff;
import com.bql.feedback.model.UserAccount;
import com.bql.feedback.repository.StaffRepository;
import com.bql.feedback.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Khởi tạo duy nhất 1 tài khoản CS_MANAGER khi ứng dụng khởi động và áp đặt ràng buộc
 * duy nhất ở tầng cơ sở dữ liệu (SQL Server Filtered Unique Index) trên cả 2 bảng staff và user_accounts.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class CsManagerBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CsManagerBootstrapRunner.class);

    static final String CREATE_STAFF_UNIQUE_INDEX_SQL = """
            IF NOT EXISTS (
                SELECT 1 FROM sys.indexes
                WHERE name = 'UX_staff_single_cs_manager'
                  AND object_id = OBJECT_ID('dbo.staff')
            )
            BEGIN
                CREATE UNIQUE INDEX UX_staff_single_cs_manager
                ON dbo.staff (role)
                WHERE role = 'CS_MANAGER';
            END
            """;

    static final String CREATE_USER_ACCOUNT_UNIQUE_INDEX_SQL = """
            IF NOT EXISTS (
                SELECT 1 FROM sys.indexes
                WHERE name = 'UX_user_accounts_single_cs_manager'
                  AND object_id = OBJECT_ID('dbo.user_accounts')
            )
            BEGIN
                CREATE UNIQUE INDEX UX_user_accounts_single_cs_manager
                ON dbo.user_accounts (role)
                WHERE role = 'CS_MANAGER';
            END
            """;

    private final UserAccountRepository userAccountRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionOperations transactionOperations;
    private final String csManagerUsername;
    private final String csManagerPassword;
    private final String csManagerFullName;

    public CsManagerBootstrapRunner(
            UserAccountRepository userAccountRepository,
            StaffRepository staffRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate,
            TransactionOperations transactionOperations,
            @Value("${app.security.cs-manager.username}") String csManagerUsername,
            @Value("${app.security.cs-manager.password}") String csManagerPassword,
            @Value("${app.security.cs-manager.full-name:Quản lý CSKH}") String csManagerFullName
    ) {
        this.userAccountRepository = userAccountRepository;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionOperations = transactionOperations;
        this.csManagerUsername = csManagerUsername;
        this.csManagerPassword = csManagerPassword;
        this.csManagerFullName = csManagerFullName;
    }

    @Override
    public void run(ApplicationArguments args) {
        transactionOperations.executeWithoutResult(status -> normalizeExistingCsManagers());
        ensureSingleCsManagerIndexes();
        transactionOperations.executeWithoutResult(status -> seedCsManagerIfAbsent());
    }

    private void normalizeExistingCsManagers() {
        List<UserAccount> managerAccounts = userAccountRepository.findByRoleOrderByIdAsc(Role.CS_MANAGER);
        UserAccount primaryAccount = selectPrimaryAccount(managerAccounts);

        if (managerAccounts.size() > 1) {
            List<UserAccount> excessAccounts = new ArrayList<>();
            for (UserAccount account : managerAccounts) {
                if (!Objects.equals(account.getId(), primaryAccount.getId())) {
                    account.setRole(Role.CS_STAFF);
                    excessAccounts.add(account);
                }
            }
            userAccountRepository.saveAll(excessAccounts);
            userAccountRepository.flush();
            log.warn("CsManagerBootstrapRunner: phát hiện {} tài khoản CS_MANAGER dư thừa; giữ lại '{}' (id={}) và chuyển các tài khoản còn lại sang CS_STAFF.",
                    excessAccounts.size(), primaryAccount.getUsername(), primaryAccount.getId());
        }

        List<Staff> managerStaffs = staffRepository.findByRoleOrderByIdAsc(Role.CS_MANAGER);
        if (managerStaffs.isEmpty()) {
            return;
        }

        Staff primaryStaff = selectPrimaryStaff(managerStaffs, primaryAccount);
        List<Staff> excessStaffs = new ArrayList<>();
        for (Staff staff : managerStaffs) {
            if (primaryStaff == null || !Objects.equals(staff.getId(), primaryStaff.getId())) {
                staff.setRole(Role.CS_STAFF);
                excessStaffs.add(staff);
            }
        }
        if (!excessStaffs.isEmpty()) {
            staffRepository.saveAll(excessStaffs);
            staffRepository.flush();
            log.warn("CsManagerBootstrapRunner: phát hiện {} hồ sơ nhân sự CS_MANAGER dư thừa/không phù hợp; đã chuyển sang CS_STAFF.",
                    excessStaffs.size());
        }
    }

    private void ensureSingleCsManagerIndexes() {
        jdbcTemplate.execute(CREATE_STAFF_UNIQUE_INDEX_SQL);
        jdbcTemplate.execute(CREATE_USER_ACCOUNT_UNIQUE_INDEX_SQL);
    }

    private void seedCsManagerIfAbsent() {
        List<UserAccount> existingAccounts = userAccountRepository.findByRoleOrderByIdAsc(Role.CS_MANAGER);
        if (!existingAccounts.isEmpty()) {
            UserAccount existing = existingAccounts.get(0);
            log.info("CsManagerBootstrapRunner: giữ nguyên tài khoản CS_MANAGER hiện có '{}' (id={}, staffId={}).",
                    existing.getUsername(), existing.getId(), existing.getStaffId());
            return;
        }

        String username = csManagerUsername != null ? csManagerUsername.trim() : "";
        if (username.isBlank()) {
            throw new IllegalStateException("Thiếu cấu hình app.security.cs-manager.username để khởi tạo CS_MANAGER");
        }
        if (csManagerPassword == null || csManagerPassword.isBlank()) {
            throw new IllegalStateException("Thiếu cấu hình app.security.cs-manager.password để khởi tạo CS_MANAGER");
        }
        if (userAccountRepository.existsByUsername(username)) {
            throw new IllegalStateException(
                    "Tên đăng nhập CS_MANAGER mặc định '" + username + "' đã thuộc về một tài khoản khác");
        }

        String fullName = (csManagerFullName == null || csManagerFullName.isBlank())
                ? "Quản lý CSKH"
                : csManagerFullName.trim();

        Staff managerStaff;
        List<Staff> existingManagerStaffs = staffRepository.findByRoleOrderByIdAsc(Role.CS_MANAGER);
        if (!existingManagerStaffs.isEmpty()) {
            managerStaff = existingManagerStaffs.get(0);
            if (!managerStaff.isActive()) {
                managerStaff.setActive(true);
                managerStaff = staffRepository.save(managerStaff);
            }
        } else {
            managerStaff = staffRepository.save(
                    new Staff(null, fullName, Role.CS_MANAGER, Department.CUSTOMER_SERVICE, true));
        }

        UserAccount account = new UserAccount(
                null,
                username,
                passwordEncoder.encode(csManagerPassword),
                Role.CS_MANAGER,
                null,
                managerStaff.getId(),
                true,
                LocalDateTime.now()
        );
        account.setAccountStatus(AccountStatus.APPROVED);
        UserAccount savedAccount = userAccountRepository.save(account);

        log.info("CsManagerBootstrapRunner: đã khởi tạo tài khoản CS_MANAGER duy nhất '{}' (id={}, staffId={}).",
                savedAccount.getUsername(), savedAccount.getId(), managerStaff.getId());
    }

    private UserAccount selectPrimaryAccount(List<UserAccount> managerAccounts) {
        if (managerAccounts.isEmpty()) {
            return null;
        }
        for (UserAccount account : managerAccounts) {
            if (account.isActive() && account.getAccountStatus() == AccountStatus.APPROVED) {
                return account;
            }
        }
        return managerAccounts.get(0);
    }

    private Staff selectPrimaryStaff(List<Staff> managerStaffs, UserAccount primaryAccount) {
        if (primaryAccount != null && primaryAccount.getStaffId() != null) {
            for (Staff staff : managerStaffs) {
                if (Objects.equals(staff.getId(), primaryAccount.getStaffId())) {
                    return staff;
                }
            }
        }
        if (primaryAccount == null) {
            for (Staff staff : managerStaffs) {
                if (staff.isActive() && userAccountRepository.findByStaffId(staff.getId()).isEmpty()) {
                    return staff;
                }
            }
            for (Staff staff : managerStaffs) {
                if (userAccountRepository.findByStaffId(staff.getId()).isEmpty()) {
                    return staff;
                }
            }
            return null;
        }
        for (Staff staff : managerStaffs) {
            if (staff.isActive()) {
                return staff;
            }
        }
        return managerStaffs.get(0);
    }
}
