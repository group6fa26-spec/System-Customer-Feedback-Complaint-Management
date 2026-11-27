package com.bql.feedback.security;

import com.bql.feedback.model.AccountStatus;
import com.bql.feedback.model.Department;
import com.bql.feedback.model.Role;
import com.bql.feedback.model.Staff;
import com.bql.feedback.model.UserAccount;
import com.bql.feedback.repository.StaffRepository;
import com.bql.feedback.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CsManagerBootstrapRunnerTests {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void createsSingleCsManagerAndIndexesWhenAbsent() {
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        StaffRepository staffs = mock(StaffRepository.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);

        when(accounts.findByRoleOrderByIdAsc(Role.CS_MANAGER)).thenReturn(List.of());
        when(staffs.findByRoleOrderByIdAsc(Role.CS_MANAGER)).thenReturn(List.of());
        when(accounts.existsByUsername("csmanager")).thenReturn(false);
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

        CsManagerBootstrapRunner runner = new CsManagerBootstrapRunner(
                accounts, staffs, encoder, jdbc, TransactionOperations.withoutTransaction(),
                "csmanager", "CsManager@123", "Quản lý CSKH");

        runner.run(null);

        verify(jdbc).execute(CsManagerBootstrapRunner.CREATE_STAFF_UNIQUE_INDEX_SQL);
        verify(jdbc).execute(CsManagerBootstrapRunner.CREATE_USER_ACCOUNT_UNIQUE_INDEX_SQL);

        ArgumentCaptor<Staff> staffCaptor = ArgumentCaptor.forClass(Staff.class);
        verify(staffs).save(staffCaptor.capture());
        Staff savedStaff = staffCaptor.getValue();
        assertEquals("Quản lý CSKH", savedStaff.getFullName());
        assertEquals(Role.CS_MANAGER, savedStaff.getRole());
        assertEquals(Department.CUSTOMER_SERVICE, savedStaff.getDepartment());
        assertTrue(savedStaff.isActive());

        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(accounts).save(accountCaptor.capture());
        UserAccount savedAccount = accountCaptor.getValue();
        assertEquals("csmanager", savedAccount.getUsername());
        assertEquals(Role.CS_MANAGER, savedAccount.getRole());
        assertEquals(AccountStatus.APPROVED, savedAccount.getAccountStatus());
        assertEquals(10L, savedAccount.getStaffId());
        assertTrue(savedAccount.isActive());
        assertNotEquals("CsManager@123", savedAccount.getPassword());
        assertTrue(encoder.matches("CsManager@123", savedAccount.getPassword()));
    }

    @Test
    void keepsExistingCsManagerUntouchedWithoutOverwritingPassword() {
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        StaffRepository staffs = mock(StaffRepository.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);

        String existingHash = encoder.encode("OldSecret@999");
        Staff existingStaff = new Staff(5L, "Quan ly cu", Role.CS_MANAGER, Department.CUSTOMER_SERVICE, true);
        UserAccount existingAcc = new UserAccount(
                7L, "old_manager", existingHash, Role.CS_MANAGER, null, 5L, true, LocalDateTime.now());
        existingAcc.setAccountStatus(AccountStatus.APPROVED);

        when(accounts.findByRoleOrderByIdAsc(Role.CS_MANAGER)).thenReturn(List.of(existingAcc));
        when(staffs.findByRoleOrderByIdAsc(Role.CS_MANAGER)).thenReturn(List.of(existingStaff));

        CsManagerBootstrapRunner runner = new CsManagerBootstrapRunner(
                accounts, staffs, encoder, jdbc, TransactionOperations.withoutTransaction(),
                "csmanager", "CsManager@123", "Quản lý CSKH");

        runner.run(null);

        verify(jdbc).execute(CsManagerBootstrapRunner.CREATE_STAFF_UNIQUE_INDEX_SQL);
        verify(jdbc).execute(CsManagerBootstrapRunner.CREATE_USER_ACCOUNT_UNIQUE_INDEX_SQL);
        verify(accounts, never()).save(any());
        verify(staffs, never()).save(any());
        assertEquals("old_manager", existingAcc.getUsername());
        assertEquals(existingHash, existingAcc.getPassword());
    }

    @Test
    void demotesExcessCsManagersBeforeCreatingUniqueIndexes() {
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        StaffRepository staffs = mock(StaffRepository.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);

        Staff s1 = new Staff(1L, "Manager chinh", Role.CS_MANAGER, Department.CUSTOMER_SERVICE, true);
        Staff s2 = new Staff(2L, "Manager du thua", Role.CS_MANAGER, Department.CUSTOMER_SERVICE, true);
        UserAccount a1 = new UserAccount(
                10L, "mgr1", encoder.encode("Pass1"), Role.CS_MANAGER, null, 1L, true, LocalDateTime.now());
        a1.setAccountStatus(AccountStatus.APPROVED);
        UserAccount a2 = new UserAccount(
                11L, "mgr2", encoder.encode("Pass2"), Role.CS_MANAGER, null, 2L, true, LocalDateTime.now());
        a2.setAccountStatus(AccountStatus.APPROVED);

        when(accounts.findByRoleOrderByIdAsc(Role.CS_MANAGER))
                .thenReturn(List.of(a1, a2))
                .thenReturn(List.of(a1));
        when(staffs.findByRoleOrderByIdAsc(Role.CS_MANAGER)).thenReturn(List.of(s1, s2));

        CsManagerBootstrapRunner runner = new CsManagerBootstrapRunner(
                accounts, staffs, encoder, jdbc, TransactionOperations.withoutTransaction(),
                "csmanager", "CsManager@123", "Quản lý CSKH");

        runner.run(null);

        assertEquals(Role.CS_MANAGER, a1.getRole());
        assertEquals(Role.CS_STAFF, a2.getRole());
        assertEquals(Role.CS_MANAGER, s1.getRole());
        assertEquals(Role.CS_STAFF, s2.getRole());
        verify(accounts).saveAll(List.of(a2));
        verify(staffs).saveAll(List.of(s2));
        verify(jdbc).execute(CsManagerBootstrapRunner.CREATE_STAFF_UNIQUE_INDEX_SQL);
        verify(jdbc).execute(CsManagerBootstrapRunner.CREATE_USER_ACCOUNT_UNIQUE_INDEX_SQL);
        verify(accounts, never()).save(any());
    }

    @Test
    void reusesExistingUnlinkedCsManagerStaffWhenAccountMissing() {
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        StaffRepository staffs = mock(StaffRepository.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);

        Staff existingStaff = new Staff(33L, "Quan ly san co", Role.CS_MANAGER, Department.CUSTOMER_SERVICE, true);
        when(accounts.findByRoleOrderByIdAsc(Role.CS_MANAGER)).thenReturn(List.of());
        when(staffs.findByRoleOrderByIdAsc(Role.CS_MANAGER)).thenReturn(List.of(existingStaff));
        when(accounts.findByStaffId(33L)).thenReturn(Optional.empty());
        when(accounts.existsByUsername("csmanager")).thenReturn(false);
        when(accounts.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount ua = inv.getArgument(0);
            ua.setId(44L);
            return ua;
        });

        CsManagerBootstrapRunner runner = new CsManagerBootstrapRunner(
                accounts, staffs, encoder, jdbc, TransactionOperations.withoutTransaction(),
                "csmanager", "CsManager@123", "Quản lý CSKH");

        runner.run(null);

        verify(staffs, never()).save(any());
        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(accounts).save(accountCaptor.capture());
        assertEquals(33L, accountCaptor.getValue().getStaffId());
    }
}
