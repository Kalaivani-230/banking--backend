package com.example.MiniProject.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="fee_components")
public class FeeComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="policy_id", nullable=false)
    private Long policyId;

    @Column(name="component_type", nullable=false)
    private String componentType; // BASE_FEE, INTERMEDIARY_FEE, HANDLING_FEE, TAX

    @Column(name="calc_type", nullable=false)
    private String calcType; // FIXED, PERCENT, SLAB (for now FIXED/PERCENT enough)

    @Column(name = "component_value", nullable = false, precision = 18, scale = 8)
    private BigDecimal value;

    private BigDecimal minFee;
    private BigDecimal maxFee;

    @Column(name="is_estimated", nullable=false)
    private Boolean isEstimated = false; // true for intermediary fee

    private Instant createdAt;

    @PrePersist
    public void prePersist(){ createdAt = Instant.now(); }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getPolicyId() {
		return policyId;
	}

	public void setPolicyId(Long policyId) {
		this.policyId = policyId;
	}

	public String getComponentType() {
		return componentType;
	}

	public void setComponentType(String componentType) {
		this.componentType = componentType;
	}

	public String getCalcType() {
		return calcType;
	}

	public void setCalcType(String calcType) {
		this.calcType = calcType;
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}

	public BigDecimal getMinFee() {
		return minFee;
	}

	public void setMinFee(BigDecimal minFee) {
		this.minFee = minFee;
	}

	public BigDecimal getMaxFee() {
		return maxFee;
	}

	public void setMaxFee(BigDecimal maxFee) {
		this.maxFee = maxFee;
	}

	public Boolean getIsEstimated() {
		return isEstimated;
	}

	public void setIsEstimated(Boolean isEstimated) {
		this.isEstimated = isEstimated;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

    // getters & setters
    // ...
}