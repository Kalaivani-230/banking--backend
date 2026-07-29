package com.example.MiniProject.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="customer_limits")
public class CustomerLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="customer_id", nullable=false)
    private Long customerId;

    @Column(name="per_txn_limit", nullable=false)
    private BigDecimal perTxnLimit;

    @Column(name="daily_limit", nullable=false)
    private BigDecimal dailyLimit;

    @Column(name="monthly_limit", nullable=false)
    private BigDecimal monthlyLimit;

    @Column(name="effective_from", nullable=false)
    private Instant effectiveFrom;

    private Instant effectiveTo;

    @Column(name="updated_by")
    private Long updatedBy; // internal user id

    private Instant updatedAt;

    @PrePersist
    public void prePersist(){ updatedAt = Instant.now(); }

    @PreUpdate
    public void preUpdate(){ updatedAt = Instant.now(); }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCustomerId() {
		return customerId;
	}

	public void setCustomerId(Long customerId) {
		this.customerId = customerId;
	}

	public BigDecimal getPerTxnLimit() {
		return perTxnLimit;
	}

	public void setPerTxnLimit(BigDecimal perTxnLimit) {
		this.perTxnLimit = perTxnLimit;
	}

	public BigDecimal getDailyLimit() {
		return dailyLimit;
	}

	public void setDailyLimit(BigDecimal dailyLimit) {
		this.dailyLimit = dailyLimit;
	}

	public BigDecimal getMonthlyLimit() {
		return monthlyLimit;
	}

	public void setMonthlyLimit(BigDecimal monthlyLimit) {
		this.monthlyLimit = monthlyLimit;
	}

	public Instant getEffectiveFrom() {
		return effectiveFrom;
	}

	public void setEffectiveFrom(Instant effectiveFrom) {
		this.effectiveFrom = effectiveFrom;
	}

	public Instant getEffectiveTo() {
		return effectiveTo;
	}

	public void setEffectiveTo(Instant effectiveTo) {
		this.effectiveTo = effectiveTo;
	}

	public Long getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(Long updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	

    // getters & setters...
    // (generate via Eclipse: Source → Generate Getters and Setters)
}
