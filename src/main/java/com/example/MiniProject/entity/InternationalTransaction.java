package com.example.MiniProject.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "international_transactions")
public class InternationalTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_id", unique = true, nullable = false)
    private String referenceId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "from_account_no", nullable = false)
    private String fromAccountNo;

    @Column(name = "from_currency", length = 10)
    private String fromCurrency;

    @Column(name = "to_currency", length = 10)
    private String toCurrency;

    @Column(name = "send_amount", precision = 18, scale = 2)
    private BigDecimal sendAmount;

    @Column(name = "total_debit_amount", precision = 18, scale = 2)
    private BigDecimal totalDebitAmount;

    @Column(name = "receiver_gets", precision = 18, scale = 2)
    private BigDecimal receiverGets;

    @Column(name = "quote_ref")
    private String quoteRef;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "recipient_account_no")
    private String recipientAccountNo;

    @Column(name = "recipient_swift")
    private String recipientSwift;

    @Column(name = "recipient_bank")
    private String recipientBank;

    @Column(name = "transfer_purpose")
    private String transferPurpose;

    // INITIATED → PENDING_COMPLIANCE | VALIDATED → APPROVED → DEBITED → PROCESSING → SETTLED → COMPLETED
    // FAILED | BLOCKED
    @Column(nullable = false)
    private String status;

    @Column(name = "aml_risk_level", length = 10)
    private String amlRiskLevel; // LOW | MEDIUM | HIGH

    @Column(name = "aml_risk_score")
    private Integer amlRiskScore;

    @Column(name = "aml_flags")
    private String amlFlags; // comma-separated flag reasons

    @Column(name = "compliance_action_by")
    private Long complianceActionBy;

    @Column(name = "compliance_action_at")
    private Instant complianceActionAt;

    @Column(name = "compliance_reason")
    private String complianceReason;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "failure_stage")
    private String failureStage;

    @Column(name = "admin_flag_reason")
    private String adminFlagReason; // set when auto-escalated to admin for review

    @Column(name = "admin_reviewed")
    private Boolean adminReviewed = false;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "debited_at")
    private Instant debitedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        if (status == null) status = "INITIATED";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String r) { this.referenceId = r; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long c) { this.customerId = c; }
    public String getFromAccountNo() { return fromAccountNo; }
    public void setFromAccountNo(String f) { this.fromAccountNo = f; }
    public String getFromCurrency() { return fromCurrency; }
    public void setFromCurrency(String f) { this.fromCurrency = f; }
    public String getToCurrency() { return toCurrency; }
    public void setToCurrency(String t) { this.toCurrency = t; }
    public BigDecimal getSendAmount() { return sendAmount; }
    public void setSendAmount(BigDecimal s) { this.sendAmount = s; }
    public BigDecimal getTotalDebitAmount() { return totalDebitAmount; }
    public void setTotalDebitAmount(BigDecimal t) { this.totalDebitAmount = t; }
    public BigDecimal getReceiverGets() { return receiverGets; }
    public void setReceiverGets(BigDecimal r) { this.receiverGets = r; }
    public String getQuoteRef() { return quoteRef; }
    public void setQuoteRef(String q) { this.quoteRef = q; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String r) { this.recipientName = r; }
    public String getRecipientAccountNo() { return recipientAccountNo; }
    public void setRecipientAccountNo(String r) { this.recipientAccountNo = r; }
    public String getRecipientSwift() { return recipientSwift; }
    public void setRecipientSwift(String r) { this.recipientSwift = r; }
    public String getRecipientBank() { return recipientBank; }
    public void setRecipientBank(String r) { this.recipientBank = r; }
    public String getTransferPurpose() { return transferPurpose; }
    public void setTransferPurpose(String t) { this.transferPurpose = t; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getAmlRiskLevel() { return amlRiskLevel; }
    public void setAmlRiskLevel(String a) { this.amlRiskLevel = a; }
    public Integer getAmlRiskScore() { return amlRiskScore; }
    public void setAmlRiskScore(Integer a) { this.amlRiskScore = a; }
    public String getAmlFlags() { return amlFlags; }
    public void setAmlFlags(String a) { this.amlFlags = a; }
    public Long getComplianceActionBy() { return complianceActionBy; }
    public void setComplianceActionBy(Long c) { this.complianceActionBy = c; }
    public Instant getComplianceActionAt() { return complianceActionAt; }
    public void setComplianceActionAt(Instant c) { this.complianceActionAt = c; }
    public String getComplianceReason() { return complianceReason; }
    public void setComplianceReason(String c) { this.complianceReason = c; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String f) { this.failureReason = f; }
    public String getFailureStage() { return failureStage; }
    public void setFailureStage(String f) { this.failureStage = f; }
    public String getAdminFlagReason() { return adminFlagReason; }
    public void setAdminFlagReason(String a) { this.adminFlagReason = a; }
    public Boolean getAdminReviewed() { return adminReviewed; }
    public void setAdminReviewed(Boolean a) { this.adminReviewed = a; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant c) { this.createdAt = c; }
    public Instant getDebitedAt() { return debitedAt; }
    public void setDebitedAt(Instant d) { this.debitedAt = d; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant c) { this.completedAt = c; }

    // Transient — only populated on initiate response, never persisted
    @Transient
    private String otpPreview;
    public String getOtpPreview() { return otpPreview; }
    public void setOtpPreview(String o) { this.otpPreview = o; }

    // Transient — populated for compliance queue display
    @Transient
    private String customerName;
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String n) { this.customerName = n; }
}
