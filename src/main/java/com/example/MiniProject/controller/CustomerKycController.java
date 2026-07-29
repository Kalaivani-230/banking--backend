package com.example.MiniProject.controller;

import com.example.MiniProject.dto.KycSubmitRequest;
import com.example.MiniProject.entity.KycRequest;
import com.example.MiniProject.service.KycService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer/kyc")
public class CustomerKycController {

    private final KycService kycService;

    public CustomerKycController(KycService kycService) {
        this.kycService = kycService;
    }

    // TEMP: pass customerId (later we will extract from JWT claims)
    @PostMapping("/submit/{customerId}")
    public KycRequest submit(@PathVariable Long customerId,
                             @Valid @RequestBody KycSubmitRequest req) {
        return kycService.submitKyc(customerId, req);
    }

    @GetMapping("/status/{customerId}")
    public KycRequest status(@PathVariable Long customerId) {
        return kycService.getLatestKyc(customerId);
    }
}