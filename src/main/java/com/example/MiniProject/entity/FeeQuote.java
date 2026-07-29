package com.example.MiniProject.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fee_quotes")
public class FeeQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quote_ref", nullable = false, unique = true)
    private String quoteRef; // e.g. QT-20240101-000001

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "policy_version")
    private Integer policyVersion;

    @Column(name = "fx_rate_id")
    private Long fxRateId;

    @Column(name = "from_currency", length = 10)
    private String fromCurrency;

    @Column(name = "to_currency", length = 10)
    private String toCurrency;

    @Column(name = "send_amount", precision = 18, scale = 2)
    private BigDecimal sendAmount;

    @Column(name = "fx_rate", precision = 18, scale = 6)
    private BigDecimal fxRate;

    @Column(name = "fx_markup_percent", precision = 8, scale = 4)
    private BigDecimal fxMarkupPercent;

    @Column(name = "fx_markup_amount", precision = 18, scale = 2)
    private BigDecimal fxMarkupAmount;

    @Column(name = "base_fee", precision = 18, scale = 2)
    private BigDecimal baseFee;

    @Column(name = "intermediary_fee", precision = 18, scale = 2)
    private BigDecimal intermediaryFee;

    @Column(name = "handling_fee", precision = 18, scale = 2)
    private BigDecimal handlingFee;

    @Column(name = "tax_amount", precision = 18, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_fees", precision = 18, scale = 2)
    private BigDecimal totalFees;

    @Column(name = "total_debit_amount", precision = 18, scale = 2)
    private BigDecimal totalDebitAmount;

    @Column(name = "receiver_gets", precision = 18, scale = 2)
    private BigDecimal receiverGets;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private String status; // ACTIVE | EXPIRED | USED

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        if (status == null) status = "ACTIVE";
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQuoteRef() { return quoteRef; }
    public void setQuoteRef(String quoteRef) { this.quoteRef = quoteRef; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }

    public Integer getPolicyVersion() { return policyVersion; }
    public void setPolicyVersion(Integer policyVersion) { this.policyVersion = policyVersion; }

    public Long getFxRateId() { return fxRateId; }
    public void setFxRateId(Long fxRateId) { this.fxRateId = fxRateId; }

    public String getFromCurrency() { return fromCurrency; }
    public void setFromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; }

    public String getToCurrency() { return toCurrency; }
    public void setToCurrency(String toCurrency) { this.toCurrency = toCurrency; }

    public BigDecimal getSendAmount() { return sendAmount; }
    public void setSendAmount(BigDecimal sendAmount) { this.sendAmount = sendAmount; }

    public BigDecimal getFxRate() { return fxRate; }
    public void setFxRate(BigDecimal fxRate) { this.fxRate = fxRate; }

    public BigDecimal getFxMarkupPercent() { return fxMarkupPercent; }
    public void setFxMarkupPercent(BigDecimal fxMarkupPercent) { this.fxMarkupPercent = fxMarkupPercent; }

    public BigDecimal getFxMarkupAmount() { return fxMarkupAmount; }
    public void setFxMarkupAmount(BigDecimal fxMarkupAmount) { this.fxMarkupAmount = fxMarkupAmount; }

    public BigDecimal getBaseFee() { return baseFee; }
    public void setBaseFee(BigDecimal baseFee) { this.baseFee = baseFee; }

    public BigDecimal getIntermediaryFee() { return intermediaryFee; }
    public void setIntermediaryFee(BigDecimal intermediaryFee) { this.intermediaryFee = intermediaryFee; }

    public BigDecimal getHandlingFee() { return handlingFee; }
    public void setHandlingFee(BigDecimal handlingFee) { this.handlingFee = handlingFee; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getTotalFees() { return totalFees; }
    public void setTotalFees(BigDecimal totalFees) { this.totalFees = totalFees; }

    public BigDecimal getTotalDebitAmount() { return totalDebitAmount; }
    public void setTotalDebitAmount(BigDecimal totalDebitAmount) { this.totalDebitAmount = totalDebitAmount; }

    public BigDecimal getReceiverGets() { return receiverGets; }
    public void setReceiverGets(BigDecimal receiverGets) { this.receiverGets = receiverGets; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
