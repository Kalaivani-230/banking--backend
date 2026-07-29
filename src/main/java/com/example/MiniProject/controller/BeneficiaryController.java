package com.example.MiniProject.controller;

import com.example.MiniProject.entity.Beneficiary;
import com.example.MiniProject.repository.BeneficiaryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/customer/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryRepository repo;

    public BeneficiaryController(BeneficiaryRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/{customerId}")
    public List<Beneficiary> list(@PathVariable Long customerId) {
        return repo.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @PostMapping("/{customerId}")
    public Beneficiary add(@PathVariable Long customerId,
                           @RequestBody Map<String, String> body) {
        String nickName  = body.get("nickName");
        String fullName  = body.get("fullName");
        String accountNo = body.get("accountNo");

        if (nickName == null || nickName.isBlank())
            throw new RuntimeException("Nickname is required");
        if (fullName == null || fullName.isBlank())
            throw new RuntimeException("Full name is required");
        if (accountNo == null || accountNo.isBlank())
            throw new RuntimeException("Account number is required");

        Beneficiary b = new Beneficiary();
        b.setCustomerId(customerId);
        b.setNickName(nickName.trim());
        b.setFullName(fullName.trim());
        b.setAccountNo(accountNo.trim());
        b.setBankName(body.getOrDefault("bankName", ""));
        b.setSwiftCode(body.getOrDefault("swiftCode", ""));
        b.setIfscCode(body.getOrDefault("ifscCode", ""));
        b.setCountryCode(body.getOrDefault("countryCode", ""));
        b.setCurrencyCode(body.getOrDefault("currencyCode", ""));
        return repo.save(b);
    }

    @DeleteMapping("/{customerId}/{beneficiaryId}")
    public ResponseEntity<Void> delete(@PathVariable Long customerId,
                                       @PathVariable Long beneficiaryId) {
        Beneficiary b = repo.findById(beneficiaryId)
                .orElseThrow(() -> new RuntimeException("Beneficiary not found"));
        if (!b.getCustomerId().equals(customerId))
            throw new RuntimeException("Unauthorized");
        repo.delete(b);
        return ResponseEntity.noContent().build();
    }
}
