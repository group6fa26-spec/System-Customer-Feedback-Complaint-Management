package com.bql.feedback.repository;

import com.bql.feedback.model.RentalContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RentalContractRepository extends JpaRepository<RentalContract, Long> {
    List<RentalContract> findByTenantCustomerIdOrderByStartDateDesc(Long tenantCustomerId);
}