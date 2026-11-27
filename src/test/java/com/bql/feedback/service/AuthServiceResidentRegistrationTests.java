package com.bql.feedback.service;

import com.bql.feedback.dto.LoginRequestDto;
import com.bql.feedback.dto.RegisterRequestDto;
import com.bql.feedback.exception.BusinessException;
import com.bql.feedback.exception.TooManyRequestsException;
import com.bql.feedback.exception.UnauthorizedException;
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
import com.bql.feedback.security.AuthenticatedUser;
import com.bql.feedback.security.JwtService;
import com.bql.feedback.security.LoginAttemptService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceResidentRegistrationTests {

    private static final String TEST_SECRET = "bql-feedback-test-jwt-secret-key-minimum-32-bytes-2026";

    private AuthService newAuthService(
            UserAccountRepository accountRepository,
            CustomerRepository customerRepository,
            RentalContractRepository contractRepository,
            StaffRepository staffRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LoginAttemptService loginAttemptService
    ) {
        return new AuthService(
                accountRepository,
                customerRepository,
                contractRepository,
                staffRepository,
                passwordEncoder,
                jwtService,
                loginAttemptService
        );
    }

    @Test
    void tenantRegistrationCreatesPendingCustomerAccountWithBcryptAndNullToken() {
        UserAccountRepository accountRepository = mock(UserAccountRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        RentalContractRepository contractRepository = mock(RentalContractRepository.class);
        StaffRepository staffRepository = mock(StaffRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        JwtService jwtService = new JwtService(TEST_SECRET, 480);
        LoginAttemptService loginAttemptService = new LoginAttemptService(5, 15);
        AuthService authService = newAuthService(
                accountRepository, customerRepository, contractRepository, staffRepository,
                passwordEncoder, jwtService, loginAttemptService);

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
        assertNull(response.token());
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
        assertNotEquals("password", accountCaptor.getValue().getPassword());
        assertTrue(passwordEncoder.matches("password", accountCaptor.getValue().getPassword()));
    }

    @Test
    void pendingAccountCannotLogInAndDoesNotCountAsFailedPassword() {
        UserAccountRepository accountRepository = mock(UserAccountRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        RentalContractRepository contractRepository = mock(RentalContractRepository.class);
        StaffRepository staffRepository = mock(StaffRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        JwtService jwtService = new JwtService(TEST_SECRET, 480);
        LoginAttemptService loginAttemptService = new LoginAttemptService(2, 15);
        AuthService authService = newAuthService(
                accountRepository, customerRepository, contractRepository, staffRepository,
                passwordEncoder, jwtService, loginAttemptService);

        UserAccount pendingAccount = new UserAccount(
                7L, "tenant01", passwordEncoder.encode("password"), Role.CUSTOMER, 42L, null, false, LocalDateTime.now());
        pendingAccount.setAccountStatus(AccountStatus.PENDING);
        when(accountRepository.findByUsername("tenant01")).thenReturn(Optional.of(pendingAccount));

        assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequestDto("tenant01", "password")));
        assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequestDto("tenant01", "password")));
        // Đúng mật khẩu nhưng PENDING không bị khóa 429
        assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequestDto("tenant01", "password")));
    }

    @Test
    void approvedCustomerLoginIssuesValidJwtToken() {
        UserAccountRepository accountRepository = mock(UserAccountRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        RentalContractRepository contractRepository = mock(RentalContractRepository.class);
        StaffRepository staffRepository = mock(StaffRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        JwtService jwtService = new JwtService(TEST_SECRET, 480);
        LoginAttemptService loginAttemptService = new LoginAttemptService(5, 15);
        AuthService authService = newAuthService(
                accountRepository, customerRepository, contractRepository, staffRepository,
                passwordEncoder, jwtService, loginAttemptService);

        Customer customer = new Customer(
                42L, "Nguyen Van An", "0901234567", "an@gmail.com", "A1-0501", true, LocalDateTime.now());
        UserAccount approved = new UserAccount(
                7L, "an@gmail.com", passwordEncoder.encode("123456"), Role.CUSTOMER, 42L, null, true, LocalDateTime.now());
        approved.setAccountStatus(AccountStatus.APPROVED);

        when(accountRepository.findByUsername("an@gmail.com")).thenReturn(Optional.of(approved));
        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));
        when(contractRepository.findByTenantCustomerIdOrderByStartDateDesc(42L)).thenReturn(List.of());
        when(accountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = authService.login(new LoginRequestDto("an@gmail.com", "123456"));

        assertNotNull(response.token());
        AuthenticatedUser parsed = jwtService.parse(response.token());
        assertEquals(7L, parsed.accountId());
        assertEquals("an@gmail.com", parsed.username());
        assertEquals("CUSTOMER", parsed.role());
        assertEquals(42L, parsed.customerId());
        assertNull(parsed.staffId());
    }

    @Test
    void wrongPasswordThrowsUnauthorizedAndLocksAfterMaxAttempts() {
        UserAccountRepository accountRepository = mock(UserAccountRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        RentalContractRepository contractRepository = mock(RentalContractRepository.class);
        StaffRepository staffRepository = mock(StaffRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        JwtService jwtService = new JwtService(TEST_SECRET, 480);
        LoginAttemptService loginAttemptService = new LoginAttemptService(2, 15);
        AuthService authService = newAuthService(
                accountRepository, customerRepository, contractRepository, staffRepository,
                passwordEncoder, jwtService, loginAttemptService);

        UserAccount approved = new UserAccount(
                7L, "an@gmail.com", passwordEncoder.encode("123456"), Role.CUSTOMER, 42L, null, true, LocalDateTime.now());
        approved.setAccountStatus(AccountStatus.APPROVED);
        when(accountRepository.findByUsername("an@gmail.com")).thenReturn(Optional.of(approved));

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequestDto("an@gmail.com", "wrong1")));
        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequestDto("an@gmail.com", "wrong2")));

        TooManyRequestsException locked = assertThrows(TooManyRequestsException.class,
                () -> authService.login(new LoginRequestDto("an@gmail.com", "123456")));
        assertTrue(locked.getRetryAfterSeconds() > 0);
    }

    @Test
    void registrationNormalizesPlus84PhoneAndLowercasesEmail() {
        UserAccountRepository accountRepository = mock(UserAccountRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        RentalContractRepository contractRepository = mock(RentalContractRepository.class);
        StaffRepository staffRepository = mock(StaffRepository.class);
        AuthService authService = newAuthService(
                accountRepository, customerRepository, contractRepository, staffRepository,
                new BCryptPasswordEncoder(), new JwtService(TEST_SECRET, 480), new LoginAttemptService(5, 15));

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
        AuthService authService = newAuthService(
                mock(UserAccountRepository.class),
                mock(CustomerRepository.class),
                mock(RentalContractRepository.class),
                mock(StaffRepository.class),
                new BCryptPasswordEncoder(),
                new JwtService(TEST_SECRET, 480),
                new LoginAttemptService(5, 15));

        assertThrows(BusinessException.class, () -> authService.registerCustomer(new RegisterRequestDto(
                "owner01", "password", "Owner Name", "123", "owner@example.com",
                "A1-0501", ResidentType.OWNER, null, null)));

        assertThrows(BusinessException.class, () -> authService.registerCustomer(new RegisterRequestDto(
                "owner01", "password", "Owner Name", "0901234567", "invalid-email",
                "A1-0501", ResidentType.OWNER, null, null)));
    }
}
