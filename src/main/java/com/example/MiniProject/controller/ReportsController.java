package com.example.MiniProject.controller;

import com.example.MiniProject.entity.AuditLog;
import com.example.MiniProject.entity.FeePolicy;
import com.example.MiniProject.entity.FeeQuote;
import com.example.MiniProject.entity.InternationalTransaction;
import com.example.MiniProject.repository.ApprovalRepository;
import com.example.MiniProject.repository.FeePolicyRepository;
import com.example.MiniProject.repository.FeeQuoteRepository;
import com.example.MiniProject.repository.InternationalTransactionRepository;
import com.example.MiniProject.service.AuditService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
public class ReportsController {

    private final FeeQuoteRepository quoteRepo;
    private final InternationalTransactionRepository txnRepo;
    private final AuditService auditService;
    private final ApprovalRepository approvalRepo;
    private final FeePolicyRepository feePolicyRepo;

    public ReportsController(FeeQuoteRepository quoteRepo,
                             InternationalTransactionRepository txnRepo,
                             AuditService auditService,
                             ApprovalRepository approvalRepo,
                             FeePolicyRepository feePolicyRepo) {
        this.quoteRepo = quoteRepo;
        this.txnRepo = txnRepo;
        this.auditService = auditService;
        this.approvalRepo = approvalRepo;
        this.feePolicyRepo = feePolicyRepo;
    }

    // ── US065 — Fee Revenue Report (Admin) ────────────────────────────────

    @GetMapping("/admin/reports/fee-revenue")
    public Map<String, Object> feeRevenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String fromCurrency,
            @RequestParam(required = false) String toCurrency) {

        Instant from = dateFrom != null ? dateFrom.atStartOfDay(ZoneOffset.UTC).toInstant() : Instant.EPOCH;
        Instant to   = dateTo  != null ? dateTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : Instant.now();

        List<FeeQuote> quotes = quoteRepo.findAll().stream()
                .filter(q -> "USED".equals(q.getStatus()))
                .filter(q -> q.getCreatedAt() != null
                        && !q.getCreatedAt().isBefore(from)
                        && q.getCreatedAt().isBefore(to))
                .filter(q -> fromCurrency == null || fromCurrency.equalsIgnoreCase(q.getFromCurrency()))
                .filter(q -> toCurrency == null || toCurrency.equalsIgnoreCase(q.getToCurrency()))
                .toList();

        BigDecimal totalBaseFee         = safeSum(quotes, FeeQuote::getBaseFee);
        BigDecimal totalIntermediaryFee = safeSum(quotes, FeeQuote::getIntermediaryFee);
        BigDecimal totalHandlingFee     = safeSum(quotes, FeeQuote::getHandlingFee);
        BigDecimal totalFxMarkup        = safeSum(quotes, FeeQuote::getFxMarkupAmount);
        BigDecimal totalTax             = safeSum(quotes, FeeQuote::getTaxAmount);
        BigDecimal totalRevenue         = safeSum(quotes, FeeQuote::getTotalFees);

        Map<String, BigDecimal> byCurrency = quotes.stream()
                .collect(Collectors.groupingBy(
                        q -> q.getFromCurrency() + "->" + q.getToCurrency(),
                        Collectors.reducing(BigDecimal.ZERO, FeeQuote::getTotalFees, BigDecimal::add)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalTransactions", quotes.size());
        result.put("totalBaseFee", totalBaseFee);
        result.put("totalIntermediaryFee", totalIntermediaryFee);
        result.put("totalHandlingFee", totalHandlingFee);
        result.put("totalFxMarkup", totalFxMarkup);
        result.put("totalTax", totalTax);
        result.put("totalRevenue", totalRevenue);
        result.put("revenueByCurrency", byCurrency);
        result.put("dateFrom", from.toString());
        result.put("dateTo", to.toString());
        return result;
    }

    // ── US067 — Compliance Report ─────────────────────────────────────────

    @GetMapping("/compliance/reports")
    public Map<String, Object> complianceReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String status) {

        Instant from = dateFrom != null ? dateFrom.atStartOfDay(ZoneOffset.UTC).toInstant() : Instant.EPOCH;
        Instant to   = dateTo  != null ? dateTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : Instant.now();

        List<String> complianceStatuses = List.of(
                "PENDING_COMPLIANCE", "PENDING_COMPLIANCE_ESCALATED", "APPROVED", "BLOCKED");

        List<InternationalTransaction> txns = txnRepo.findAll().stream()
                .filter(t -> complianceStatuses.contains(t.getStatus()))
                .filter(t -> status == null || status.equalsIgnoreCase(t.getStatus()))
                .filter(t -> t.getCreatedAt() != null
                        && !t.getCreatedAt().isBefore(from)
                        && t.getCreatedAt().isBefore(to))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();

        long flagged  = txns.stream().filter(t -> t.getStatus().startsWith("PENDING_COMPLIANCE")).count();
        long approved = txns.stream().filter(t -> "APPROVED".equals(t.getStatus())).count();
        long blocked  = txns.stream().filter(t -> "BLOCKED".equals(t.getStatus())).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalFlagged", flagged);
        result.put("totalApproved", approved);
        result.put("totalRejected", blocked);
        result.put("transactions", txns);
        result.put("dateFrom", from.toString());
        result.put("dateTo", to.toString());

        // BUG-013: Include fee policy approvals in compliance report
        List<FeePolicy> feePolicies = feePolicyRepo.findAll().stream()
                .filter(p -> "APPROVED".equals(p.getStatus()) || "REJECTED".equals(p.getStatus())
                          || "PENDING_APPROVAL".equals(p.getStatus()))
                .filter(p -> p.getCreatedAt() != null
                        && !p.getCreatedAt().isBefore(from)
                        && p.getCreatedAt().isBefore(to))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
        result.put("feePolicies", feePolicies);
        result.put("totalFeePoliciesApproved", feePolicies.stream().filter(p -> "APPROVED".equals(p.getStatus())).count());
        result.put("totalFeePoliciesRejected", feePolicies.stream().filter(p -> "REJECTED".equals(p.getStatus())).count());

        return result;
    }

    // ── US044 — Audit Log Query ───────────────────────────────────────────

    @GetMapping("/admin/audit-logs")
    public List<AuditLog> getAuditLogs(
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        Instant from = dateFrom != null ? dateFrom.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant to   = dateTo  != null ? dateTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        return auditService.search(actorId, action, entityType, from, to);
    }

    // ── US064 — Compliance escalated queue ───────────────────────────────

    @GetMapping("/compliance/escalated")
    public List<InternationalTransaction> getEscalatedQueue() {
        return txnRepo.findByStatusOrderByCreatedAtDesc("PENDING_COMPLIANCE_ESCALATED");
    }

    private BigDecimal safeSum(List<FeeQuote> quotes, Function<FeeQuote, BigDecimal> getter) {
        return quotes.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
