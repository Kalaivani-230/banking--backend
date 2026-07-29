package com.example.MiniProject.controller;

import com.example.MiniProject.dto.DomesticTransferRequest;
import com.example.MiniProject.entity.DomesticTransaction;
import com.example.MiniProject.service.DomesticTransferService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/customer/domestic-transfer")
public class DomesticTransferController {

    private final DomesticTransferService service;

    public DomesticTransferController(DomesticTransferService service) {
        this.service = service;
    }

    // Step 1: validate + initiate + send OTP
    @PostMapping("/{customerId}/initiate")
    public DomesticTransaction initiate(@PathVariable Long customerId,
                                        @Valid @RequestBody DomesticTransferRequest req) {
        return service.initiate(customerId, req);
    }

    // Step 2: confirm with OTP
    @PostMapping("/{customerId}/confirm")
    public DomesticTransaction confirm(@PathVariable Long customerId,
                                       @RequestBody Map<String, String> body) {
        return service.confirm(customerId, body.get("referenceId"), body.get("otpCode"));
    }

    // Transaction history with optional filters
    @GetMapping("/{customerId}/history")
    public List<DomesticTransaction> history(
            @PathVariable Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        Instant from = dateFrom != null ? dateFrom.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant to   = dateTo  != null ? dateTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        return service.getFilteredHistory(customerId, status, from, to);
    }
}
