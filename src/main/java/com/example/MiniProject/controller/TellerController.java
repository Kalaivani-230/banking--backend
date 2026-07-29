package com.example.MiniProject.controller;

import com.example.MiniProject.entity.*;
import com.example.MiniProject.repository.*;
import com.example.MiniProject.service.AuditService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/teller")
public class TellerController {

    private final AccountRepository accountRepo;
    private final CustomerRepository customerRepo;
    private final ChequeRequestRepository chequeRepo;
    private final TellerSessionRepository sessionRepo;
    private final AuditService auditService;
    private final KycRequestRepository kycRepo;

    public TellerController(AccountRepository accountRepo,
                            CustomerRepository customerRepo,
                            ChequeRequestRepository chequeRepo,
                            TellerSessionRepository sessionRepo,
                            AuditService auditService,
                            KycRequestRepository kycRepo) {
        this.accountRepo = accountRepo;
        this.customerRepo = customerRepo;
        this.chequeRepo = chequeRepo;
        this.sessionRepo = sessionRepo;
        this.auditService = auditService;
        this.kycRepo = kycRepo;
    }

    // ── US069 — Customer Lookup & Verification ─────────────────────────────

    @GetMapping("/search/{accountNo}")
    public Map<String, Object> searchAccount(@PathVariable String accountNo) {
        Account account = accountRepo.findByAccountNo(accountNo)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNo));
        Customer customer = customerRepo.findById(account.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Map<String, Object> result = new HashMap<>();
        result.put("account", account);
        result.put("customerName", customer.getFullName());
        result.put("customerEmail", customer.getEmail());
        result.put("customerMobile", customer.getMobile());
        result.put("customerId", customer.getId());
        return result;
    }

    @PostMapping("/verify-customer")
    public Map<String, Object> verifyCustomer(@RequestBody Map<String, String> body) {
        String accountNo = body.get("accountNo");
        String idProofType = body.get("idProofType");
        String idProofNumber = body.get("idProofNumber");
        String verificationMethod = body.getOrDefault("verificationMethod", "ID_PROOF");
        String signatureMatch = body.getOrDefault("signatureMatch", "false");

        if (accountNo == null || accountNo.isBlank())
            throw new RuntimeException("Account number is required");

        Account account = accountRepo.findByAccountNo(accountNo)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        Customer customer = customerRepo.findById(account.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        boolean verified = false;
        String result = "FAILED";

        if ("SIGNATURE".equalsIgnoreCase(verificationMethod)) {
            verified = "true".equalsIgnoreCase(signatureMatch);
        } else {
            // BUG-028 & BUG-030: Cross-check ID against customer's verified KYC document
            if (idProofType == null || idProofType.isBlank() || idProofNumber == null || idProofNumber.isBlank())
                throw new RuntimeException("ID type and number are required");

            KycRequest kyc = kycRepo.findTopByCustomerIdOrderByCreatedAtDesc(account.getCustomerId()).orElse(null);
            if (kyc == null)
                throw new RuntimeException("No KYC record found for this customer");
            if (!"VERIFIED".equals(kyc.getStatus()))
                throw new RuntimeException("Customer KYC is not verified yet");

            String enteredNumber = idProofNumber.trim().toUpperCase();
            switch (idProofType.toUpperCase()) {
                case "PAN":
                    verified = kyc.getPan() != null && kyc.getPan().equalsIgnoreCase(enteredNumber);
                    break;
                case "AADHAAR":
                    verified = kyc.getAadhaar() != null && kyc.getAadhaar().equals(idProofNumber.trim());
                    break;
                default:
                    throw new RuntimeException("Invalid ID type. Allowed: PAN, AADHAAR");
            }
            if (!verified)
                throw new RuntimeException("ID number does not match the customer's KYC records");
        }

        if (verified) result = "SUCCESS";

        Long tellerId = getCallerId();
        auditService.log(tellerId, "TELLER", "CUSTOMER_VERIFICATION",
                "ACCOUNT", accountNo,
                "Method: " + verificationMethod + " | Result: " + result);

        if (!verified) throw new RuntimeException("Verification failed");

        Map<String, Object> res = new HashMap<>();
        res.put("verified", true);
        res.put("customerName", customer.getFullName());
        res.put("accountNo", account.getAccountNo());
        res.put("accountStatus", account.getStatus());
        res.put("availableBalance", account.getAvailableBalance());
        return res;
    }

    // ── US070 — Cash Deposit ───────────────────────────────────────────────

    @PostMapping("/deposit/{accountNo}")
    public Map<String, Object> deposit(@PathVariable String accountNo,
                                       @RequestBody Map<String, BigDecimal> body) {
        BigDecimal amount = body.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Invalid deposit amount");

        Account account = accountRepo.findByAccountNo(accountNo)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!"ACTIVE".equals(account.getStatus()))
            throw new RuntimeException("Account is not active");

        // Verify account belongs to a real customer (prevents operating on arbitrary accounts)
        customerRepo.findById(account.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found for this account"));
        account.setLedgerBalance(account.getLedgerBalance().add(amount));
        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        accountRepo.save(account);

        Long tellerId = getCallerId();
        updateSession(tellerId, amount, BigDecimal.ZERO, BigDecimal.ZERO);
        auditService.log(tellerId, "TELLER", "CASH_DEPOSIT", "ACCOUNT", accountNo,
                "Deposited " + amount + " to " + accountNo);

        String receiptId = "DEP-" + System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        result.put("receiptId", receiptId);
        result.put("type", "DEPOSIT");
        result.put("amount", amount);
        result.put("newBalance", account.getAvailableBalance());
        result.put("accountNo", accountNo);
        result.put("txnId", "BR-TXN-" + System.currentTimeMillis());
        result.put("timestamp", Instant.now().toString());
        return result;
    }

    // ── US071 — Cash Withdrawal ────────────────────────────────────────────

    @PostMapping("/withdraw/{accountNo}")
    public Map<String, Object> withdraw(@PathVariable String accountNo,
                                        @RequestBody Map<String, BigDecimal> body) {
        BigDecimal amount = body.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Invalid withdrawal amount");

        Account account = accountRepo.findByAccountNo(accountNo)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!"ACTIVE".equals(account.getStatus()))
            throw new RuntimeException("Account is not active");

        // Verify account belongs to a real customer
        customerRepo.findById(account.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found for this account"));

        if (account.getAvailableBalance().compareTo(amount) < 0)
            throw new RuntimeException("Insufficient funds");

        account.setLedgerBalance(account.getLedgerBalance().subtract(amount));
        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        accountRepo.save(account);

        Long tellerId = getCallerId();
        updateSession(tellerId, BigDecimal.ZERO, amount, BigDecimal.ZERO);
        auditService.log(tellerId, "TELLER", "CASH_WITHDRAWAL", "ACCOUNT", accountNo,
                "Withdrew " + amount + " from " + accountNo);

        Map<String, Object> result = new HashMap<>();
        result.put("receiptId", "WDR-" + System.currentTimeMillis());
        result.put("type", "WITHDRAWAL");
        result.put("amount", amount);
        result.put("newBalance", account.getAvailableBalance());
        result.put("accountNo", accountNo);
        result.put("txnId", "BR-TXN-" + System.currentTimeMillis());
        result.put("timestamp", Instant.now().toString());
        return result;
    }

    // ── US072 — Cheque Processing ──────────────────────────────────────────

    @PostMapping("/cheque/{accountNo}")
    public ChequeRequest processCheque(@PathVariable String accountNo,
                                       @RequestBody Map<String, Object> body) {
        String chequeNumber = (String) body.get("chequeNumber");
        String drawerBank = (String) body.get("drawerBank");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        LocalDate chequeDate = LocalDate.parse((String) body.get("chequeDate"));

        if (chequeNumber == null || chequeNumber.isBlank())
            throw new RuntimeException("Cheque number is required");
        if (drawerBank == null || drawerBank.isBlank())
            throw new RuntimeException("Drawer bank is required");
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Amount must be greater than 0");

        Account account = accountRepo.findByAccountNo(accountNo)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!"ACTIVE".equals(account.getStatus()))
            throw new RuntimeException("Account is not active");

        Long tellerId = getCallerId();

        ChequeRequest cheque = new ChequeRequest();
        cheque.setReceiptId("CHQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        cheque.setAccountNo(accountNo);
        cheque.setCustomerId(account.getCustomerId());
        cheque.setChequeNumber(chequeNumber);
        cheque.setDrawerBank(drawerBank);
        cheque.setAmount(amount);
        cheque.setChequeDate(chequeDate);
        cheque.setStatus("IN_CLEARING");
        cheque.setTellerId(tellerId);
        ChequeRequest saved = chequeRepo.save(cheque);

        updateSession(tellerId, BigDecimal.ZERO, BigDecimal.ZERO, amount);
        auditService.log(tellerId, "TELLER", "CHEQUE_SUBMITTED", "ACCOUNT", accountNo,
                "Cheque " + chequeNumber + " submitted for clearing, amount: " + amount);

        return saved;
    }

    @GetMapping("/cheques/{accountNo}")
    public List<ChequeRequest> getCheques(@PathVariable String accountNo) {
        return chequeRepo.findByAccountNoOrderByCreatedAtDesc(accountNo);
    }

    // ── US073 — EOD Reconciliation ─────────────────────────────────────────

    @GetMapping("/eod-summary")
    public TellerSession getEodSummary() {
        Long tellerId = getCallerId();
        return sessionRepo.findByTellerIdAndSessionDate(tellerId, LocalDate.now())
                .orElseGet(() -> {
                    // Auto-create and persist session so it's ready for EOD submit
                    TellerSession s = new TellerSession();
                    s.setTellerId(tellerId);
                    s.setSessionDate(LocalDate.now());
                    s.setStatus("OPEN");
                    return sessionRepo.save(s);
                });
    }

    @PostMapping("/eod-submit")
    public TellerSession submitEod(@RequestBody Map<String, Object> body) {
        Long tellerId = getCallerId();
        // Auto-create session if none exists (teller may not have done any transactions today)
        TellerSession session = sessionRepo.findByTellerIdAndSessionDate(tellerId, LocalDate.now())
                .orElseGet(() -> {
                    TellerSession s = new TellerSession();
                    s.setTellerId(tellerId);
                    s.setSessionDate(LocalDate.now());
                    s.setStatus("OPEN");
                    return sessionRepo.save(s);
                });

        if ("SUBMITTED".equals(session.getStatus()))
            throw new RuntimeException("EOD already submitted for today.");

        BigDecimal physicalCash = new BigDecimal(body.getOrDefault("physicalCash", "0").toString());
        String mismatchReason = (String) body.getOrDefault("mismatchReason", "");

        BigDecimal systemNet = session.getTotalDeposits().subtract(session.getTotalWithdrawals());
        boolean mismatch = physicalCash.compareTo(systemNet) != 0;

        session.setStatus(mismatch ? "RECONCILIATION_PENDING" : "SUBMITTED");
        if (mismatch && mismatchReason != null && !mismatchReason.isBlank())
            session.setMismatchReason(mismatchReason);
        session.setSubmittedAt(Instant.now());
        TellerSession saved = sessionRepo.save(session);

        auditService.log(tellerId, "TELLER", "EOD_SUBMITTED", "TELLER_SESSION",
                String.valueOf(session.getId()),
                "EOD submitted. Status: " + saved.getStatus());
        return saved;
    }

    // ── US074 — Teller Report Submission ──────────────────────────────────

    @PostMapping("/report/submit")
    public Map<String, Object> submitReport(@RequestBody(required = false) Map<String, String> body) {
        Long tellerId = getCallerId();
        TellerSession session = sessionRepo.findByTellerIdAndSessionDate(tellerId, LocalDate.now())
                .orElseGet(() -> {
                    TellerSession s = new TellerSession();
                    s.setTellerId(tellerId);
                    s.setSessionDate(LocalDate.now());
                    s.setStatus("SUBMITTED");
                    return sessionRepo.save(s);
                });

        auditService.log(tellerId, "TELLER", "DAILY_REPORT_SUBMITTED", "TELLER_SESSION",
                String.valueOf(session.getId()),
                "Daily report submitted at " + Instant.now());

        return Map.of(
            "message", "Daily report submitted successfully",
            "submittedAt", Instant.now().toString(),
            "sessionId", session.getId(),
            "sessionDate", session.getSessionDate().toString()
        );
    }

    @PostMapping("/dev/reset-eod")
    public Map<String, Object> devResetEod() {
        Long tellerId = getCallerId();
        sessionRepo.findByTellerIdAndSessionDate(tellerId, LocalDate.now())
                .ifPresent(s -> { s.setStatus("OPEN"); s.setSubmittedAt(null); sessionRepo.save(s); });
        return Map.of("message", "EOD session reset to OPEN");
    }

    @GetMapping("/report/history")
    public List<TellerSession> getReportHistory() {
        Long tellerId = getCallerId();
        return sessionRepo.findAll().stream()
                .filter(s -> s.getTellerId().equals(tellerId))
                .sorted((a, b) -> b.getSessionDate().compareTo(a.getSessionDate()))
                .toList();
    }

    // ── Helper ─────────────────────────────────────────────────────────────

    private Long getCallerId() {
        String subject = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return Long.parseLong(subject.split(":")[1]);
    }

    private void updateSession(Long tellerId, BigDecimal deposit, BigDecimal withdrawal, BigDecimal cheque) {
        TellerSession session = sessionRepo.findByTellerIdAndSessionDate(tellerId, LocalDate.now())
                .orElseGet(() -> {
                    TellerSession s = new TellerSession();
                    s.setTellerId(tellerId);
                    s.setSessionDate(LocalDate.now());
                    s.setStatus("OPEN");
                    return s;
                });

        // Block transactions after EOD is submitted
        if ("SUBMITTED".equals(session.getStatus())) {
            throw new RuntimeException("EOD already submitted for today. No further transactions allowed.");
        }

        if (deposit.compareTo(BigDecimal.ZERO) > 0) {
            session.setTotalDeposits(session.getTotalDeposits().add(deposit));
            session.setDepositCount(session.getDepositCount() + 1);
        }
        if (withdrawal.compareTo(BigDecimal.ZERO) > 0) {
            session.setTotalWithdrawals(session.getTotalWithdrawals().add(withdrawal));
            session.setWithdrawalCount(session.getWithdrawalCount() + 1);
        }
        if (cheque.compareTo(BigDecimal.ZERO) > 0) {
            session.setTotalCheques(session.getTotalCheques().add(cheque));
            session.setChequeCount(session.getChequeCount() + 1);
        }
        sessionRepo.save(session);
    }
}
