package com.example.MiniProject.service;

import com.example.MiniProject.entity.Account;
import com.example.MiniProject.entity.AccountLimit;
import com.example.MiniProject.entity.CustomerLimit;
import com.example.MiniProject.entity.FeeQuote;
import com.example.MiniProject.entity.InternationalTransaction;
import com.example.MiniProject.repository.CustomerRepository;
import com.example.MiniProject.repository.AccountRepository;
import com.example.MiniProject.repository.AccountLimitRepository;
import com.example.MiniProject.repository.CustomerLimitRepository;
import com.example.MiniProject.repository.InternationalTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InternationalTransferService {

    private final InternationalTransactionRepository txnRepo;
    private final AccountRepository accountRepo;
    private final CustomerLimitRepository limitRepo;
    private final AccountLimitRepository accountLimitRepo;
    private final FeeEngineService feeEngine;
    private final AmlRiskService amlRisk;
    private final OtpService otpService;
    private final AuditService auditService;
    private final CustomerRepository customerRepo;

    public InternationalTransferService(InternationalTransactionRepository txnRepo,
                                        AccountRepository accountRepo,
                                        CustomerLimitRepository limitRepo,
                                        AccountLimitRepository accountLimitRepo,
                                        FeeEngineService feeEngine,
                                        AmlRiskService amlRisk,
                                        OtpService otpService,
                                        AuditService auditService,
                                        CustomerRepository customerRepo) {
        this.txnRepo          = txnRepo;
        this.accountRepo      = accountRepo;
        this.limitRepo        = limitRepo;
        this.accountLimitRepo = accountLimitRepo;
        this.feeEngine        = feeEngine;
        this.amlRisk          = amlRisk;
        this.otpService       = otpService;
        this.auditService     = auditService;
        this.customerRepo     = customerRepo;
    }

    /**
     * US024 — Initiate: validate quote, send OTP.
     * Called when customer clicks "Confirm & Send" on the quote screen.
     */
    public InternationalTransaction initiate(Long customerId, Map<String, Object> body) {
        String quoteRef        = (String) body.get("quoteRef");
        String fromAccountNo   = (String) body.get("fromAccountNo");
        String recipientName   = (String) body.get("recipientName");
        String recipientAccNo  = (String) body.get("recipientAccountNo");
        String recipientSwift  = (String) body.get("recipientSwift");
        String recipientBank   = (String) body.getOrDefault("recipientBank", "");
        String purpose         = (String) body.getOrDefault("purpose", "");

        // Validate and mark quote as USED
        FeeQuote quote = feeEngine.useQuote(quoteRef);

        // Build transaction record (US043 — log INITIATED)
        InternationalTransaction txn = new InternationalTransaction();
        txn.setReferenceId("INTL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        txn.setCustomerId(customerId);
        txn.setFromAccountNo(fromAccountNo);
        txn.setFromCurrency(quote.getFromCurrency());
        txn.setToCurrency(quote.getToCurrency());
        txn.setSendAmount(quote.getSendAmount());
        txn.setTotalDebitAmount(quote.getTotalDebitAmount());
        txn.setReceiverGets(quote.getReceiverGets());
        txn.setQuoteRef(quoteRef);
        txn.setRecipientName(recipientName);
        txn.setRecipientAccountNo(recipientAccNo);
        txn.setRecipientSwift(recipientSwift);
        txn.setRecipientBank(recipientBank);
        txn.setTransferPurpose(purpose);
        txn.setStatus("INITIATED");
        txnRepo.save(txn);

        // Send OTP
        var otp = otpService.createOtp(customerId, "INTL_TRANSFER");
        System.out.println("INTL_TRANSFER OTP for customerId=" + customerId + " → " + otp.getOtpCode());

        // Return OTP in response for dev/demo mode
        txn.setOtpPreview(otp.getOtpCode());
        return txn;
    }

    /**
     * US024 — Confirm with OTP → US032 balance → US033 limits → US034 AML → US036 routing.
     */
    public InternationalTransaction confirm(Long customerId, String referenceId, String otpCode) {
        InternationalTransaction txn = getOwned(customerId, referenceId);

        if (!"INITIATED".equals(txn.getStatus()))
            throw new RuntimeException("Transaction already processed.");

        // Verify OTP
        boolean otpOk = otpService.verifyOtp(customerId, "INTL_TRANSFER", otpCode);
        if (!otpOk) {
            fail(txn, "VALIDATION", "Invalid OTP");
            throw new RuntimeException("Invalid OTP. Try again.");
        }

        Account account = accountRepo.findByAccountNo(txn.getFromAccountNo())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!"ACTIVE".equals(account.getStatus())) {
            fail(txn, "VALIDATION", "Account frozen. Contact branch.");
            throw new RuntimeException("Account frozen. Contact branch.");
        }

        // US032 — Balance check (total debit including fees)
        if (account.getAvailableBalance().compareTo(txn.getTotalDebitAmount()) < 0) {
            fail(txn, "VALIDATION", "Insufficient balance");
            throw new RuntimeException("Insufficient balance");
        }

        // US033 — Per-transaction and daily limit check
        // AccountLimit (set by Admin per account) takes priority over CustomerLimit
        Account acctForLimit = accountRepo.findByAccountNo(txn.getFromAccountNo())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        AccountLimit acctLimit = accountLimitRepo
                .findTopByAccountIdOrderByEffectiveFromDesc(acctForLimit.getId()).orElse(null);
        CustomerLimit custLimit = limitRepo.findTopByCustomerIdOrderByEffectiveFromDesc(customerId).orElse(null);

        BigDecimal perTxnLimit  = acctLimit != null ? acctLimit.getPerTxnLimit()
                : (custLimit != null && custLimit.getPerTxnLimit()  != null ? custLimit.getPerTxnLimit()  : new BigDecimal("100000"));
        BigDecimal dailyLimit   = acctLimit != null ? acctLimit.getDailyLimit()
                : (custLimit != null && custLimit.getDailyLimit()   != null ? custLimit.getDailyLimit()   : new BigDecimal("200000"));
        BigDecimal monthlyLimit = acctLimit != null ? acctLimit.getMonthlyLimit()
                : (custLimit != null && custLimit.getMonthlyLimit() != null ? custLimit.getMonthlyLimit() : new BigDecimal("1000000"));

        if (txn.getSendAmount().compareTo(perTxnLimit) > 0) {
            // Score AML before failing — limit breach on large amounts is suspicious
            amlRisk.score(txn);
            String flagReason = "Per-transaction limit of ₹" + perTxnLimit + " exceeded";
            if ("HIGH".equals(txn.getAmlRiskLevel())) {
                txn.setAdminFlagReason(flagReason + " | AML: " + txn.getAmlFlags());
                txn.setAdminReviewed(false);
            }
            fail(txn, "VALIDATION", flagReason);
            throw new RuntimeException("Per-transaction limit of ₹" + perTxnLimit + " exceeded.");
        }

        // Count in-flight + completed transactions
        List<String> countedStatuses = List.of("INITIATED", "DEBITED", "PROCESSING", "SETTLED", "COMPLETED");

        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        List<InternationalTransaction> todayTxns = txnRepo
                .findByFromAccountNoAndStatusInAndCreatedAtAfter(
                        txn.getFromAccountNo(), countedStatuses, startOfDay);
        BigDecimal dailyUsed = todayTxns.stream()
                .map(InternationalTransaction::getSendAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (dailyUsed.add(txn.getSendAmount()).compareTo(dailyLimit) > 0) {
            amlRisk.score(txn);
            String flagReason = "Daily limit of ₹" + dailyLimit + " exceeded";
            if ("HIGH".equals(txn.getAmlRiskLevel())) {
                txn.setAdminFlagReason(flagReason + " | AML: " + txn.getAmlFlags());
                txn.setAdminReviewed(false);
            }
            fail(txn, "VALIDATION", flagReason);
            throw new RuntimeException("Daily transaction limit of ₹" + dailyLimit + " exceeded.");
        }

        Instant startOfMonth = Instant.now().atZone(ZoneOffset.UTC)
                .withDayOfMonth(1).toInstant().truncatedTo(ChronoUnit.DAYS);
        List<InternationalTransaction> monthTxns = txnRepo
                .findByFromAccountNoAndStatusInAndCreatedAtAfter(
                        txn.getFromAccountNo(), countedStatuses, startOfMonth);
        BigDecimal monthlyUsed = monthTxns.stream()
                .map(InternationalTransaction::getSendAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (monthlyUsed.add(txn.getSendAmount()).compareTo(monthlyLimit) > 0) {
            amlRisk.score(txn);
            String flagReason = "Monthly limit of ₹" + monthlyLimit + " exceeded";
            if ("HIGH".equals(txn.getAmlRiskLevel())) {
                txn.setAdminFlagReason(flagReason + " | AML: " + txn.getAmlFlags());
                txn.setAdminReviewed(false);
            }
            fail(txn, "VALIDATION", flagReason);
            throw new RuntimeException("Monthly transaction limit of ₹" + monthlyLimit + " exceeded.");
        }

        // US034 — AML risk scoring
        amlRisk.score(txn);

        // US036 — Route HIGH risk to compliance queue
        if ("HIGH".equals(txn.getAmlRiskLevel())) {
            txn.setStatus("PENDING_COMPLIANCE");
            txnRepo.save(txn);
            auditService.log(customerId, "CUSTOMER", "TXN_ROUTED_COMPLIANCE",
                    "TRANSACTION", txn.getReferenceId(), "AML HIGH risk — routed to compliance");
            return txn;
        }

        // Low/Medium — proceed to debit immediately
        txn.setStatus("VALIDATED");
        txnRepo.save(txn);
        return debit(txn, account);
    }

    /**
     * US038 — Debit sender account.
     */
    public InternationalTransaction debit(InternationalTransaction txn, Account account) {
        if (account == null)
            account = accountRepo.findByAccountNo(txn.getFromAccountNo())
                    .orElseThrow(() -> new RuntimeException("Account not found"));

        // Re-check balance
        if (account.getAvailableBalance().compareTo(txn.getTotalDebitAmount()) < 0) {
            fail(txn, "DEBIT", "Insufficient balance at time of debit");
            throw new RuntimeException("Insufficient balance");
        }

        account.setAvailableBalance(account.getAvailableBalance().subtract(txn.getTotalDebitAmount()));
        account.setLedgerBalance(account.getLedgerBalance().subtract(txn.getTotalDebitAmount()));
        accountRepo.save(account);

        txn.setStatus("DEBITED");
        txn.setDebitedAt(Instant.now());
        txnRepo.save(txn);

        // Simulate processing → settled → completed (in real system these are async)
        return complete(txn);
    }

    /**
     * US041 — Simulate settlement and credit confirmation → COMPLETED.
     */
    private InternationalTransaction complete(InternationalTransaction txn) {
        txn.setStatus("PROCESSING");
        txnRepo.save(txn);

        txn.setStatus("SETTLED");
        txnRepo.save(txn);

        txn.setStatus("COMPLETED");
        txn.setCompletedAt(Instant.now());
        InternationalTransaction completed = txnRepo.save(txn);
        auditService.log(null, "SYSTEM", "INTL_TRANSFER_COMPLETED",
                "TRANSACTION", completed.getReferenceId(), "International transfer completed");
        return completed;
    }

    // ── Compliance actions (US037) ──────────────────────────────────────────

    /** Compliance approves a PENDING_COMPLIANCE transaction → debit and complete. */
    public InternationalTransaction complianceApprove(String referenceId, Long officerId) {
        InternationalTransaction txn = txnRepo.findByReferenceId(referenceId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!"PENDING_COMPLIANCE".equals(txn.getStatus()) && !"PENDING_COMPLIANCE_ESCALATED".equals(txn.getStatus()))
            throw new RuntimeException("Transaction is not pending compliance.");

        txn.setStatus("APPROVED");
        txn.setComplianceActionBy(officerId);
        txn.setComplianceActionAt(Instant.now());
        txnRepo.save(txn);
        auditService.log(officerId, "COMPLIANCE_OPS", "TXN_COMPLIANCE_APPROVED",
                "TRANSACTION", referenceId, "Compliance approved transfer");
        return debit(txn, null);
    }

    /** Compliance rejects → BLOCKED + auto-flag to admin if AML score is HIGH. */
    public InternationalTransaction complianceReject(String referenceId, Long officerId, String reason) {
        if (reason == null || reason.isBlank())
            throw new RuntimeException("Rejection reason is mandatory.");

        InternationalTransaction txn = txnRepo.findByReferenceId(referenceId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!"PENDING_COMPLIANCE".equals(txn.getStatus()) && !"PENDING_COMPLIANCE_ESCALATED".equals(txn.getStatus()))
            throw new RuntimeException("Transaction is not pending compliance.");

        txn.setStatus("BLOCKED");
        txn.setComplianceActionBy(officerId);
        txn.setComplianceActionAt(Instant.now());
        txn.setComplianceReason(reason);

        // Auto-flag to admin if AML risk is HIGH
        if ("HIGH".equals(txn.getAmlRiskLevel())) {
            txn.setAdminFlagReason("Compliance rejected (HIGH AML): " + reason
                    + " | Flags: " + txn.getAmlFlags());
            txn.setAdminReviewed(false);
        }

        InternationalTransaction blocked = txnRepo.save(txn);
        auditService.log(officerId, "COMPLIANCE_OPS", "TXN_COMPLIANCE_REJECTED",
                "TRANSACTION", referenceId, "Compliance rejected: " + reason);
        return blocked;
    }

    // ── Queries ─────────────────────────────────────────────────────────────

    public List<InternationalTransaction> getCustomerHistory(Long customerId,
                                                              String status,
                                                              Instant from, Instant to) {
        return txnRepo.findFiltered(customerId, status, from, to);
    }

    public List<InternationalTransaction> getComplianceQueue() {
        List<InternationalTransaction> list = txnRepo.findByStatusOrderByCreatedAtDesc("PENDING_COMPLIANCE");
        list.forEach(t -> customerRepo.findById(t.getCustomerId())
                .ifPresent(c -> t.setCustomerName(c.getFullName())));
        return list;
    }

    public List<InternationalTransaction> getEscalatedQueue() {
        List<InternationalTransaction> list = txnRepo.findByStatusOrderByCreatedAtDesc("PENDING_COMPLIANCE_ESCALATED");
        list.forEach(t -> customerRepo.findById(t.getCustomerId())
                .ifPresent(c -> t.setCustomerName(c.getFullName())));
        return list;
    }

    public InternationalTransaction getByRef(String referenceId) {
        return txnRepo.findByReferenceId(referenceId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private InternationalTransaction getOwned(Long customerId, String referenceId) {
        InternationalTransaction txn = txnRepo.findByReferenceId(referenceId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        if (!txn.getCustomerId().equals(customerId))
            throw new RuntimeException("Unauthorized");
        return txn;
    }

    private void fail(InternationalTransaction txn, String stage, String reason) {
        txn.setStatus("FAILED");
        txn.setFailureStage(stage);
        txn.setFailureReason(reason);
        txnRepo.save(txn);
    }
}
