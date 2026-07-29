package com.example.MiniProject.controller;

import com.example.MiniProject.entity.InternationalTransaction;
import com.example.MiniProject.entity.InternalUser;
import com.example.MiniProject.repository.AccountRepository;
import com.example.MiniProject.repository.InternalUserRepository;
import com.example.MiniProject.repository.InternationalTransactionRepository;
import com.example.MiniProject.service.AuditService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ops")
public class OpsController {

    private final InternationalTransactionRepository txnRepo;
    private final AuditService auditService;
    private final InternalUserRepository internalUserRepo;
    private final BCryptPasswordEncoder encoder;
    private final AccountRepository accountRepo;

    public OpsController(InternationalTransactionRepository txnRepo, AuditService auditService,
                         InternalUserRepository internalUserRepo, BCryptPasswordEncoder encoder,
                         AccountRepository accountRepo) {
        this.txnRepo = txnRepo;
        this.auditService = auditService;
        this.internalUserRepo = internalUserRepo;
        this.encoder = encoder;
        this.accountRepo = accountRepo;
    }

    // ── Dev: seed OPS user ────────────────────────────────────────────────
    @GetMapping("/dev/seed-ops-user")
    public Map<String, String> seedOpsUser() {
        if (internalUserRepo.existsByEmployeeId("EMP4001")) {
            InternalUser existing = internalUserRepo.findByEmployeeId("EMP4001").orElse(null);
            existing.setRole("OPS");
            existing.setStatus("ACTIVE");
            existing.setFailedLoginAttempts(0);
            existing.setPasswordHash(encoder.encode("Password@123"));
            internalUserRepo.save(existing);
            return Map.of("message", "EMP4001 reset: Role=OPS, Status=ACTIVE, Password=Password@123");
        }
        InternalUser u = new InternalUser();
        u.setEmployeeId("EMP4001");
        u.setFullName("Ops One");
        u.setEmail("ops1@bank.com");
        u.setRole("OPS");
        u.setStatus("ACTIVE");
        u.setFailedLoginAttempts(0);
        u.setPasswordHash(encoder.encode("Password@123"));
        internalUserRepo.save(u);
        return Map.of("message", "EMP4001 created. Login with EMP4001 / Password@123");
    }

    // ── US061 — View all transactions with search/filter ──────────────────

    @GetMapping("/transactions")
    public List<Map<String, Object>> searchTransactions(
            @RequestParam(required = false) String referenceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        return txnRepo.findAll().stream()
                .filter(t -> referenceId == null || t.getReferenceId().contains(referenceId))
                .filter(t -> status == null || status.equalsIgnoreCase(t.getStatus()))
                .filter(t -> {
                    if (dateFrom == null) return true;
                    Instant from = dateFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
                    return t.getCreatedAt() != null && !t.getCreatedAt().isBefore(from);
                })
                .filter(t -> {
                    if (dateTo == null) return true;
                    Instant to = dateTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
                    return t.getCreatedAt() != null && t.getCreatedAt().isBefore(to);
                })
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::enrich)
                .toList();
    }

    @GetMapping("/transactions/{referenceId}")
    public InternationalTransaction getTransaction(@PathVariable String referenceId) {
        return txnRepo.findByReferenceId(referenceId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    // ── US062 — Failed transactions with failure stage filter ─────────────

    @GetMapping("/transactions/failed")
    public List<InternationalTransaction> getFailedTransactions(
            @RequestParam(required = false) String failureStage) {
        return txnRepo.findAll().stream()
                .filter(t -> "FAILED".equals(t.getStatus()) || "BLOCKED".equals(t.getStatus()))
                .filter(t -> failureStage == null || failureStage.equalsIgnoreCase(t.getFailureStage()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    // ── US063 — Flag suspicious transfer → PENDING_COMPLIANCE_ESCALATED ───

    @PostMapping("/transactions/{referenceId}/flag")
    public InternationalTransaction flagTransaction(@PathVariable String referenceId,
                                                    @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        if (reason == null || reason.isBlank())
            throw new RuntimeException("Flag reason is required");

        InternationalTransaction txn = txnRepo.findByReferenceId(referenceId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        String oldStatus = txn.getStatus();

        // BLOCKED = already compliance-rejected, cannot re-flag
        // FAILED = can be flagged (e.g. limit-exceeded, suspicious failure)
        if ("BLOCKED".equals(oldStatus))
            throw new RuntimeException("Cannot flag a BLOCKED transaction.");
        if ("PENDING_COMPLIANCE_ESCALATED".equals(oldStatus))
            throw new RuntimeException("Transaction is already escalated.");

        txn.setStatus("PENDING_COMPLIANCE_ESCALATED");
        txn.setComplianceReason(reason);
        InternationalTransaction saved = txnRepo.save(txn);

        Long opsUserId = getCallerId();
        auditService.log(opsUserId, "OPS", "TXN_FLAGGED_BY_OPS",
                "TRANSACTION", referenceId,
                "Ops flagged transaction. Reason: " + reason, oldStatus, "PENDING_COMPLIANCE_ESCALATED");

        return saved;
    }

    private Long getCallerId() {
        String subject = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return Long.parseLong(subject.split(":")[1]);
    }

    private Map<String, Object> enrich(InternationalTransaction t) {
        Map<String, Object> m = new HashMap<>();
        m.put("referenceId",      t.getReferenceId());
        m.put("customerId",       t.getCustomerId());
        m.put("fromAccountNo",    t.getFromAccountNo());
        m.put("fromCurrency",     t.getFromCurrency());
        m.put("toCurrency",       t.getToCurrency());
        m.put("sendAmount",       t.getSendAmount());
        m.put("totalDebitAmount", t.getTotalDebitAmount());
        m.put("receiverGets",     t.getReceiverGets());
        m.put("quoteRef",         t.getQuoteRef());
        m.put("status",           t.getStatus());
        m.put("amlRiskLevel",     t.getAmlRiskLevel());
        m.put("amlRiskScore",     t.getAmlRiskScore());
        m.put("amlFlags",         t.getAmlFlags());
        m.put("failureStage",     t.getFailureStage());
        m.put("failureReason",    t.getFailureReason());
        m.put("complianceReason", t.getComplianceReason());
        m.put("createdAt",        t.getCreatedAt());
        m.put("completedAt",      t.getCompletedAt());
        // Enrich with account info
        accountRepo.findByAccountNo(t.getFromAccountNo()).ifPresent(a -> {
            m.put("accountId",     a.getId());
            m.put("accountStatus", a.getStatus());
        });
        return m;
    }

    @GetMapping("/dev/whoami")
    public Map<String, Object> whoami() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return Map.of(
            "principal", auth.getPrincipal().toString(),
            "authorities", auth.getAuthorities().toString()
        );
    }
}
