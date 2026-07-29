package com.example.MiniProject.controller;

import com.example.MiniProject.entity.Approval;
import com.example.MiniProject.entity.FeeComponent;
import com.example.MiniProject.entity.FeePolicy;
import com.example.MiniProject.repository.ApprovalRepository;
import com.example.MiniProject.repository.FeeComponentRepository;
import com.example.MiniProject.repository.FeePolicyRepository;
import com.example.MiniProject.service.FeePolicyService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/compliance/fee-policies")
public class ComplianceFeeApprovalController {

    private final FeePolicyService service;
    private final FeePolicyRepository policyRepo;
    private final ApprovalRepository approvalRepo;
    private final FeeComponentRepository componentRepo;

    public ComplianceFeeApprovalController(FeePolicyService service,
                                           FeePolicyRepository policyRepo,
                                           ApprovalRepository approvalRepo,
                                           FeeComponentRepository componentRepo) {
        this.service = service;
        this.policyRepo = policyRepo;
        this.approvalRepo = approvalRepo;
        this.componentRepo = componentRepo;
    }

    // Pending approval queue
    @GetMapping("/pending")
    public List<Map<String, Object>> pending() {
        try {
            return policyRepo.findByStatusOrderByUpdatedAtDesc("PENDING_APPROVAL")
                    .stream().map(this::enrich).toList();
        } catch (Exception e) {
            // Return raw list if enrich fails for any policy
            return policyRepo.findByStatusOrderByUpdatedAtDesc("PENDING_APPROVAL")
                    .stream().map(p -> {
                        try { return enrich(p); }
                        catch (Exception ex) {
                            java.util.Map<String, Object> m = new java.util.HashMap<>();
                            m.put("id", p.getId());
                            m.put("policyName", p.getPolicyName());
                            m.put("status", p.getStatus());
                            m.put("sendCurrency", p.getSendCurrency());
                            m.put("receiveCurrency", p.getReceiveCurrency());
                            m.put("channelCode", p.getChannelCode());
                            m.put("customerTypeCode", p.getCustomerTypeCode());
                            m.put("corridorId", p.getCorridorId());
                            m.put("priority", p.getPriority());
                            m.put("effectiveFrom", p.getEffectiveFrom());
                            m.put("components", componentRepo.findByPolicyId(p.getId()));
                            return m;
                        }
                    }).toList();
        }
    }

    // All policies (for full view)
    @GetMapping
    public List<FeePolicy> all() {
        return policyRepo.findAllByOrderByUpdatedAtDesc();
    }

    // Get single policy with components
    @GetMapping("/{policyId}")
    public Map<String, Object> get(@PathVariable Long policyId) {
        FeePolicy p = policyRepo.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found"));
        return enrich(p);
    }

    // Approve
    @PostMapping("/{policyId}/approve")
    public FeePolicy approve(@PathVariable Long policyId) {
        Long approverId = getCallerId();
        return service.approve(policyId, approverId);
    }

    // Reject
    @PostMapping("/{policyId}/reject")
    public FeePolicy reject(@PathVariable Long policyId,
                            @RequestBody Map<String, String> body) {
        Long approverId = getCallerId();
        return service.reject(policyId, approverId, body.get("reason"));
    }

    // Deactivate active policy
    @PostMapping("/{policyId}/deactivate")
    public FeePolicy deactivate(@PathVariable Long policyId,
                                @RequestBody Map<String, String> body) {
        Long approverId = getCallerId();
        return service.deactivate(policyId, approverId, body.get("reason"));
    }

    // Approval history for a policy
    @GetMapping("/{policyId}/history")
    public List<Approval> history(@PathVariable Long policyId) {
        return approvalRepo.findByEntityTypeAndStatusOrderBySubmittedAtDesc("FEE_POLICY", "COMPLETED")
                .stream().filter(a -> a.getEntityId().equals(policyId)).toList();
    }

    private Long getCallerId() {
        String subject = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Long.parseLong(subject.split(":")[1]);
    }

    private Map<String, Object> enrich(FeePolicy p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("policyName", p.getPolicyName());
        map.put("corridorId", p.getCorridorId());
        map.put("sendCurrency", p.getSendCurrency());
        map.put("receiveCurrency", p.getReceiveCurrency());
        map.put("channelCode", p.getChannelCode());
        map.put("customerTypeCode", p.getCustomerTypeCode());
        map.put("amountMin", p.getAmountMin());
        map.put("amountMax", p.getAmountMax());
        map.put("priority", p.getPriority());
        map.put("status", p.getStatus());
        map.put("versionNumber", p.getVersionNumber());
        map.put("effectiveFrom", p.getEffectiveFrom());
        map.put("effectiveTo", p.getEffectiveTo());
        map.put("createdBy", p.getCreatedBy());
        map.put("createdAt", p.getCreatedAt());
        map.put("updatedAt", p.getUpdatedAt());

        List<FeeComponent> components = componentRepo.findByPolicyId(p.getId());
        map.put("components", components);

        // Latest approval record
        approvalRepo.findTopByEntityIdAndEntityTypeAndStatusOrderBySubmittedAtDesc(
                p.getId(), "FEE_POLICY", "PENDING").ifPresent(a ->
                map.put("pendingApproval", a));

        return map;
    }
}
