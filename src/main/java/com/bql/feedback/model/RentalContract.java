package com.bql.feedback.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_contracts")
public class RentalContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantCustomerId;

    @Column(nullable = false, length = 50)
    private String apartmentCode;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RentalContractStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected RentalContract() {
    }

    public RentalContract(Long id, Long tenantCustomerId, String apartmentCode,
                          LocalDate startDate, LocalDate endDate,
                          RentalContractStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.tenantCustomerId = tenantCustomerId;
        this.apartmentCode = apartmentCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantCustomerId() { return tenantCustomerId; }
    public void setTenantCustomerId(Long tenantCustomerId) { this.tenantCustomerId = tenantCustomerId; }
    public String getApartmentCode() { return apartmentCode; }
    public void setApartmentCode(String apartmentCode) { this.apartmentCode = apartmentCode; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public RentalContractStatus getStatus() { return status; }
    public void setStatus(RentalContractStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}