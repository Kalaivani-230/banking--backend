package com.example.MiniProject.controller;

import com.example.MiniProject.entity.FeeQuote;
import com.example.MiniProject.entity.MasterCorridor;
import com.example.MiniProject.repository.MasterCorridorRepository;
import com.example.MiniProject.service.FeeEngineService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/customer/fee-quote")
public class FeeQuoteController {

    private final FeeEngineService feeEngine;
    private final MasterCorridorRepository corridorRepo;

    public FeeQuoteController(FeeEngineService feeEngine, MasterCorridorRepository corridorRepo) {
        this.feeEngine = feeEngine;
        this.corridorRepo = corridorRepo;
    }

    @PostMapping
    public FeeQuote requestQuote(@RequestBody Map<String, Object> body) {
        Long       customerId   = Long.parseLong(body.get("customerId").toString());
        String     fromCurrency = (String) body.get("fromCurrency");
        String     toCurrency   = (String) body.get("toCurrency");
        String     channelCode  = (String) body.getOrDefault("channelCode", "ONLINE");
        String     customerType = (String) body.getOrDefault("customerTypeCode", "RETAIL");
        BigDecimal sendAmount   = new BigDecimal(body.get("sendAmount").toString());

        // Resolve corridorId from currency pair — frontend no longer needs to pass it
        Long corridorId;
        if (body.get("corridorId") != null) {
            corridorId = Long.parseLong(body.get("corridorId").toString());
        } else {
            String fromCountry = countryForCurrency(fromCurrency);
            String toCountry   = countryForCurrency(toCurrency);
            MasterCorridor corridor = corridorRepo
                    .findByFromCountryCodeAndToCountryCodeAndIsActiveTrue(fromCountry, toCountry)
                    .orElseThrow(() -> new RuntimeException(
                            "No active corridor for " + fromCurrency + " → " + toCurrency));
            corridorId = corridor.getId();
        }

        return feeEngine.calculateQuote(
                customerId, fromCurrency, toCurrency,
                channelCode, customerType, corridorId, sendAmount);
    }

    @GetMapping("/{quoteRef}")
    public FeeQuote getQuote(@PathVariable String quoteRef) {
        return feeEngine.getByRef(quoteRef);
    }

    private String countryForCurrency(String currency) {
        return switch (currency.toUpperCase()) {
            case "INR" -> "IN";
            case "USD" -> "US";
            case "GBP" -> "GB";
            case "EUR" -> "EU";
            case "SGD" -> "SG";
            case "AED" -> "AE";
            default -> throw new RuntimeException("Unknown currency: " + currency);
        };
    }
}
