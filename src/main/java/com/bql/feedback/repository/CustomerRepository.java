package com.bql.feedback.repository;

import com.bql.feedback.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** CRUD đầy đủ do JpaRepository cung cấp. Khuyến nghị "xóa" = đặt active=false thay vì xóa dòng. */
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByEmailIgnoreCase(String email);
    Optional<Customer> findByPhone(String phone);
    boolean existsByEmail(String email);
    List<Customer> findByApartmentCode(String apartmentCode);
    List<Customer> findByBlockedTrue();
}
