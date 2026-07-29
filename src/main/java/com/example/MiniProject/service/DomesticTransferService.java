package com.example.MiniProject.service;

import com.example.MiniProject.dto.DomesticTransferRequest;
import com.example.MiniProject.entity.Account;
import com.example.MiniProject.entity.AccountLimit;
import com.example.MiniProject.entity.CustomerLimit;
import com.example.MiniProject.entity.DomesticTransaction;
import com.example.MiniProject.repository.AccountRepository;
import com.example.MiniProject.repository.AccountLimitRepository;
import com.example.MiniProject.repository.CustomerLimitRepository;
import com.example.MiniProject.repository.DomesticTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class DomesticTransferService {

    private final AccountRepository accountRepo;
    private final DomesticTransactionRepository txnRepo;
    private final OtpService otpService;
    private final CustomerLimitRepository limitRepo;
    private final AccountLimitRepository accountLimitRepo;
    private final AuditService auditService;

    public DomesticTransferService(AccountRepository accountRepo,
                                   DomesticTransactionRepository txnRepo,
                                   OtpService otpService,
                                   CustomerLimitRepository limitRepo,
                                   AccountLimitRepository accountLimitRepo,
                                   AuditService auditService) {
        this.accountRepo      = accountRepo;
        this.txnRepo          = txnRepo;
        this.otpService       = otpService;
        this.limitRepo        = limitRepo;
        this.accountLimitRepo = accountLimitRepo;
        this.auditService     = auditService;
    }

    /** Step 1: Validate and initiate — returns referenceId */
    public DomesticTransaction initiate(Long customerId, DomesticTransferRequest req) {

        Account from = accountRepo.findByAccountNo(req.getFromAccountNo())
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        if (!"ACTIVE".equals(from.getStatus()))
            throw new RuntimeException("Account is not available for transactions.");

        if (!from.getCustomerId().equals(customerId))
            throw new RuntimeException("Account does not belong to this customer");

        // Validate destination exists
        accountRepo.findByAccountNo(req.getToAccountNo())
                .orElseThrow(() -> new RuntimeException("Destination account not found"));

        // Balance check
        if (from.getAvailableBalance().compareTo(req.getAmount()) < 0)
            throw new RuntimeException("Insufficient balance");

        // Per-transaction limit check — AccountLimit (Admin-set) takes priority over CustomerLimit
        AccountLimit acctLimit = accountLimitRepo
                .findTopByAccountIdOrderByEffectiveFromDesc(from.getId()).orElse(null);
        CustomerLimit custLimit = limitRepo.findTopByCustomerIdOrderByEffectiveFromDesc(customerId).orElse(null);

        BigDecimal perTxnLimit  = acctLimit != null ? acctLimit.getPerTxnLimit()
                : (custLimit != null && custLimit.getPerTxnLimit()  != null ? custLimit.getPerTxnLimit()  : new BigDecimal("100000"));
        BigDecimal dailyLimit   = acctLimit != null ? acctLimit.getDailyLimit()
                : (custLimit != null && custLimit.getDailyLimit()   != null ? custLimit.getDailyLimit()   : new BigDecimal("200000"));
        BigDecimal monthlyLimit = acctLimit != null ? acctLimit.getMonthlyLimit()
                : (custLimit != null && custLimit.getMonthlyLimit() != null ? custLimit.getMonthlyLimit() : new BigDecimal("1000000"));

        if (req.getAmount().compareTo(perTxnLimit) > 0)
            throw new RuntimeException("Per-transaction limit of ₹" + perTxnLimit + " exceeded.");

        // Count in-flight + completed transactions (consistent with international transfer)
        List<String> countedStatuses = List.of("INITIATED", "COMPLETED");

        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        List<DomesticTransaction> todayTxns = txnRepo
                .findByFromAccountNoAndStatusInAndCreatedAtAfter(req.getFromAccountNo(), countedStatuses, startOfDay);
        BigDecimal dailyUsed = todayTxns.stream()
                .map(DomesticTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (dailyUsed.add(req.getAmount()).compareTo(dailyLimit) > 0)
            throw new RuntimeException("Daily transaction limit of ₹" + dailyLimit + " exceeded.");

        Instant startOfMonth = Instant.now().atZone(java.time.ZoneOffset.UTC)
                .withDayOfMonth(1).toInstant().truncatedTo(ChronoUnit.DAYS);
        List<DomesticTransaction> monthTxns = txnRepo
                .findByFromAccountNoAndStatusInAndCreatedAtAfter(req.getFromAccountNo(), countedStatuses, startOfMonth);
        BigDecimal monthlyUsed = monthTxns.stream()
                .map(DomesticTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (monthlyUsed.add(req.getAmount()).compareTo(monthlyLimit) > 0)
            throw new RuntimeException("Monthly transaction limit of ₹" + monthlyLimit + " exceeded.");

        // Create INITIATED transaction log
        DomesticTransaction txn = new DomesticTransaction();
        txn.setReferenceId("DOM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        txn.setCustomerId(customerId);
        txn.setFromAccountNo(req.getFromAccountNo());
        txn.setToAccountNo(req.getToAccountNo());
        txn.setBeneficiaryName(req.getBeneficiaryName());
        txn.setAmount(req.getAmount());
        txn.setRemarks(req.getRemarks());
        txn.setStatus("INITIATED");
        DomesticTransaction saved = txnRepo.save(txn);

        // Send OTP
        var otp = otpService.createOtp(customerId, "DOMESTIC_TRANSFER");
        System.out.println("DOMESTIC_TRANSFER OTP for customerId " + customerId + " = " + otp.getOtpCode());

        // Return OTP in response for dev/demo mode (frontend will display it on screen)
        saved.setOtpPreview(otp.getOtpCode());
        return saved;
    }

    /** Step 2: Verify OTP and execute debit */
    public DomesticTransaction confirm(Long customerId, String referenceId, String otpCode) {

        DomesticTransaction txn = txnRepo.findByReferenceId(referenceId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!txn.getCustomerId().equals(customerId))
            throw new RuntimeException("Unauthorized");

        if (!"INITIATED".equals(txn.getStatus()))
            throw new RuntimeException("Transaction already processed");

        // Verify OTP
        boolean ok = otpService.verifyOtp(customerId, "DOMESTIC_TRANSFER", otpCode);
        if (!ok) {
            txn.setStatus("FAILED");
            txn.setFailureReason("Invalid OTP");
            txnRepo.save(txn);
            throw new RuntimeException("Invalid OTP. Try again.");
        }

        // Re-check balance before debit
        Account from = accountRepo.findByAccountNo(txn.getFromAccountNo())
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        if (from.getAvailableBalance().compareTo(txn.getAmount()) < 0) {
            txn.setStatus("FAILED");
            txn.setFailureReason("Insufficient balance at time of debit");
            txnRepo.save(txn);
            throw new RuntimeException("Insufficient balance");
        }

        // Debit sender
        from.setAvailableBalance(from.getAvailableBalance().subtract(txn.getAmount()));
        from.setLedgerBalance(from.getLedgerBalance().subtract(txn.getAmount()));
        accountRepo.save(from);

        // Credit receiver
        accountRepo.findByAccountNo(txn.getToAccountNo()).ifPresent(to -> {
            to.setAvailableBalance(to.getAvailableBalance().add(txn.getAmount()));
            to.setLedgerBalance(to.getLedgerBalance().add(txn.getAmount()));
            accountRepo.save(to);
        });

        // Mark completed
        txn.setStatus("COMPLETED");
        txn.setCompletedAt(Instant.now());
        DomesticTransaction completed = txnRepo.save(txn);
        auditService.log(customerId, "CUSTOMER", "DOMESTIC_TRANSFER_COMPLETED",
                "TRANSACTION", txn.getReferenceId(),
                "Domestic transfer completed: " + txn.getAmount() + " from " + txn.getFromAccountNo());
        return completed;
    }

    /** Get transaction history for customer */
    public List<DomesticTransaction> getHistory(Long customerId) {
        return txnRepo.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    /** Get filtered transaction history */
    public List<DomesticTransaction> getFilteredHistory(Long customerId, String status, Instant from, Instant to) {
        return txnRepo.findFiltered(customerId, status, from, to);
    }
}
