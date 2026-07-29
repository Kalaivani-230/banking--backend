package com.example.MiniProject.service;

import com.example.MiniProject.dto.FeeComponentRequest;
import com.example.MiniProject.dto.FeePolicyRequest;
import com.example.MiniProject.entity.Approval;
import com.example.MiniProject.entity.FeeComponent;
import com.example.MiniProject.entity.FeePolicy;
import com.example.MiniProject.repository.ApprovalRepository;
import com.example.MiniProject.repository.FeeComponentRepository;
import com.example.MiniProject.repository.FeePolicyRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class FeePolicyService {

    private final FeePolicyRepository policyRepo;
    private final FeeComponentRepository componentRepo;
    private final ApprovalRepository approvalRepo;

    public FeePolicyService(FeePolicyRepository policyRepo,
                            FeeComponentRepository componentRepo,
                            ApprovalRepository approvalRepo) {
        this.policyRepo = policyRepo;
        this.componentRepo = componentRepo;
        this.approvalRepo = approvalRepo;
    }

    public FeePolicy createDraft(FeePolicyRequest req, Long createdBy) {
        FeePolicy p = new FeePolicy();
        mapPolicy(req, p);
        p.setStatus("DRAFT");
        p.setCreatedBy(createdBy);

        FeePolicy saved = policyRepo.save(p);
        saveComponents(saved.getId(), req);

        return saved;
    }

    public FeePolicy updateDraft(Long policyId, FeePolicyRequest req) {
        FeePolicy p = policyRepo.findById(policyId)
                .orElseThrow(() -> new RuntimeException("FeePolicy not found"));

        // BUG-011: Allow update for DRAFT and PENDING_APPROVAL; block once ACTIVE/APPROVED
        if (!"DRAFT".equalsIgnoreCase(p.getStatus()) && !"PENDING_APPROVAL".equalsIgnoreCase(p.getStatus())) {
            throw new RuntimeException("Policy can only be updated when in DRAFT or PENDING_APPROVAL status");
        }

        mapPolicy(req, p);
        FeePolicy saved = policyRepo.save(p);

        // Replace components completely
        componentRepo.deleteByPolicyId(saved.getId());
        saveComponents(saved.getId(), req);

        return saved;
    }

    public void submitForApproval(Long policyId, Long submittedBy, String reason) {
        FeePolicy p = policyRepo.findById(policyId)
                .orElseThrow(() -> new RuntimeException("FeePolicy not found"));

        if (!"DRAFT".equalsIgnoreCase(p.getStatus())) {
            throw new RuntimeException("Only DRAFT policy can be submitted");
        }

        p.setStatus("PENDING_APPROVAL");
        policyRepo.save(p);

        Approval a = new Approval();
        a.setEntityType("FEE_POLICY");
        a.setEntityId(policyId);
        a.setSubmittedBy(submittedBy);
        a.setSubmittedAt(Instant.now());
        a.setStatus("PENDING");
        a.setDecisionReason(reason);

        approvalRepo.save(a);
    }

    public FeePolicy approve(Long policyId, Long approverId) {
        FeePolicy p = policyRepo.findById(policyId)
                .orElseThrow(() -> new RuntimeException("FeePolicy not found"));

        if (!"PENDING_APPROVAL".equalsIgnoreCase(p.getStatus()))
            throw new RuntimeException("Policy is not pending approval");

        // Conflict check — only expire ACTIVE policies with the exact same
        // corridor + channel + customerType + currency pair + priority
        // (different slabs or different priorities can coexist)
        List<FeePolicy> conflicts = policyRepo.findByCorridorIdAndChannelCodeAndCustomerTypeCodeAndStatus(
                p.getCorridorId(), p.getChannelCode(), p.getCustomerTypeCode(), "ACTIVE");

        for (FeePolicy conflict : conflicts) {
            if (conflict.getId().equals(policyId)) continue;
            // Only expire if same currency pair AND same priority (true duplicate)
            if (conflict.getPriority().equals(p.getPriority())
                    && conflict.getSendCurrency().equalsIgnoreCase(p.getSendCurrency())
                    && conflict.getReceiveCurrency().equalsIgnoreCase(p.getReceiveCurrency())) {
                conflict.setStatus("EXPIRED");
                policyRepo.save(conflict);
            }
        }

        // Activate
        String newStatus = (p.getEffectiveFrom() == null || !Instant.now().isBefore(p.getEffectiveFrom()))
                ? "ACTIVE" : "SCHEDULED";
        p.setStatus(newStatus);
        FeePolicy saved = policyRepo.save(p);

        approvalRepo.findTopByEntityIdAndEntityTypeAndStatusOrderBySubmittedAtDesc(
                policyId, "FEE_POLICY", "PENDING").ifPresent(a -> {
            a.setApproverId(approverId);
            a.setDecision("APPROVED");
            a.setDecidedAt(Instant.now());
            a.setStatus("COMPLETED");
            approvalRepo.save(a);
        });

        return saved;
    }

    public FeePolicy reject(Long policyId, Long approverId, String reason) {
        if (reason == null || reason.isBlank())
            throw new RuntimeException("Rejection reason is mandatory");

        FeePolicy p = policyRepo.findById(policyId)
                .orElseThrow(() -> new RuntimeException("FeePolicy not found"));

        if (!"PENDING_APPROVAL".equalsIgnoreCase(p.getStatus()))
            throw new RuntimeException("Policy is not pending approval");

        p.setStatus("REJECTED");
        FeePolicy saved = policyRepo.save(p);

        approvalRepo.findTopByEntityIdAndEntityTypeAndStatusOrderBySubmittedAtDesc(
                policyId, "FEE_POLICY", "PENDING").ifPresent(a -> {
            a.setApproverId(approverId);
            a.setDecision("REJECTED");
            a.setDecisionReason(reason);
            a.setDecidedAt(Instant.now());
            a.setStatus("COMPLETED");
            approvalRepo.save(a);
        });

        return saved;
    }

    public FeePolicy deactivate(Long policyId, Long approverId, String reason) {
        if (reason == null || reason.isBlank())
            throw new RuntimeException("Deactivation reason is mandatory");

        FeePolicy p = policyRepo.findById(policyId)
                .orElseThrow(() -> new RuntimeException("FeePolicy not found"));

        if (!"ACTIVE".equalsIgnoreCase(p.getStatus()) && !"SCHEDULED".equalsIgnoreCase(p.getStatus()))
            throw new RuntimeException("Only ACTIVE or SCHEDULED policies can be deactivated");

        p.setStatus("DEACTIVATED");
        System.out.println("Policy " + policyId + " deactivated by " + approverId + ". Reason: " + reason);
        return policyRepo.save(p);
    }

    private void mapPolicy(FeePolicyRequest req, FeePolicy p) {
        p.setPolicyName(req.policyName);
        p.setCorridorId(req.corridorId);
        p.setSendCurrency(req.sendCurrency);
        p.setReceiveCurrency(req.receiveCurrency);
        p.setChannelCode(req.channelCode);
        p.setCustomerTypeCode(req.customerTypeCode);
        p.setAmountMin(req.amountMin);
        p.setAmountMax(req.amountMax);
        p.setPriority(req.priority);
        p.setEffectiveFrom(req.effectiveFrom);
        p.setEffectiveTo(req.effectiveTo);
    }

    private void saveComponents(Long policyId, FeePolicyRequest req) {
        if (req.components == null) return;

        for (FeeComponentRequest cReq : req.components) {
            FeeComponent c = new FeeComponent();
            c.setPolicyId(policyId);
            c.setComponentType(cReq.componentType);
            c.setCalcType(cReq.calcType);
            c.setValue(cReq.value);
            c.setMinFee(cReq.minFee);
            c.setMaxFee(cReq.maxFee);
            c.setIsEstimated(cReq.isEstimated != null ? cReq.isEstimated : false);
            componentRepo.save(c);
        }
    }
}