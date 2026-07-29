package com.example.MiniProject.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "customer_otp")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerOtp {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long customerId;

    @Column(nullable=false)
    private String otpCode;

    @Column(nullable=false)
    private String purpose; // REGISTRATION, LOGIN_2FA, TRANSFER_2FA

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOtpCode() {
		return otpCode;
	}

	public void setOtpCode(String otpCode) {
		this.otpCode = otpCode;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public Integer getAttempts() {
		return attempts;
	}

	public void setAttempts(Integer attempts) {
		this.attempts = attempts;
	}

	public Long getCustomerId() {
		return customerId;
	}

	@Column(nullable=false)
    private Instant expiresAt;

    private Instant verifiedAt;
    private Integer attempts;
    private Instant createdAt;

    @PrePersist
    void prePersist(){
        setCreatedAt(Instant.now());
        if(attempts == null) attempts = 0;
    }

	public void setCustomerId(Long customerId2) {
		customerId = customerId2;
	}

	public Instant getVerifiedAt() {
		return verifiedAt;
	}

	public void setVerifiedAt(Instant verifiedAt) {
		this.verifiedAt = verifiedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}



}