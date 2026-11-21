package com.bql.feedback.repository;

import com.bql.feedback.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<UserAccount> findByCustomerId(Long customerId);
    Optional<UserAccount> findByStaffId(Long staffId);
}
