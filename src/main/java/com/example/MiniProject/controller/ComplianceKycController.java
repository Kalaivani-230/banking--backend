package com.example.MiniProject.controller;

import com.example.MiniProject.dto.KycRejectRequest;
import com.example.MiniProject.entity.KycRequest;
import com.example.MiniProject.repository.KycRequestRepository;
import com.example.MiniProject.service.KycService;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/compliance/kyc")
public class ComplianceKycController {

    private final KycRequestRepository kycRepo;
    private final KycService kycService;

    public ComplianceKycController(KycRequestRepository kycRepo, KycService kycService) {
        this.kycRepo = kycRepo;
        this.kycService = kycService;
    }

    @GetMapping("/queue")
    public List<KycRequest> queue(@RequestParam(defaultValue = "SUBMITTED") String status) {
        return kycRepo.findByStatusOrderByCreatedAtDesc(status);
    }

    // TEMP: pass internalUserId (later extract from JWT claims)
    @PostMapping("/{kycId}/verify")
	public KycRequest verify(@PathVariable Long kycId) {
	    org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	    String subject = (String) auth.getPrincipal(); // "internal:2"
	    Long internalUserId = Long.parseLong(subject.split(":")[1]);
	    return kycService.verifyKyc(kycId, internalUserId);
	}


    @PostMapping("/{kycId}/reject/{internalUserId}")
    public KycRequest reject(@PathVariable Long kycId,
                             @PathVariable Long internalUserId,
                             @Valid @RequestBody KycRejectRequest req) {
        return kycService.rejectKyc(kycId, internalUserId, req.getReason());
    }
}