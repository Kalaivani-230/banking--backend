package com.example.MiniProject.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="approvals")
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="entity_type", nullable=false)
    private String entityType; // FEE_POLICY, FX_MARKUP_RULE

    @Column(name="entity_id", nullable=false)
    private Long entityId;

    @Column(name="submitted_by", nullable=false)
    private Long submittedBy;

    @Column(name="submitted_at", nullable=false)
    private Instant submittedAt;

    @Column(name="approver_id")
    private Long approverId;

    private String decision; // APPROVED, REJECTED
    private String decisionReason;
    private Instant decidedAt;

    @Column(nullable=false)
    private String status; // PENDING, COMPLETED

    @PrePersist
    public void prePersist(){
        submittedAt = Instant.now();
        if (status == null) status = "PENDING";
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public Long getEntityId() {
		return entityId;
	}

	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	public Long getSubmittedBy() {
		return submittedBy;
	}

	public void setSubmittedBy(Long submittedBy) {
		this.submittedBy = submittedBy;
	}

	public Instant getSubmittedAt() {
		return submittedAt;
	}

	public void setSubmittedAt(Instant submittedAt) {
		this.submittedAt = submittedAt;
	}

	public Long getApproverId() {
		return approverId;
	}

	public void setApproverId(Long approverId) {
		this.approverId = approverId;
	}

	public String getDecision() {
		return decision;
	}

	public void setDecision(String decision) {
		this.decision = decision;
	}

	public String getDecisionReason() {
		return decisionReason;
	}

	public void setDecisionReason(String decisionReason) {
		this.decisionReason = decisionReason;
	}

	public Instant getDecidedAt() {
		return decidedAt;
	}

	public void setDecidedAt(Instant decidedAt) {
		this.decidedAt = decidedAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

    // getters/setters
}