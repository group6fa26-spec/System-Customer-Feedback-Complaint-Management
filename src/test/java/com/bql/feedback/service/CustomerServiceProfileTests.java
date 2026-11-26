package com.bql.feedback.service;

import com.bql.feedback.dto.CustomerResponseDto;
import com.bql.feedback.dto.UpdateCustomerProfileRequestDto;
import com.bql.feedback.exception.BusinessException;
import com.bql.feedback.exception.NotFoundException;
import com.bql.feedback.model.Customer;
import com.bql.feedback.repository.CustomerRepository;
import com.bql.feedback.repository.RentalContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerServiceProfileTests {

    private CustomerRepository customerRepository;
    private RentalContractRepository rentalContractRepository;
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        rentalContractRepository = mock(RentalContractRepository.class);
        customerService = new CustomerService(customerRepository, rentalContractRepository);
        when(rentalContractRepository.findByTenantCustomerIdOrderByStartDateDesc(any()))
                .thenReturn(List.of());
    }

    @Test
    void selfUpdateNormalizesFieldsAndPreservesApartmentAndVerified() {
        Customer existing = new Customer(
                1L, "Nguyen Van A", "0901111111", "a@example.com", "A1-0501", true, LocalDateTime.now());
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.findByEmailIgnoreCase("new.email@example.com")).thenReturn(Optional.empty());
        when(customerRepository.findByPhone("0902222222")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponseDto updated = customerService.updateProfile(
                1L,
                new UpdateCustomerProfileRequestDto("  Nguyen Van An  ", "  0902222222 ", "  New.Email@Example.COM  "),
                true
        );

        assertEquals("Nguyen Van An", updated.fullName());
        assertEquals("0902222222", updated.phone());
        assertEquals("new.email@example.com", updated.email());
        assertEquals("A1-0501", updated.apartmentCode());
        assertTrue(updated.verifiedResident());
    }

    @Test
    void updateAllowsKeepingOwnEmailAndPhone() {
        Customer existing = new Customer(
                1L, "Nguyen Van A", "0901111111", "a@example.com", "A1-0501", true, LocalDateTime.now());
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.findByEmailIgnoreCase("a@example.com")).thenReturn(Optional.of(existing));
        when(customerRepository.findByPhone("0901111111")).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponseDto updated = customerService.updateProfile(
                1L,
                new UpdateCustomerProfileRequestDto("Nguyen Van A Updated", "0901111111", "A@EXAMPLE.COM"),
                true
        );

        assertEquals("Nguyen Van A Updated", updated.fullName());
        assertEquals("a@example.com", updated.email());
    }

    @Test
    void updateRejectsDuplicateEmailOfAnotherCustomer() {
        Customer current = new Customer(
                1L, "Nguyen Van A", "0901111111", "a@example.com", "A1-0501", true, LocalDateTime.now());
        Customer other = new Customer(
                2L, "Tran Van B", "0902222222", "b@example.com", "A1-0502", true, LocalDateTime.now());
        when(customerRepository.findById(1L)).thenReturn(Optional.of(current));
        when(customerRepository.findByEmailIgnoreCase("b@example.com")).thenReturn(Optional.of(other));

        assertThrows(BusinessException.class, () -> customerService.updateProfile(
                1L,
                new UpdateCustomerProfileRequestDto("Nguyen Van A", "0901111111", "B@Example.com"),
                true
        ));
    }

    @Test
    void updateRejectsDuplicatePhoneOfAnotherCustomer() {
        Customer current = new Customer(
                1L, "Nguyen Van A", "0901111111", "a@example.com", "A1-0501", true, LocalDateTime.now());
        Customer other = new Customer(
                2L, "Tran Van B", "0902222222", "b@example.com", "A1-0502", true, LocalDateTime.now());
        when(customerRepository.findById(1L)).thenReturn(Optional.of(current));
        when(customerRepository.findByEmailIgnoreCase("a@example.com")).thenReturn(Optional.of(current));
        when(customerRepository.findByPhone("0902222222")).thenReturn(Optional.of(other));

        assertThrows(BusinessException.class, () -> customerService.updateProfile(
                1L,
                new UpdateCustomerProfileRequestDto("Nguyen Van A", "0902222222", "a@example.com"),
                true
        ));
    }

    @Test
    void blockedCustomerCannotSelfUpdateButStaffCanUpdate() {
        Customer blocked = new Customer(
                1L, "Blocked User", "0901111111", "blocked@example.com", "A1-0501", true, LocalDateTime.now());
        blocked.setBlocked(true);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(blocked));
        when(customerRepository.findByEmailIgnoreCase("blocked@example.com")).thenReturn(Optional.of(blocked));
        when(customerRepository.findByPhone("0901111111")).thenReturn(Optional.of(blocked));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCustomerProfileRequestDto request =
                new UpdateCustomerProfileRequestDto("Updated Name", "0901111111", "blocked@example.com");

        assertThrows(BusinessException.class, () -> customerService.updateProfile(1L, request, true));

        CustomerResponseDto byStaff = customerService.updateProfile(1L, request, false);
        assertEquals("Updated Name", byStaff.fullName());
    }

    @Test
    void inactiveCustomerCannotBeUpdated() {
        Customer inactive = new Customer(
                1L, "Inactive User", "0901111111", "inactive@example.com", "A1-0501", true, LocalDateTime.now());
        inactive.setActive(false);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThrows(BusinessException.class, () -> customerService.updateProfile(
                1L,
                new UpdateCustomerProfileRequestDto("New Name", "0901111111", "inactive@example.com"),
                false
        ));
    }

    @Test
    void missingCustomerThrowsNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> customerService.updateProfile(
                99L,
                new UpdateCustomerProfileRequestDto("New Name", "0901111111", "a@example.com"),
                true
        ));
    }

    @Test
    void blankFieldsThrowBusinessException() {
        assertThrows(BusinessException.class, () -> customerService.updateProfile(
                1L,
                new UpdateCustomerProfileRequestDto("   ", "0901111111", "a@example.com"),
                true
        ));
        assertThrows(BusinessException.class, () -> customerService.updateProfile(
                1L,
                new UpdateCustomerProfileRequestDto("Name", " ", "a@example.com"),
                true
        ));
        assertThrows(BusinessException.class, () -> customerService.updateProfile(
                1L,
                new UpdateCustomerProfileRequestDto("Name", "0901111111", ""),
                true
        ));
    }

    @Test
    void updateRejectsInvalidEmailFormat() {
        assertThrows(BusinessException.class, () -> customerService.updateProfile(
                1L,
                new UpdateCustomerProfileRequestDto("Name", "0901111111", "abc"),
                true
        ));
        assertThrows(BusinessException.class, () -> customerService.updateProfile(
                1L,
                new UpdateCustomerProfileRequestDto("Name", "0901111111", "a@b"),
                true
        ));
    }

    @Test
    void updateRejectsInvalidPhoneFormat() {
        assertThrows(BusinessException.class, () -> customerService.updateProfile(
                1L,
                new UpdateCustomerProfileRequestDto("Name", "123", "a@example.com"),
                true
        ));
        assertThrows(BusinessException.class, () -> customerService.updateProfile(
                1L,
                new UpdateCustomerProfileRequestDto("Name", "09012abcde", "a@example.com"),
                true
        ));
        assertThrows(BusinessException.class, () -> customerService.updateProfile(
                1L,
                new UpdateCustomerProfileRequestDto("Name", "09012345678", "a@example.com"),
                true
        ));
    }

    @Test
    void updateNormalizesPlus84PhoneAndDetectsDuplicateWithLeadingZero() {
        Customer current = new Customer(
                1L, "Nguyen Van A", "0901111111", "a@example.com", "A1-0501", true, LocalDateTime.now());
        when(customerRepository.findById(1L)).thenReturn(Optional.of(current));
        when(customerRepository.findByEmailIgnoreCase("a@example.com")).thenReturn(Optional.of(current));
        when(customerRepository.findByPhone("0902222222")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponseDto updated = customerService.updateProfile(
                1L,
                new UpdateCustomerProfileRequestDto("Nguyen Van A", " +84902222222 ", "a@example.com"),
                true
        );
        assertEquals("0902222222", updated.phone());

        Customer other = new Customer(
                2L, "Tran Van B", "0903333333", "b@example.com", "A1-0502", true, LocalDateTime.now());
        when(customerRepository.findByPhone("0903333333")).thenReturn(Optional.of(other));

        assertThrows(BusinessException.class, () -> customerService.updateProfile(
                1L,
                new UpdateCustomerProfileRequestDto("Nguyen Van A", "+84903333333", "a@example.com"),
                true
        ));
    }
}
