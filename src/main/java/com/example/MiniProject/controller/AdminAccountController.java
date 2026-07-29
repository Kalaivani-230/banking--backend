package com.example.MiniProject.controller;

import com.example.MiniProject.dto.CreateAccountRequest;
import com.example.MiniProject.dto.LimitRequest;
import com.example.MiniProject.entity.Account;
import com.example.MiniProject.entity.AccountLimit;
import com.example.MiniProject.entity.Customer;
import com.example.MiniProject.entity.CustomerLimit;
import com.example.MiniProject.entity.InternationalTransaction;
import com.example.MiniProject.repository.AccountRepository;
import com.example.MiniProject.repository.CustomerRepository;
import com.example.MiniProject.repository.InternationalTransactionRepository;
import com.example.MiniProject.service.AccountService;
import com.example.MiniProject.service.LimitService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/accounts")
public class AdminAccountController {

    private final AccountRepository accountRepo;
    private final CustomerRepository customerRepo;
    private final AccountService accountService;
    private final LimitService limitService;
    private final InternationalTransactionRepository intlTxnRepo;

    public AdminAccountController(AccountRepository accountRepo,
                                  CustomerRepository customerRepo,
                                  AccountService accountService,
                                  LimitService limitService,
                                  InternationalTransactionRepository intlTxnRepo) {
        this.accountRepo = accountRepo;
        this.customerRepo = customerRepo;
        this.accountService = accountService;
        this.limitService = limitService;
        this.intlTxnRepo = intlTxnRepo;
    }

    // ── Original endpoints ─────────────────────────────────────

    @PostMapping
    public Account create(@RequestBody CreateAccountRequest req) {
        return accountService.createAccount(req);
    }

    @PostMapping("/{customerId}/customer-limits/{internalUserId}")
    public CustomerLimit setCustomerLimits(@PathVariable Long customerId,
                                           @PathVariable Long internalUserId,
                                           @RequestBody LimitRequest req) {
        return limitService.setCustomerLimits(customerId, internalUserId, req);
    }

    @PostMapping("/{accountId}/account-limits/{internalUserId}")
    public AccountLimit setAccountLimits(@PathVariable Long accountId,
                                         @PathVariable Long internalUserId,
                                         @RequestBody LimitRequest req) {
        return limitService.setAccountLimits(accountId, internalUserId, req);
    }

    // ── Search & Management ────────────────────────────────────

    @GetMapping("/search")
    public List<Map<String, Object>> search(
            @RequestParam(required = false) String accountNo,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String customerName) {

        List<Account> accounts;

        if (accountNo != null && !accountNo.isBlank()) {
            accounts = accountRepo.findByAccountNo(accountNo.trim())
                    .map(List::of).orElse(List.of());
        } else if (customerId != null && !customerId.isBlank()) {
            try {
                accounts = accountRepo.findByCustomerId(Long.parseLong(customerId.trim()))
                        .map(List::of).orElse(List.of());
            } catch (NumberFormatException e) {
                accounts = List.of();
            }
        } else if (customerName != null && !customerName.isBlank()) {
            List<Customer> customers = customerRepo.findByFullNameContainingIgnoreCase(customerName.trim());
            accounts = new ArrayList<>();
            for (Customer c : customers) {
                accountRepo.findByCustomerId(c.getId()).ifPresent(accounts::add);
            }
        } else {
            accounts = accountRepo.findAll();
        }

        return accounts.stream().map(this::enrichAccount).toList();
    }

    @GetMapping("/{accountId}/detail")
    public Map<String, Object> getAccountDetail(@PathVariable Long accountId) {
        Account a = accountRepo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return enrichAccount(a);
    }

    @PostMapping("/{accountId}/freeze")
    public Map<String, Object> freeze(@PathVariable Long accountId,
                                      @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        if (reason == null || reason.isBlank())
            throw new RuntimeException("Reason is required to freeze an account");
        Account a = accountRepo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        a.setStatus("FROZEN");
        accountRepo.save(a);
        System.out.println("Account " + a.getAccountNo() + " FROZEN. Reason: " + reason);
        return enrichAccount(a);
    }

