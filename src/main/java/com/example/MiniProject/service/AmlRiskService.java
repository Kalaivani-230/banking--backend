package com.example.MiniProject.service;

import com.example.MiniProject.entity.InternationalTransaction;
import com.example.MiniProject.repository.InternationalTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AmlRiskService {

    // High-risk destination currencies (simplified corridor proxy)
    private static final Set<String> HIGH_RISK_CURRENCIES = Set.of("IRR", "KPW", "SYP", "MMK");
    private static final Set<String> MEDIUM_RISK_CURRENCIES = Set.of("NGN", "PKR", "AFN", "SDG");

    private static final BigDecimal HIGH_AMOUNT_THRESHOLD   = new BigDecimal("50000");  // 50k INR → HIGH risk
    private static final BigDecimal MEDIUM_AMOUNT_THRESHOLD = new BigDecimal("20000");  // 20k INR → MEDIUM risk
    private static final int HIGH_FREQ_THRESHOLD   = 5; // >5 intl txns in 24h
    private static final int MEDIUM_FREQ_THRESHOLD = 3;

    private final InternationalTransactionRepository txnRepo;

    public AmlRiskService(InternationalTransactionRepository txnRepo) {
        this.txnRepo = txnRepo;
    }

    /**
     * US034 — Deterministic AML risk scoring.
     * Returns the transaction with amlRiskLevel, amlRiskScore, amlFlags set.
     */
    public InternationalTransaction score(InternationalTransaction txn) {
        int score = 0;
        List<String> flags = new ArrayList<>();

        // Rule 1: High-risk corridor
        if (HIGH_RISK_CURRENCIES.contains(txn.getToCurrency().toUpperCase())) {
            score += 50;
            flags.add("HIGH_RISK_CORRIDOR");
        } else if (MEDIUM_RISK_CURRENCIES.contains(txn.getToCurrency().toUpperCase())) {
            score += 25;
            flags.add("MEDIUM_RISK_CORRIDOR");
        }

        // Rule 2: High amount threshold
        if (txn.getSendAmount().compareTo(HIGH_AMOUNT_THRESHOLD) >= 0) {
            score += 50;  // Ensure HIGH risk alone triggers compliance queue
            flags.add("HIGH_AMOUNT");
        } else if (txn.getSendAmount().compareTo(MEDIUM_AMOUNT_THRESHOLD) >= 0) {
            score += 20;
            flags.add("MEDIUM_AMOUNT");
        }

        // Rule 3: Transaction frequency in last 24h
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        List<InternationalTransaction> recent = txnRepo
                .findByFromAccountNoAndStatusInAndCreatedAtAfter(
                        txn.getFromAccountNo(),
                        List.of("INITIATED", "VALIDATED", "APPROVED", "DEBITED",
                                "PROCESSING", "SETTLED", "COMPLETED"),
                        since);

        int freq = recent.size();
        if (freq >= HIGH_FREQ_THRESHOLD) {
            score += 30;
            flags.add("HIGH_FREQUENCY");
        } else if (freq >= MEDIUM_FREQ_THRESHOLD) {
            score += 15;
            flags.add("MEDIUM_FREQUENCY");
        }

        // Determine risk level
        String riskLevel;
        if (score >= 50) riskLevel = "HIGH";
        else if (score >= 20) riskLevel = "MEDIUM";
        else riskLevel = "LOW";

        txn.setAmlRiskScore(score);
        txn.setAmlRiskLevel(riskLevel);
        txn.setAmlFlags(flags.isEmpty() ? null : String.join(",", flags));

        return txn;
    }
}
