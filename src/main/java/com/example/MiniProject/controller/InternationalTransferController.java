package com.example.MiniProject.controller;

import com.example.MiniProject.entity.InternationalTransaction;
import com.example.MiniProject.service.InternationalTransferService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@RestController
public class InternationalTransferController {

    private final InternationalTransferService service;

    public InternationalTransferController(InternationalTransferService service) {
        this.service = service;
    }

    // ── Customer endpoints (/customer/intl-transfer) ──────────────────────

    // US024 Step 1 — initiate (validate quote, send OTP)
    @PostMapping("/customer/intl-transfer/{customerId}/initiate")
    public InternationalTransaction initiate(@PathVariable Long customerId,
                                             @RequestBody Map<String, Object> body) {
        verifyCallerIsCustomer(customerId);
        return service.initiate(customerId, body);
    }

    // US024 Step 2 — confirm with OTP → full pipeline
    @PostMapping("/customer/intl-transfer/{customerId}/confirm")
    public InternationalTransaction confirm(@PathVariable Long customerId,
                                            @RequestBody Map<String, String> body) {
        verifyCallerIsCustomer(customerId);
        return service.confirm(customerId, body.get("referenceId"), body.get("otpCode"));
    }

    // US042 — Customer transaction history (intl)
    @GetMapping("/customer/intl-transfer/{customerId}/history")
    public List<InternationalTransaction> history(
            @PathVariable Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        verifyCallerIsCustomer(customerId);
        Instant from = dateFrom != null ? dateFrom.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant to   = dateTo  != null ? dateTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        return service.getCustomerHistory(customerId, status, from, to);
    }

    // Get single transaction detail
    @GetMapping("/customer/intl-transfer/detail/{referenceId}")
    public InternationalTransaction detail(@PathVariable String referenceId) {
        return service.getByRef(referenceId);
    }

    // ── Compliance endpoints (/compliance/intl-transfer) ─────────────────

    // US036/037 — Compliance queue
    @GetMapping("/compliance/intl-transfer/pending")
    public List<InternationalTransaction> complianceQueue() {
        return service.getComplianceQueue();
    }

    // US037 — Approve
    @PostMapping("/compliance/intl-transfer/{referenceId}/approve")
    public InternationalTransaction approve(@PathVariable String referenceId) {
        Long officerId = getCallerId();
        return service.complianceApprove(referenceId, officerId);
    }

    // US037 — Reject
    @PostMapping("/compliance/intl-transfer/{referenceId}/reject")
    public InternationalTransaction reject(@PathVariable String referenceId,
                                           @RequestBody Map<String, String> body) {
        Long officerId = getCallerId();
        return service.complianceReject(referenceId, officerId, body.get("reason"));
    }

    // US064 — Compliance escalated queue (ops-flagged)
    @GetMapping("/compliance/intl-transfer/escalated")
    public List<InternationalTransaction> escalatedQueue() {
        return service.getEscalatedQueue();
    }

    // US064 — Approve escalated
    @PostMapping("/compliance/intl-transfer/escalated/{referenceId}/approve")
    public InternationalTransaction approveEscalated(@PathVariable String referenceId) {
        Long officerId = getCallerId();
        return service.complianceApprove(referenceId, officerId);
    }

    // US064 — Reject escalated
    @PostMapping("/compliance/intl-transfer/escalated/{referenceId}/reject")
    public InternationalTransaction rejectEscalated(@PathVariable String referenceId,
                                                    @RequestBody Map<String, String> body) {
        Long officerId = getCallerId();
        return service.complianceReject(referenceId, officerId, body.get("reason"));
    }

    private Long getCallerId() {
        String subject = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return Long.parseLong(subject.split(":")[1]);
    }

    private void verifyCallerIsCustomer(Long customerId) {
        String subject = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        if (!subject.startsWith("customer:"))
            throw new RuntimeException("Unauthorized");
        Long callerId = Long.parseLong(subject.split(":")[1]);
        if (!callerId.equals(customerId))
            throw new RuntimeException("Unauthorized: customer ID mismatch");
    }
}
