package com.example.MiniProject.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="fee_policies")
public class FeePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="policy_name", nullable=false)
    private String policyName;

    @Column(name="corridor_id", nullable=false)
    private Long corridorId;

    @Column(name="send_currency", nullable=false, length=10)
    private String sendCurrency;

    @Column(name="receive_currency", nullable=false, length=10)
    private String receiveCurrency;

    @Column(name="channel_code", nullable=false, length=20)
    private String channelCode; // ONLINE/BRANCH

    @Column(name="customer_type_code", nullable=false, length=20)
    private String customerTypeCode; // RETAIL/CORPORATE

    @Column(name="amount_min")
    private BigDecimal amountMin;

    @Column(name="amount_max")
    private BigDecimal amountMax;

    private Integer priority;

    @Column(name="effective_from", nullable=false)
    private Instant effectiveFrom;

    private Instant effectiveTo;

    @Column(nullable=false)
    private String status; // DRAFT, PENDING_APPROVAL, ACTIVE, REJECTED, DEACTIVATED, EXPIRED

    @Column(name="version_number")
    private Integer versionNumber;

    @Column(name="created_by")
    private Long createdBy; // internal user id (Admin)

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist(){
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) status = "DRAFT";
        if (versionNumber == null) versionNumber = 1;
        if (priority == null) priority = 0;
        if (amountMin == null) amountMin = BigDecimal.ZERO;
    }

    @PreUpdate
    public void preUpdate(){
        updatedAt = Instant.now();
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPolicyName() {
		return policyName;
	}

	public void setPolicyName(String policyName) {
		this.policyName = policyName;
	}

	public Long getCorridorId() {
		return corridorId;
	}

	public void setCorridorId(Long corridorId) {
		this.corridorId = corridorId;
	}

	public String getSendCurrency() {
		return sendCurrency;
	}

	public void setSendCurrency(String sendCurrency) {
		this.sendCurrency = sendCurrency;
	}

	public String getReceiveCurrency() {
		return receiveCurrency;
	}

	public void setReceiveCurrency(String receiveCurrency) {
		this.receiveCurrency = receiveCurrency;
	}

	public String getChannelCode() {
		return channelCode;
	}

	public void setChannelCode(String channelCode) {
		this.channelCode = channelCode;
	}

	public String getCustomerTypeCode() {
		return customerTypeCode;
	}

	public void setCustomerTypeCode(String customerTypeCode) {
		this.customerTypeCode = customerTypeCode;
	}

	public BigDecimal getAmountMin() {
		return amountMin;
	}

	public void setAmountMin(BigDecimal amountMin) {
		this.amountMin = amountMin;
	}

	public BigDecimal getAmountMax() {
		return amountMax;
	}

	public void setAmountMax(BigDecimal amountMax) {
		this.amountMax = amountMax;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getVersionNumber() {
		return versionNumber;
	}

	public void setVersionNumber(Integer versionNumber) {
		this.versionNumber = versionNumber;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

    // getters & setters (generate via Eclipse)
    // ...
}