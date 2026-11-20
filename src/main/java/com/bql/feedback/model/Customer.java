package com.bql.feedback.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** CF01/CF02 - Hồ sơ cư dân (khách hàng). */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String phone;
    private String email;
    private String apartmentCode; // Mã căn hộ, vd A1-0501
    private boolean verifiedResident; // BQL đã xác minh là cư dân thật (chống tài khoản ảo)
    private boolean active;
    private boolean blocked; // Bị khóa gửi phản ánh do spam/vi phạm
    private int spamStrikes; // Số lần bị ghi nhận spam/phản ánh giả/ngôn từ xúc phạm
    private int ratingTrustScore; // 0-100, thấp thì đánh giá sao ít trọng số
    private LocalDateTime createdAt;

    protected Customer() {
    }

    public Customer(Long id, String fullName, String phone, String email, String apartmentCode,
                    boolean verifiedResident, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.apartmentCode = apartmentCode;
        this.verifiedResident = verifiedResident;
        this.createdAt = createdAt;
        this.active = true;
        this.ratingTrustScore = 100;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getApartmentCode() { return apartmentCode; }
    public void setApartmentCode(String apartmentCode) { this.apartmentCode = apartmentCode; }
    public boolean isVerifiedResident() { return verifiedResident; }
    public void setVerifiedResident(boolean verifiedResident) { this.verifiedResident = verifiedResident; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public int getSpamStrikes() { return spamStrikes; }
    public void setSpamStrikes(int spamStrikes) { this.spamStrikes = spamStrikes; }
    public int getRatingTrustScore() { return ratingTrustScore; }
    public void setRatingTrustScore(int ratingTrustScore) { this.ratingTrustScore = ratingTrustScore; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean canSubmit() { return active && !blocked; }

    /** 3 lần vi phạm thì khóa tạm, Manager mở lại. */
    public void addSpamStrike() {
        spamStrikes++;
        ratingTrustScore = Math.max(0, ratingTrustScore - 20);
        if (spamStrikes >= 3) blocked = true;
    }
}
