package com.example.MiniProject.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "cheque_requests")
public class ChequeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_id", unique = true, nullable = false)
    private String receiptId;

    @Column(name = "account_no", nullable = false)
    private String accountNo;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "cheque_number", nullable = false)
    private String chequeNumber;

    @Column(name = "drawer_bank", nullable = false)
    private String drawerBank;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "cheque_date", nullable = false)
    private LocalDate chequeDate;

    @Column(name = "status", nullable = false)
    private String status; // IN_CLEARING, CLEARED, BOUNCED

    @Column(name = "teller_id")
    private Long tellerId;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        if (status == null) status = "IN_CLEARING";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReceiptId() { return receiptId; }
    public void setReceiptId(String receiptId) { this.receiptId = receiptId; }
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getChequeNumber() { return chequeNumber; }
    public void setChequeNumber(String chequeNumber) { this.chequeNumber = chequeNumber; }
    public String getDrawerBank() { return drawerBank; }
    public void setDrawerBank(String drawerBank) { this.drawerBank = drawerBank; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getChequeDate() { return chequeDate; }
    public void setChequeDate(LocalDate chequeDate) { this.chequeDate = chequeDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getTellerId() { return tellerId; }
    public void setTellerId(Long tellerId) { this.tellerId = tellerId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
