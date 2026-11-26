package com.bql.feedback.service;

import com.bql.feedback.dto.LoginRequestDto;
import com.bql.feedback.dto.RegisterRequestDto;
import com.bql.feedback.exception.BusinessException;
import com.bql.feedback.model.AccountStatus;
import com.bql.feedback.model.Customer;
import com.bql.feedback.model.RentalContract;
import com.bql.feedback.model.RentalContractStatus;
import com.bql.feedback.model.ResidentType;
import com.bql.feedback.model.Role;
import com.bql.feedback.model.UserAccount;
import com.bql.feedback.repository.CustomerRepository;
import com.bql.feedback.repository.RentalContractRepository;
import com.bql.feedback.repository.StaffRepository;
import com.bql.feedback.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceResidentRegistrationTests {

    @Test
    void tenantRegistrationCreatesPendingCustomerAccountAndContract() {
        UserAccountRepository accountRepository = mock(UserAccountRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        RentalContractRepository contractRepository = mock(RentalContractRepository.class);
        StaffRepository staffRepository = mock(StaffRepository.class);
        AuthService authService = new AuthService(
                accountRepository, customerRepository, contractRepository, staffRepository);

        when(accountRepository.existsByUsername("tenant01")).thenReturn(false);
        when(customerRepository.existsByEmail("tenant@example.com")).thenReturn(false);
        when(customerRepository.findByPhone("0900000000")).thenReturn(Optional.empty());
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(12);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setId(42L);
            return customer;
        });
        when(contractRepository.save(any(RentalContract.class))).thenAnswer(invocation -> {
            RentalContract contract = invocation.getArgument(0);
            contract.setId(3L);
            return contract;
        });
        when(contractRepository.findByTenantCustomerIdOrderByStartDateDesc(42L)).thenReturn(List.of(
                new RentalContract(3L, 42L, "A1-0501", startDate, endDate,
                        RentalContractStatus.PENDING, LocalDateTime.now())));
        when(accountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount account = invocation.getArgument(0);
            account.setId(7L);
            return account;
        });

        var response = authService.registerCustomer(new RegisterRequestDto(
                "tenant01", "password", "Tenant Name", "0900000000", "tenant@example.com",
                "A1-0501", ResidentType.TENANT, startDate, endDate));

        assertEquals("CUSTOMER", response.role());
        assertEquals("TENANT", response.residentType());
        assertEquals("PENDING", response.accountStatus());
        assertEquals(1, response.rentalContracts().size());
        assertEquals(startDate, response.rentalContracts().get(0).startDate());
        assertEquals(endDate, response.rentalContracts().get(0).endDate());

        var contractCaptor = org.mockito.ArgumentCaptor.forClass(RentalContract.class);
        verify(contractRepository).save(contractCaptor.capture());
        assertEquals(42L, contractCaptor.getValue().getTenantCustomerId());
        assertEquals(RentalContractStatus.PENDING, contractCaptor.getValue().getStatus());

        var accountCaptor = org.mockito.ArgumentCaptor.forClass(UserAccount.class);
        verify(accountRepository).save(accountCaptor.capture());
        assertEquals(Role.CUSTOMER, accountCaptor.getValue().getRole());
        assertEquals(AccountStatus.PENDING, accountCaptor.getValue().getAccountStatus());
        assertFalse(accountCaptor.getValue().isActive());
    }

    @Test
    void pendingAccountCannotLogIn() {
        UserAccountRepository accountRepository = mock(UserAccountRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        RentalContractRepository contractRepository = mock(RentalContractRepository.class);
        StaffRepository staffRepository = mock(StaffRepository.class);
        AuthService authService = new AuthService(
                accountRepository, customerRepository, contractRepository, staffRepository);

        UserAccount pendingAccount = new UserAccount(
                7L, "tenant01", "password", Role.CUSTOMER, 42L, null, false, LocalDateTime.now());
        pendingAccount.setAccountStatus(AccountStatus.PENDING);
        when(accountRepository.findByUsername("tenant01")).thenReturn(Optional.of(pendingAccount));

        assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequestDto("tenant01", "password")));
    }

    @Test
    void registrationNormalizesPlus84PhoneAndLowercasesEmail() {
        UserAccountRepository accountRepository = mock(UserAccountRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        RentalContractRepository contractRepository = mock(RentalContractRepository.class);
        StaffRepository staffRepository = mock(StaffRepository.class);
        AuthService authService = new AuthService(
                accountRepository, customerRepository, contractRepository, staffRepository);

        when(accountRepository.existsByUsername("owner01")).thenReturn(false);
        when(customerRepository.existsByEmail("owner@example.com")).thenReturn(false);
        when(customerRepository.findByPhone("0901234567")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setId(50L);
            return customer;
        });
        when(contractRepository.findByTenantCustomerIdOrderByStartDateDesc(50L)).thenReturn(List.of());
        when(accountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount account = invocation.getArgument(0);
            account.setId(10L);
            return account;
        });

        authService.registerCustomer(new RegisterRequestDto(
                "owner01", "password", "Owner Name", " +84901234567 ", " Owner@Example.COM ",
                "A1-0501", ResidentType.OWNER, null, null));

        var customerCaptor = org.mockito.ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        assertEquals("0901234567", customerCaptor.getValue().getPhone());
        assertEquals("owner@example.com", customerCaptor.getValue().getEmail());
    }

    @Test
    void registrationRejectsInvalidEmailAndPhone() {
        AuthService authService = new AuthService(
                mock(UserAccountRepository.class),
                mock(CustomerRepository.class),
                mock(RentalContractRepository.class),
                mock(StaffRepository.class));

        assertThrows(BusinessException.class, () -> authService.registerCustomer(new RegisterRequestDto(
                "owner01", "password", "Owner Name", "123", "owner@example.com",
                "A1-0501", ResidentType.OWNER, null, null)));

        assertThrows(BusinessException.class, () -> authService.registerCustomer(new RegisterRequestDto(
                "owner01", "password", "Owner Name", "0901234567", "invalid-email",
                "A1-0501", ResidentType.OWNER, null, null)));
    }
}