package com.bql.feedback.service;

import com.bql.feedback.dto.AccountManagementResponseDto;
import com.bql.feedback.dto.CreateStaffAccountRequestDto;
import com.bql.feedback.dto.ResetPasswordRequestDto;
import com.bql.feedback.dto.UpdateAccountRequestDto;
import com.bql.feedback.exception.BusinessException;
import com.bql.feedback.exception.ForbiddenException;
import com.bql.feedback.model.AccountStatus;
import com.bql.feedback.model.Customer;
import com.bql.feedback.model.Department;
import com.bql.feedback.model.Role;
import com.bql.feedback.model.Staff;
import com.bql.feedback.model.UserAccount;
import com.bql.feedback.repository.CustomerRepository;
import com.bql.feedback.repository.StaffRepository;
import com.bql.feedback.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountManagementServiceTests {

    private UserAccountRepository accounts;
    private StaffRepository staffs;
    private CustomerRepository customers;
    private PasswordEncoder encoder;
    private AccountManagementService service;

    @BeforeEach
    void setup() {
        accounts = mock(UserAccountRepository.class);
        staffs = mock(StaffRepository.class);
        customers = mock(CustomerRepository.class);
        encoder = new BCryptPasswordEncoder();
        service = new AccountManagementService(accounts, staffs, customers, encoder);

        Staff manager = new Staff(1L, "Quan ly CSKH", Role.CS_MANAGER, Department.CUSTOMER_SERVICE, true);
        when(staffs.findById(1L)).thenReturn(Optional.of(manager));
    }

    @Test
    void createStaffWithManualPasswordSetsMustChangeAndHidesPasswordInResponse() {
        when(accounts.findByUsername("cs01")).thenReturn(Optional.empty());
        when(staffs.save(any(Staff.class))).thenAnswer(inv -> {
            Staff s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });
        when(accounts.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount ua = inv.getArgument(0);
            ua.setId(20L);
            return ua;
        });

        AccountManagementResponseDto res = service.createStaffAccount(1L, new CreateStaffAccountRequestDto(
                "cs01", "TempPass@1", "Nhan vien CS", Role.CS_STAFF, null, true));

        assertEquals("CS_STAFF", res.role());
        assertEquals("CUSTOMER_SERVICE", res.department());
        assertEquals("APPROVED", res.accountStatus());
        assertTrue(res.active());
        assertTrue(res.mustChangePassword());
        assertNull(res.temporaryPassword());
    }

    @Test
    void createStaffWithBlankPasswordAutoGeneratesStrongPasswordOnce() {
        when(accounts.findByUsername("tech01")).thenReturn(Optional.empty());
        when(staffs.save(any(Staff.class))).thenAnswer(inv -> {
            Staff s = inv.getArgument(0);
            s.setId(11L);
            return s;
        });
        final String[] savedHash = new String[1];
        when(accounts.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount ua = inv.getArgument(0);
            ua.setId(21L);
            savedHash[0] = ua.getPassword();
            return ua;
        });

        AccountManagementResponseDto res = service.createStaffAccount(1L, new CreateStaffAccountRequestDto(
                "tech01", "   ", "Ky thuat vien", Role.DEPT_STAFF, Department.TECHNICAL, false));

        assertNotNull(res.temporaryPassword());
        assertTrue(res.temporaryPassword().length() >= 8);
        assertTrue(res.mustChangePassword());
        assertTrue(encoder.matches(res.temporaryPassword(), savedHash[0]));
    }

    @Test
    void createStaffRejectsWeakPasswordAndCsManagerRole() {
        when(accounts.findByUsername(any())).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.createStaffAccount(1L,
                new CreateStaffAccountRequestDto("cs02", "123456", "NV", Role.CS_STAFF, Department.CUSTOMER_SERVICE, false)));

        assertThrows(BusinessException.class, () -> service.createStaffAccount(1L,
                new CreateStaffAccountRequestDto("mgr2", "Strong@123", "Mgr2", Role.CS_MANAGER, Department.CUSTOMER_SERVICE, true)));

        assertThrows(BusinessException.class, () -> service.createStaffAccount(1L,
                new CreateStaffAccountRequestDto("tech02", "Strong@123", "NV", Role.DEPT_STAFF, Department.CUSTOMER_SERVICE, false)));
    }

    @Test
    void createStaffRejectsUsernameBelongingToSoftDeletedAccountWithRestoreHint() {
        UserAccount deleted = new UserAccount(5L, "oldstaff", "hash", Role.CS_STAFF, null, 9L, false, LocalDateTime.now());
        deleted.setDeleted(true);
        when(accounts.findByUsername("oldstaff")).thenReturn(Optional.of(deleted));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createStaffAccount(1L,
                new CreateStaffAccountRequestDto("oldstaff", "Strong@123", "NV", Role.CS_STAFF, Department.CUSTOMER_SERVICE, false)));

        assertTrue(ex.getMessage().contains("khôi phục"));
    }

    @Test
    void blockAndUnblockCustomerDoesNotTouchSpamBlockedFlag() {
        Customer spamBlockedCustomer = new Customer(
                30L, "Cu dan A", "0901234567", "a@test.com", "A1-0101", true, LocalDateTime.now());
        spamBlockedCustomer.setBlocked(true); // Án spam hiện có

        UserAccount acc = new UserAccount(
                40L, "usera", "hash", Role.CUSTOMER, 30L, null, true, LocalDateTime.now());
        acc.setAccountStatus(AccountStatus.APPROVED);

        when(accounts.findById(40L)).thenReturn(Optional.of(acc));
        when(customers.findById(30L)).thenReturn(Optional.of(spamBlockedCustomer));
        when(accounts.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountManagementResponseDto blockedRes = service.blockAccount(1L, 40L);
        assertFalse(blockedRes.active());
        assertEquals("DISABLED", blockedRes.accountStatus());
        assertEquals(Boolean.TRUE, blockedRes.customerBlocked());

        AccountManagementResponseDto unblockedRes = service.unblockAccount(1L, 40L);
        assertTrue(unblockedRes.active());
        assertEquals("APPROVED", unblockedRes.accountStatus());
        // Quan trọng: mở khóa đăng nhập không được xóa án spam customer.blocked
        assertTrue(spamBlockedCustomer.isBlocked());
        assertEquals(Boolean.TRUE, unblockedRes.customerBlocked());
    }

    @Test
    void resetPasswordAutoGeneratesAndForcesChangePassword() {
        UserAccount staffAcc = new UserAccount(
                50L, "cs01", encoder.encode("OldPass@123"), Role.CS_STAFF, null, 10L, true, LocalDateTime.now());
        staffAcc.setAccountStatus(AccountStatus.APPROVED);
        staffAcc.setMustChangePassword(false);
        Staff staff = new Staff(10L, "NV CS", Role.CS_STAFF, Department.CUSTOMER_SERVICE, true);

        when(accounts.findById(50L)).thenReturn(Optional.of(staffAcc));
        when(staffs.findById(10L)).thenReturn(Optional.of(staff));
        when(accounts.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountManagementResponseDto res = service.resetPassword(1L, 50L, new ResetPasswordRequestDto(""));

        assertNotNull(res.temporaryPassword());
        assertTrue(res.mustChangePassword());
        assertTrue(staffAcc.isMustChangePassword());
        assertTrue(encoder.matches(res.temporaryPassword(), staffAcc.getPassword()));
    }

    @Test
    void softDeleteAndRestoreProtectsUnverifiedCustomerFromBypassingApproval() {
        Customer pendingCustomer = new Customer(
                31L, "Cu dan cho duyet", "0909999999", "p@test.com", "A1-0202", false, LocalDateTime.now());
        UserAccount pendingAcc = new UserAccount(
                41L, "pendinguser", "hash", Role.CUSTOMER, 31L, null, false, LocalDateTime.now());
        pendingAcc.setAccountStatus(AccountStatus.PENDING);

        when(accounts.findById(41L)).thenReturn(Optional.of(pendingAcc));
        when(customers.findById(31L)).thenReturn(Optional.of(pendingCustomer));
        when(accounts.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customers.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountManagementResponseDto deleted = service.softDeleteAccount(1L, 41L);
        assertTrue(deleted.deleted());
        assertNotNull(deleted.deletedAt());
        assertFalse(deleted.active());
        assertFalse(pendingCustomer.isActive());

        AccountManagementResponseDto restored = service.restoreAccount(1L, 41L);
        assertFalse(restored.deleted());
        assertNull(restored.deletedAt());
        assertEquals("PENDING", restored.accountStatus());
        assertFalse(restored.active());
        assertTrue(pendingCustomer.isActive());
    }

    @Test
    void csManagerProtectionBlocksSelfMutationAndRoleEscalation() {
        UserAccount managerAcc = new UserAccount(
                99L, "csmanager", "hash", Role.CS_MANAGER, null, 1L, true, LocalDateTime.now());
        UserAccount staffAcc = new UserAccount(
                50L, "cs01", "hash", Role.CS_STAFF, null, 10L, true, LocalDateTime.now());
        Staff staff = new Staff(10L, "NV CS", Role.CS_STAFF, Department.CUSTOMER_SERVICE, true);

        when(accounts.findById(99L)).thenReturn(Optional.of(managerAcc));
        when(accounts.findById(50L)).thenReturn(Optional.of(staffAcc));
        when(staffs.findById(10L)).thenReturn(Optional.of(staff));

        assertThrows(BusinessException.class, () -> service.softDeleteAccount(1L, 99L));
        assertThrows(BusinessException.class, () -> service.blockAccount(1L, 99L));
        assertThrows(BusinessException.class, () -> service.updateAccount(1L, 50L,
                new UpdateAccountRequestDto("NV", Role.CS_MANAGER, Department.CUSTOMER_SERVICE, true, null, null, null, null)));
    }

    @Test
    void nonCsManagerIsForbidden() {
        Staff normalStaff = new Staff(2L, "NV CS", Role.CS_STAFF, Department.CUSTOMER_SERVICE, true);
        when(staffs.findById(2L)).thenReturn(Optional.of(normalStaff));

        assertThrows(ForbiddenException.class, () -> service.listAccounts(2L, null, null, false, null));
    }
}
