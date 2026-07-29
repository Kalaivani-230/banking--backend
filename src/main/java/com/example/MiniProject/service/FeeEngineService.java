package com.example.MiniProject.service;

import com.example.MiniProject.entity.FeeComponent;
import com.example.MiniProject.entity.FeePolicy;
import com.example.MiniProject.entity.FeeQuote;
import com.example.MiniProject.entity.FxRate;
import com.example.MiniProject.repository.FeeComponentRepository;
import com.example.MiniProject.repository.FeePolicyRepository;
import com.example.MiniProject.repository.FeeQuoteRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class FeeEngineService {

    private final FeePolicyRepository policyRepo;
    private final FeeComponentRepository componentRepo;
    private final FeeQuoteRepository quoteRepo;
    private final FxRateService fxRateService;

    public FeeEngineService(FeePolicyRepository policyRepo,
                            FeeComponentRepository componentRepo,
                            FeeQuoteRepository quoteRepo,
                            FxRateService fxRateService) {
        this.policyRepo    = policyRepo;
        this.componentRepo = componentRepo;
        this.quoteRepo     = quoteRepo;
        this.fxRateService = fxRateService;
    }

    /**
     * US026 — Fetch eligible fee rule by corridor/currency/channel/customerType/amount slab.
     * US028 — Fetch active FX rate.
     * US029 — Apply FX markup.
     * US030 — Compute all fee components + tax.
     * US031 — Persist and return full quote response.
     */
    public FeeQuote calculateQuote(Long customerId,
                                   String fromCurrency, String toCurrency,
                                   String channelCode, String customerTypeCode,
                                   Long corridorId, BigDecimal sendAmount) {

        if (sendAmount == null || sendAmount.compareTo(new BigDecimal("100")) < 0)
            throw new RuntimeException("Minimum transfer amount is ₹100.");

        // US026 — fetch matching ACTIVE policies for this corridor/currency/channel/type
        List<FeePolicy> candidates = policyRepo
                .findByCorridorIdAndChannelCodeAndCustomerTypeCodeAndStatus(
                        corridorId, channelCode, customerTypeCode, "ACTIVE");

        if (candidates.isEmpty())
            throw new RuntimeException("Pricing not configured for this corridor/currency.");

        // Filter by currency pair and amount slab, pick highest priority (lowest number = highest priority)
        FeePolicy policy = candidates.stream()
                .filter(p -> p.getSendCurrency().equalsIgnoreCase(fromCurrency)
                          && p.getReceiveCurrency().equalsIgnoreCase(toCurrency))
                .filter(p -> {
                    boolean minOk = p.getAmountMin() == null
                            || sendAmount.compareTo(p.getAmountMin()) >= 0;
                    boolean maxOk = p.getAmountMax() == null
                            || sendAmount.compareTo(p.getAmountMax()) <= 0;
                    return minOk && maxOk;
                })
                .min(Comparator.comparingInt(FeePolicy::getPriority))
                .orElseThrow(() -> new RuntimeException(
                        "Pricing not configured for this corridor/currency."));

        // US028 — fetch active FX rate (stored as: 1 toCurrency = X fromCurrency, e.g. 1 USD = 83 INR)
        FxRate fxRate = fxRateService.getActiveRate(toCurrency, fromCurrency);

        // US029 — effective rate = base rate adjusted by markup
        BigDecimal markupPct    = fxRate.getMarkupPercent() != null
                ? fxRate.getMarkupPercent() : BigDecimal.ZERO;
        BigDecimal fxMarkupAmt  = sendAmount
                .multiply(markupPct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // US030 — compute fee components
        List<FeeComponent> components = componentRepo.findByPolicyId(policy.getId());

        BigDecimal baseFee         = BigDecimal.ZERO;
        BigDecimal intermediaryFee = BigDecimal.ZERO;
        BigDecimal handlingFee     = BigDecimal.ZERO;
        BigDecimal taxPercent      = BigDecimal.ZERO;

        for (FeeComponent c : components) {
            BigDecimal computed = computeComponent(c, sendAmount);
            switch (c.getComponentType().toUpperCase()) {
                case "BASE_FEE"         -> baseFee         = computed;
                case "INTERMEDIARY_FEE" -> intermediaryFee = computed;
                case "HANDLING_FEE"     -> handlingFee     = computed;
                case "TAX"              -> taxPercent       = c.getValue(); // stored as percent
            }
        }

        // Tax applied on (baseFee + intermediaryFee + handlingFee + fxMarkupAmt)
        BigDecimal taxableAmount = baseFee.add(intermediaryFee).add(handlingFee).add(fxMarkupAmt);
        BigDecimal taxAmount     = taxableAmount
                .multiply(taxPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal totalFees       = baseFee.add(intermediaryFee).add(handlingFee)
                                            .add(fxMarkupAmt).add(taxAmount);
        BigDecimal totalDebitAmt   = sendAmount.add(totalFees);

        // Receiver gets = sendAmount / fxRate (markup already reflected in fees separately)
        BigDecimal receiverGets    = sendAmount
                .divide(fxRate.getRate(), 2, RoundingMode.HALF_UP);

        // US031 — persist quote
        FeeQuote quote = new FeeQuote();
        quote.setQuoteRef("QT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        quote.setCustomerId(customerId);
        quote.setPolicyId(policy.getId());
        quote.setPolicyVersion(policy.getVersionNumber());
        quote.setFxRateId(fxRate.getId());
        quote.setFromCurrency(fromCurrency.toUpperCase());
        quote.setToCurrency(toCurrency.toUpperCase());
        quote.setSendAmount(sendAmount);
        quote.setFxRate(fxRate.getRate());
        quote.setFxMarkupPercent(markupPct);
        quote.setFxMarkupAmount(fxMarkupAmt);
        quote.setBaseFee(baseFee);
        quote.setIntermediaryFee(intermediaryFee);
        quote.setHandlingFee(handlingFee);
        quote.setTaxAmount(taxAmount);
        quote.setTotalFees(totalFees);
        quote.setTotalDebitAmount(totalDebitAmt);
        quote.setReceiverGets(receiverGets);
        quote.setExpiresAt(Instant.now().plusSeconds(60));
        quote.setStatus("ACTIVE");

        return quoteRepo.save(quote);
    }

    // Mark quote as USED (called when transfer is confirmed)
    public FeeQuote useQuote(String quoteRef) {
        FeeQuote q = quoteRepo.findByQuoteRef(quoteRef)
                .orElseThrow(() -> new RuntimeException("Quote not found"));
        if ("EXPIRED".equals(q.getStatus()) || Instant.now().isAfter(q.getExpiresAt())) {
            q.setStatus("EXPIRED");
            quoteRepo.save(q);
            throw new RuntimeException("Quote expired. Please recalculate fees.");
        }
        if ("USED".equals(q.getStatus()))
            throw new RuntimeException("Quote already used.");
        q.setStatus("USED");
        return quoteRepo.save(q);
    }

    public FeeQuote getByRef(String quoteRef) {
        FeeQuote q = quoteRepo.findByQuoteRef(quoteRef)
                .orElseThrow(() -> new RuntimeException("Quote not found"));
        // Auto-expire if past expiry
        if ("ACTIVE".equals(q.getStatus()) && Instant.now().isAfter(q.getExpiresAt())) {
            q.setStatus("EXPIRED");
            quoteRepo.save(q);
        }
        return q;
    }

    // Compute a single fee component value
    private BigDecimal computeComponent(FeeComponent c, BigDecimal amount) {
        BigDecimal raw;
        if ("PERCENT".equalsIgnoreCase(c.getCalcType())) {
            raw = amount.multiply(c.getValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            raw = c.getValue().setScale(2, RoundingMode.HALF_UP);
        }
        // Apply min/max caps
        if (c.getMinFee() != null && raw.compareTo(c.getMinFee()) < 0) raw = c.getMinFee();
        if (c.getMaxFee() != null && raw.compareTo(c.getMaxFee()) > 0) raw = c.getMaxFee();
        return raw;
    }
}
