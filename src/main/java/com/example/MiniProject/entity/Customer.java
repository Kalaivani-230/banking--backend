package com.example.MiniProject.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

@Entity
@Table(name = "customers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String customerCode;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String mobile;

    @JsonIgnore
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String status; // ACTIVE/INACTIVE/LOCKED

    @Column(nullable = false)
    private String customerTypeCode; // RETAIL/CORPORATE

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    private java.time.Instant lockedAt;
    
    private String address;

    @Column(name = "preferred_account_type")
    private String preferredAccountType; // SAVINGS or CURRENT

    public String getPreferredAccountType() {
		return preferredAccountType;
	}

	public void setPreferredAccountType(String preferredAccountType) {
		this.preferredAccountType = preferredAccountType;
	}

	private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now(); updatedAt = Instant.now();
        if (failedLoginAttempts == null) failedLoginAttempts = 0;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCustomerCode() {
		return customerCode;
	}

	public void setCustomerCode(String customerCode) {
		this.customerCode = customerCode;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getCustomerTypeCode() {
		return customerTypeCode;
	}

	public void setCustomerTypeCode(String customerTypeCode) {
		this.customerTypeCode = customerTypeCode;
	}

	public Integer getFailedLoginAttempts() { return failedLoginAttempts; }
	public void setFailedLoginAttempts(Integer failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

	public java.time.Instant getLockedAt() { return lockedAt; }
	public void setLockedAt(java.time.Instant lockedAt) { this.lockedAt = lockedAt; }

	public String getAddress() { return address; }
	public void setAddress(String address) { this.address = address; }

	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

	public Instant getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}