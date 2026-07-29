package com.example.MiniProject.controller;

import com.example.MiniProject.entity.Account;
import com.example.MiniProject.entity.CustomerLimit;
import com.example.MiniProject.repository.AuditLogRepository;
import com.example.MiniProject.service.AccountService;
import com.example.MiniProject.service.LimitService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/customer/account")
public class CustomerAccountController {

    private final AccountService accountService;
    private final LimitService limitService;
    private final AuditLogRepository auditLogRepo;

    public CustomerAccountController(AccountService accountService,
                                     LimitService limitService,
                                     AuditLogRepository auditLogRepo) {
        this.accountService = accountService;
        this.limitService = limitService;
        this.auditLogRepo = auditLogRepo;
    }

    @GetMapping("/{customerId}")
    public Account getAccount(@PathVariable Long customerId) {
        return accountService.getCustomerAccount(customerId);
    }

    @GetMapping("/{customerId}/limits")
    public CustomerLimit getCustomerLimit(@PathVariable Long customerId) {
        return limitService.getLatestCustomerLimit(customerId);
    }

    // BUG-019: Teller transactions for account statement
    @GetMapping("/{customerId}/teller-transactions")
    public List<Map<String, Object>> getTellerTransactions(@PathVariable Long customerId) {
        Account account = accountService.getCustomerAccount(customerId);
        return auditLogRepo.findAll().stream()
                .filter(log -> ("CASH_DEPOSIT".equals(log.getAction()) || "CASH_WITHDRAWAL".equals(log.getAction()))
                        && "ACCOUNT".equals(log.getEntityType())
                        && account.getAccountNo().equals(log.getEntityId()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(log -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("type", log.getAction());
                    entry.put("description", log.getDescription() != null ? log.getDescription() : "");
                    entry.put("performedBy", "Bank Staff");
                    entry.put("createdAt", log.getCreatedAt().toString());
                    // Parse amount from description e.g. "Deposited 5000.00 to ACCT100001"
                    String desc = log.getDescription() != null ? log.getDescription() : "";
                    String[] parts = desc.split(" ");
                    String amount = "";
                    if (parts.length >= 2) {
                        try { new java.math.BigDecimal(parts[1]); amount = parts[1]; } catch (Exception ignored) {}
                    }
                    entry.put("amount", amount);
                    return entry;
                })
                .collect(Collectors.toList());
    }
}
