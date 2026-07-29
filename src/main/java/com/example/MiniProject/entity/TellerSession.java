package com.example.MiniProject.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "teller_sessions")
public class TellerSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teller_id", nullable = false)
    private Long tellerId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "total_deposits", precision = 18, scale = 2)
    private BigDecimal totalDeposits = BigDecimal.ZERO;

    @Column(name = "total_withdrawals", precision = 18, scale = 2)
    private BigDecimal totalWithdrawals = BigDecimal.ZERO;

    @Column(name = "total_cheques", precision = 18, scale = 2)
    private BigDecimal totalCheques = BigDecimal.ZERO;

    @Column(name = "deposit_count")
    private Integer depositCount = 0;

    @Column(name = "withdrawal_count")
    private Integer withdrawalCount = 0;

    @Column(name = "cheque_count")
    private Integer chequeCount = 0;

    @Column(name = "status", nullable = false)
    private String status; // OPEN, SUBMITTED, RECONCILIATION_PENDING

    @Column(name = "mismatch_reason", length = 500)
    private String mismatchReason;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        if (status == null) status = "OPEN";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTellerId() { return tellerId; }
    public void setTellerId(Long tellerId) { this.tellerId = tellerId; }
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public BigDecimal getTotalDeposits() { return totalDeposits; }
    public void setTotalDeposits(BigDecimal totalDeposits) { this.totalDeposits = totalDeposits; }
    public BigDecimal getTotalWithdrawals() { return totalWithdrawals; }
    public void setTotalWithdrawals(BigDecimal totalWithdrawals) { this.totalWithdrawals = totalWithdrawals; }
    public BigDecimal getTotalCheques() { return totalCheques; }
    public void setTotalCheques(BigDecimal totalCheques) { this.totalCheques = totalCheques; }
    public Integer getDepositCount() { return depositCount; }
    public void setDepositCount(Integer depositCount) { this.depositCount = depositCount; }
    public Integer getWithdrawalCount() { return withdrawalCount; }
    public void setWithdrawalCount(Integer withdrawalCount) { this.withdrawalCount = withdrawalCount; }
    public Integer getChequeCount() { return chequeCount; }
    public void setChequeCount(Integer chequeCount) { this.chequeCount = chequeCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMismatchReason() { return mismatchReason; }
    public void setMismatchReason(String mismatchReason) { this.mismatchReason = mismatchReason; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