    @PostMapping("/{accountId}/unfreeze")
    public Map<String, Object> unfreeze(@PathVariable Long accountId,
                                        @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        if (reason == null || reason.isBlank())
            throw new RuntimeException("Reason is required to unfreeze an account");
        Account a = accountRepo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        a.setStatus("ACTIVE");
        accountRepo.save(a);
        System.out.println("Account " + a.getAccountNo() + " UNFROZEN. Reason: " + reason);
        return enrichAccount(a);
    }

    @PostMapping("/{accountId}/limits")
    public AccountLimit setLimits(@PathVariable Long accountId,
                                  @RequestBody LimitRequest req) {
        String subject = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long internalUserId = Long.parseLong(subject.split(":")[1]);

        if (req.getPerTxnLimit() != null && req.getPerTxnLimit().signum() < 0)
            throw new RuntimeException("Per-transaction limit cannot be negative");
        if (req.getDailyLimit() != null && req.getDailyLimit().signum() < 0)
            throw new RuntimeException("Daily limit cannot be negative");
        if (req.getMonthlyLimit() != null && req.getMonthlyLimit().signum() < 0)
            throw new RuntimeException("Monthly limit cannot be negative");
        if (req.getEffectiveFrom() == null) req.setEffectiveFrom(Instant.now());

        return limitService.setAccountLimits(accountId, internalUserId, req);
    }

    @GetMapping("/{accountId}/limits")
    public AccountLimit getLimits(@PathVariable Long accountId) {
        return limitService.getLatestAccountLimit(accountId);
    }

    // ── Flagged Transactions (AML auto-escalated to Admin) ────────────────

    @GetMapping("/flagged-transactions")
    public List<Map<String, Object>> getFlaggedTransactions() {
        return intlTxnRepo
                .findByAdminReviewedFalseAndAdminFlagReasonIsNotNullOrderByCreatedAtDesc()
                .stream().map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("referenceId",     t.getReferenceId());
                    m.put("customerId",      t.getCustomerId());
                    m.put("fromAccountNo",   t.getFromAccountNo());
                    m.put("sendAmount",      t.getSendAmount());
                    m.put("fromCurrency",    t.getFromCurrency());
                    m.put("toCurrency",      t.getToCurrency());
                    m.put("status",          t.getStatus());
                    m.put("amlRiskLevel",    t.getAmlRiskLevel());
                    m.put("amlRiskScore",    t.getAmlRiskScore());
                    m.put("amlFlags",        t.getAmlFlags());
                    m.put("adminFlagReason", t.getAdminFlagReason());
                    m.put("failureReason",   t.getFailureReason());
                    m.put("createdAt",       t.getCreatedAt());
                    customerRepo.findById(t.getCustomerId()).ifPresent(c -> {
                        m.put("customerName",  c.getFullName());
                        m.put("customerEmail", c.getEmail());
                    });
                    accountRepo.findByAccountNo(t.getFromAccountNo())
                            .ifPresent(a -> m.put("accountId", a.getId()));
                    return m;
                }).toList();
    }

    @PostMapping("/flagged-transactions/{referenceId}/mark-reviewed")
    public Map<String, String> markReviewed(@PathVariable String referenceId) {
        intlTxnRepo.findByReferenceId(referenceId).ifPresent(t -> {
            t.setAdminReviewed(true);
            intlTxnRepo.save(t);
        });
        return Map.of("message", "Marked as reviewed.");
    }

    // ── Helper ─────────────────────────────────────────────────

    private Map<String, Object> enrichAccount(Account a) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", a.getId());
        map.put("accountNo", a.getAccountNo());
        map.put("accountType", a.getAccountType());
        map.put("currencyCode", a.getCurrencyCode());
        map.put("status", a.getStatus());
        map.put("ledgerBalance", a.getLedgerBalance());
        map.put("availableBalance", a.getAvailableBalance());
        map.put("customerId", a.getCustomerId());
        map.put("createdAt", a.getCreatedAt());

        customerRepo.findById(a.getCustomerId()).ifPresent(c -> {
            map.put("customerName", c.getFullName());
            map.put("customerEmail", c.getEmail());
            map.put("customerMobile", c.getMobile());
            map.put("customerCode", c.getCustomerCode());
        });

        AccountLimit limit = limitService.getLatestAccountLimit(a.getId());
        if (limit != null) {
            map.put("perTxnLimit", limit.getPerTxnLimit());
            map.put("dailyLimit", limit.getDailyLimit());
            map.put("monthlyLimit", limit.getMonthlyLimit());
        }
        return map;
    }
}
