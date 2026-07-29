package com.example.MiniProject.controller;

import com.example.MiniProject.dto.FeePolicyRequest;
import com.example.MiniProject.dto.SubmitApprovalRequest;
import com.example.MiniProject.entity.FeePolicy;
import com.example.MiniProject.repository.FeeComponentRepository;
import com.example.MiniProject.repository.FeePolicyRepository;
import com.example.MiniProject.service.FeePolicyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/fee-policies")
public class AdminFeePolicyController {

    private final FeePolicyService service;
    private final FeePolicyRepository policyRepo;
    private final FeeComponentRepository componentRepo;

    public AdminFeePolicyController(FeePolicyService service,
                                    FeePolicyRepository policyRepo,
                                    FeeComponentRepository componentRepo) {
        this.service = service;
        this.policyRepo = policyRepo;
        this.componentRepo = componentRepo;
    }

    @PostMapping("/draft/{adminUserId}")
    public FeePolicy createDraft(@PathVariable Long adminUserId, @RequestBody FeePolicyRequest req) {
        return service.createDraft(req, adminUserId);
    }

    @PutMapping("/draft/{policyId}")
    public FeePolicy updateDraft(@PathVariable Long policyId, @RequestBody FeePolicyRequest req) {
        return service.updateDraft(policyId, req);
    }

    @GetMapping
    public List<FeePolicy> list() {
        return policyRepo.findAllByOrderByUpdatedAtDesc();
    }

    @GetMapping("/{policyId}")
    public FeePolicy get(@PathVariable Long policyId) {
        return policyRepo.findById(policyId).orElseThrow(() -> new RuntimeException("Not found"));
    }

    @GetMapping("/{policyId}/components")
    public Object components(@PathVariable Long policyId) {
        return componentRepo.findByPolicyId(policyId);
    }

    @PostMapping("/{policyId}/submit/{adminUserId}")
    public String submit(@PathVariable Long policyId,
                         @PathVariable Long adminUserId,
                         @RequestBody(required=false) SubmitApprovalRequest body) {
        String reason = body == null ? null : body.reason;
        service.submitForApproval(policyId, adminUserId, reason);
        return "Submitted for approval";
    }
}