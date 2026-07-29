package com.example.MiniProject.service;

import com.example.MiniProject.entity.FxRate;
import com.example.MiniProject.repository.FxRateRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class FxRateService {

    private final FxRateRepository repo;

    public FxRateService(FxRateRepository repo) { this.repo = repo; }

    // US058 — Add a new FX rate (starts INACTIVE)
    public FxRate add(String fromCurrency, String toCurrency,
                      BigDecimal rate, BigDecimal markupPercent,
                      Instant effectiveFrom, Long createdBy) {

        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Rate must be greater than zero");
        if (effectiveFrom == null)
            throw new RuntimeException("Effective from date is required");

        FxRate fx = new FxRate();
        fx.setFromCurrency(fromCurrency.toUpperCase());
        fx.setToCurrency(toCurrency.toUpperCase());
        fx.setRate(rate);
        fx.setMarkupPercent(markupPercent != null ? markupPercent : BigDecimal.ZERO);
        fx.setEffectiveFrom(effectiveFrom);
        fx.setCreatedBy(createdBy);
        fx.setStatus("INACTIVE");
        return repo.save(fx);
    }

    // US058 — Update an existing INACTIVE rate
    public FxRate update(Long id, BigDecimal rate, BigDecimal markupPercent, Instant effectiveFrom) {
        FxRate fx = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("FX rate not found"));
        if ("ACTIVE".equals(fx.getStatus()))
            throw new RuntimeException("Cannot edit an ACTIVE rate. Create a new one instead.");
        if (rate != null) {
            if (rate.compareTo(BigDecimal.ZERO) <= 0)
                throw new RuntimeException("Rate must be greater than zero");
            fx.setRate(rate);
        }
        if (markupPercent != null) fx.setMarkupPercent(markupPercent);
        if (effectiveFrom != null) fx.setEffectiveFrom(effectiveFrom);
        return repo.save(fx);
    }

    // US059 — Activate a rate; deactivate any existing ACTIVE rate for same pair
    public FxRate activate(Long id) {
        FxRate fx = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("FX rate not found"));

        // Deactivate all currently active rates for this pair
        List<FxRate> active = repo.findByFromCurrencyAndToCurrencyAndStatus(
                fx.getFromCurrency(), fx.getToCurrency(), "ACTIVE");
        for (FxRate a : active) {
            a.setStatus("INACTIVE");
            repo.save(a);
        }

        fx.setStatus("ACTIVE");
        return repo.save(fx);
    }

    // US059 — Deactivate an active rate
    public FxRate deactivate(Long id) {
        FxRate fx = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("FX rate not found"));
        if (!"ACTIVE".equals(fx.getStatus()))
            throw new RuntimeException("Rate is not currently ACTIVE");
        fx.setStatus("INACTIVE");
        return repo.save(fx);
    }

    // US060 — Full history, optionally filtered by currency pair
    public List<FxRate> history(String fromCurrency, String toCurrency) {
        if (fromCurrency != null && toCurrency != null)
            return repo.findByFromCurrencyAndToCurrencyOrderByCreatedAtDesc(
                    fromCurrency.toUpperCase(), toCurrency.toUpperCase());
        return repo.findAllByOrderByCreatedAtDesc();
    }

    // Used by fee engine (US028)
    public FxRate getActiveRate(String fromCurrency, String toCurrency) {
        return repo.findTopByFromCurrencyAndToCurrencyAndStatusOrderByEffectiveFromDesc(
                        fromCurrency.toUpperCase(), toCurrency.toUpperCase(), "ACTIVE")
                .orElseThrow(() -> new RuntimeException(
                        "FX rate unavailable for " + fromCurrency + " → " + toCurrency));
    }
}
