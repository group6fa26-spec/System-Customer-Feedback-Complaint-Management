package com.bql.feedback.service;

import com.bql.feedback.dto.AccountApprovalResponseDto;
import com.bql.feedback.exception.BusinessException;
import com.bql.feedback.exception.ForbiddenException;
import com.bql.feedback.exception.NotFoundException;
import com.bql.feedback.model.AccountStatus;
import com.bql.feedback.model.Customer;
import com.bql.feedback.model.Department;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountApprovalServiceTests {

    private UserAccountRepository userAccountRepository;
    private CustomerRepository customerRepository;
    private RentalContractRepository rentalContractRepository;
    private StaffRepository staffRepository;
    private AccountApprovalService approvalService;

    private Staff activeManager;

    @BeforeEach
    void setUp() {
        userAccountRepository = mock(UserAccountRepository.class);
        customerRepository = mock(CustomerRepository.class);
        rentalContractRepository = mock(RentalContractRepository.class);
        staffRepository = mock(StaffRepository.class);
        approvalService = new AccountApprovalService(
                userAccountRepository, customerRepository, rentalContractRepository, staffRepository);

        activeManager = new Staff(100L, "Manager", Role.CS_MANAGER, Department.CUSTOMER_SERVICE, true);
        when(staffRepository.findById(100L)).thenReturn(Optional.of(activeManager));
    }

    @Test
    void onlyActiveCsManagerIsAuthorized() {
        Staff csStaff = new Staff(101L, "CS Staff", Role.CS_STAFF, Department.CUSTOMER_SERVICE, true);
        Staff deptStaff = new Staff(102L, "Tech Staff", Role.DEPT_STAFF, Department.TECHNICAL, true);
        Staff inactiveManager = new Staff(103L, "Inactive Manager", Role.CS_MANAGER, Department.CUSTOMER_SERVICE, true);
        inactiveManager.setActive(false);

        when(staffRepository.findById(101L)).thenReturn(Optional.of(csStaff));
        when(staffRepository.findById(102L)).thenReturn(Optional.of(deptStaff));
        when(staffRepository.findById(103L)).thenReturn(Optional.of(inactiveManager));
        when(staffRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> approvalService.getPendingAccounts(101L));
        assertThrows(ForbiddenException.class, () -> approvalService.approve(1L, 102L));
        assertThrows(ForbiddenException.class, () -> approvalService.reject(1L, 103L));
        assertThrows(ForbiddenException.class, () -> approvalService.approve(1L, 999L));
        assertThrows(ForbiddenException.class, () -> approvalService.approve(1L, null));
    }

    @Test
    void approveOwnerAccountSetsApprovedActiveAndVerified() {
        Customer owner = new Customer(
                10L, "Nguyen Van A", "0901111111", "a@example.com", "A1-0501", false, LocalDateTime.now());
        owner.setResidentType(ResidentType.OWNER);
        UserAccount pending = new UserAccount(
                1L, "owner01", "123456", Role.CUSTOMER, 10L, null, false, LocalDateTime.now());
        pending.setAccountStatus(AccountStatus.PENDING);

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(owner));
        when(rentalContractRepository.findByTenantCustomerIdOrderByStartDateDesc(10L)).thenReturn(List.of());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountApprovalResponseDto result = approvalService.approve(1L, 100L);

        assertEquals("APPROVED", result.accountStatus());
        assertTrue(result.accountActive());
        assertTrue(result.verifiedResident());
        assertTrue(result.customerActive());
    }

    @Test
    void approveTenantAccountActivatesValidPendingContract() {
        Customer tenant = new Customer(
                20L, "Tran Thi B", "0902222222", "b@example.com", "B1-0802", false, LocalDateTime.now());
        tenant.setResidentType(ResidentType.TENANT);
        UserAccount pending = new UserAccount(
                2L, "tenant01", "123456", Role.CUSTOMER, 20L, null, false, LocalDateTime.now());
        pending.setAccountStatus(AccountStatus.PENDING);

        RentalContract pendingContract = new RentalContract(
                5L, 20L, "B1-0802", LocalDate.now().minusDays(1), LocalDate.now().plusMonths(6),
                RentalContractStatus.PENDING, LocalDateTime.now());

        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(pending));
        when(customerRepository.findById(20L)).thenReturn(Optional.of(tenant));
        when(rentalContractRepository.findByTenantCustomerIdOrderByStartDateDesc(20L))
                .thenReturn(List.of(pendingContract));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountApprovalResponseDto result = approvalService.approve(2L, 100L);

        assertEquals("APPROVED", result.accountStatus());
        assertTrue(result.accountActive());
        assertTrue(result.verifiedResident());
        assertEquals(1, result.rentalContracts().size());
        assertEquals("ACTIVE", result.rentalContracts().get(0).status());
        verify(rentalContractRepository).saveAll(any());
    }

    @Test
    void approveTenantAccountRejectsExpiredContract() {
        Customer tenant = new Customer(
                21L, "Expired Tenant", "0903333333", "exp@example.com", "B1-0901", false, LocalDateTime.now());
        tenant.setResidentType(ResidentType.TENANT);
        UserAccount pending = new UserAccount(
                3L, "tenant_exp", "123456", Role.CUSTOMER, 21L, null, false, LocalDateTime.now());
        pending.setAccountStatus(AccountStatus.PENDING);

        RentalContract expiredContract = new RentalContract(
                6L, 21L, "B1-0901", LocalDate.now().minusMonths(12), LocalDate.now().minusDays(1),
                RentalContractStatus.PENDING, LocalDateTime.now());

        when(userAccountRepository.findById(3L)).thenReturn(Optional.of(pending));
        when(customerRepository.findById(21L)).thenReturn(Optional.of(tenant));
        when(rentalContractRepository.findByTenantCustomerIdOrderByStartDateDesc(21L))
                .thenReturn(List.of(expiredContract));

        assertThrows(BusinessException.class, () -> approvalService.approve(3L, 100L));
        verify(customerRepository, never()).save(any());
        verify(userAccountRepository, never()).save(any());
        verify(rentalContractRepository, never()).saveAll(any());
    }

    @Test
    void rejectTenantAccountSetsRejectedInactiveAndTerminatesPendingContract() {
        Customer tenant = new Customer(
                22L, "Rejected Tenant", "0904444444", "rej@example.com", "C1-0101", false, LocalDateTime.now());
        tenant.setResidentType(ResidentType.TENANT);
        UserAccount pending = new UserAccount(
                4L, "tenant_rej", "123456", Role.CUSTOMER, 22L, null, false, LocalDateTime.now());
        pending.setAccountStatus(AccountStatus.PENDING);

        RentalContract pendingContract = new RentalContract(
                7L, 22L, "C1-0101", LocalDate.now(), LocalDate.now().plusMonths(6),
                RentalContractStatus.PENDING, LocalDateTime.now());

        when(userAccountRepository.findById(4L)).thenReturn(Optional.of(pending));
        when(customerRepository.findById(22L)).thenReturn(Optional.of(tenant));
        when(rentalContractRepository.findByTenantCustomerIdOrderByStartDateDesc(22L))
                .thenReturn(List.of(pendingContract));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountApprovalResponseDto result = approvalService.reject(4L, 100L);

        assertEquals("REJECTED", result.accountStatus());
        assertFalse(result.accountActive());
        assertFalse(result.verifiedResident());
        assertFalse(result.customerActive());
        assertEquals("TERMINATED", result.rentalContracts().get(0).status());
        verify(rentalContractRepository).saveAll(any());
    }

    @Test
    void cannotApproveOrRejectNonPendingOrStaffAccount() {
        UserAccount alreadyApproved = new UserAccount(
                5L, "owner02", "123456", Role.CUSTOMER, 30L, null, true, LocalDateTime.now());
        alreadyApproved.setAccountStatus(AccountStatus.APPROVED);
        UserAccount staffAccount = new UserAccount(
                6L, "cs01@bql.vn", "123456", Role.CS_STAFF, null, 101L, true, LocalDateTime.now());
        staffAccount.setAccountStatus(AccountStatus.PENDING);

        when(userAccountRepository.findById(5L)).thenReturn(Optional.of(alreadyApproved));
        when(userAccountRepository.findById(6L)).thenReturn(Optional.of(staffAccount));
        when(userAccountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> approvalService.approve(5L, 100L));
        assertThrows(BusinessException.class, () -> approvalService.reject(5L, 100L));
        assertThrows(BusinessException.class, () -> approvalService.approve(6L, 100L));
        assertThrows(NotFoundException.class, () -> approvalService.approve(99L, 100L));
    }

    @Test
    void getPendingAccountsUsesBatchQueriesWithoutNPlusOne() {
        UserAccount acc1 = new UserAccount(
                1L, "owner01", "123456", Role.CUSTOMER, 10L, null, false, LocalDateTime.now());
        acc1.setAccountStatus(AccountStatus.PENDING);
        UserAccount acc2 = new UserAccount(
                2L, "tenant01", "123456", Role.CUSTOMER, 20L, null, false, LocalDateTime.now());
        acc2.setAccountStatus(AccountStatus.PENDING);

        Customer cus1 = new Customer(
                10L, "Owner 1", "0901111111", "o1@example.com", "A1-0101", false, LocalDateTime.now());
        cus1.setResidentType(ResidentType.OWNER);
        Customer cus2 = new Customer(
                20L, "Tenant 1", "0902222222", "t1@example.com", "B1-0202", false, LocalDateTime.now());
        cus2.setResidentType(ResidentType.TENANT);

        RentalContract contract2 = new RentalContract(
                9L, 20L, "B1-0202", LocalDate.now(), LocalDate.now().plusMonths(12),
                RentalContractStatus.PENDING, LocalDateTime.now());

        when(userAccountRepository.findByRoleAndAccountStatusOrderByCreatedAtAsc(Role.CUSTOMER, AccountStatus.PENDING))
                .thenReturn(List.of(acc1, acc2));
        when(customerRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(cus1, cus2));
        when(rentalContractRepository.findByTenantCustomerIdInOrderByStartDateDesc(List.of(10L, 20L)))
                .thenReturn(List.of(contract2));

        List<AccountApprovalResponseDto> results = approvalService.getPendingAccounts(100L);

        assertEquals(2, results.size());
        assertEquals("Owner 1", results.get(0).fullName());
        assertEquals(0, results.get(0).rentalContracts().size());
        assertEquals("Tenant 1", results.get(1).fullName());
        assertEquals(1, results.get(1).rentalContracts().size());

        verify(customerRepository, times(1)).findAllById(anyCollection());
        verify(rentalContractRepository, times(1)).findByTenantCustomerIdInOrderByStartDateDesc(anyCollection());
        verify(customerRepository, never()).findById(any());
        verify(rentalContractRepository, never()).findByTenantCustomerIdOrderByStartDateDesc(any());
    }
}
