package com.example.MiniProject.controller;

import com.example.MiniProject.dto.InternalUserRequest;
import com.example.MiniProject.entity.InternalUser;
import com.example.MiniProject.repository.CustomerRepository;
import com.example.MiniProject.repository.DomesticTransactionRepository;
import com.example.MiniProject.repository.InternationalTransactionRepository;
import com.example.MiniProject.repository.InternalUserRepository;
import com.example.MiniProject.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService userService;
    private final InternalUserRepository userRepo;
    private final CustomerRepository customerRepo;
    private final DomesticTransactionRepository txnRepo;
    private final InternationalTransactionRepository intlTxnRepo;

    public AdminUserController(AdminUserService userService,
                               InternalUserRepository userRepo,
                               CustomerRepository customerRepo,
                               DomesticTransactionRepository txnRepo,
                               InternationalTransactionRepository intlTxnRepo) {
        this.userService = userService;
        this.userRepo = userRepo;
        this.customerRepo = customerRepo;
        this.txnRepo = txnRepo;
        this.intlTxnRepo = intlTxnRepo;
    }

    // ── KPI Dashboard ──────────────────────────────────────────
    @GetMapping("/kpi")
    public Map<String, Object> kpi() {
        long domTotal     = txnRepo.count();
        long domCompleted = txnRepo.findAll().stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
        long domFailed    = txnRepo.findAll().stream().filter(t -> "FAILED".equals(t.getStatus())).count();

        long intlTotal     = intlTxnRepo.count();
        long intlCompleted = intlTxnRepo.findAll().stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
        long intlFailed    = intlTxnRepo.findAll().stream().filter(t -> "FAILED".equals(t.getStatus())).count();

        long totalCustomers = customerRepo.count();
        long activeUsers    = userRepo.findAll().stream().filter(u -> "ACTIVE".equals(u.getStatus())).count();

        return Map.of(
            "totalTransactions",     domTotal + intlTotal,
            "completedTransactions", domCompleted + intlCompleted,
            "failedTransactions",    domFailed + intlFailed,
            "totalCustomers",        totalCustomers,
            "activeInternalUsers",   activeUsers
        );
    }

    // ── User Management ────────────────────────────────────────
    @GetMapping
    public List<InternalUser> list() {
        return userService.listAll();
    }

    @PostMapping
    public InternalUser create(@Valid @RequestBody InternalUserRequest req) {
        return userService.create(req);
    }

    @PutMapping("/{userId}")
    public InternalUser update(@PathVariable Long userId,
                               @Valid @RequestBody InternalUserRequest req) {
        return userService.update(userId, req);
    }

    @PostMapping("/{userId}/activate")
    public InternalUser activate(@PathVariable Long userId,
                                 @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return userService.setStatus(userId, "ACTIVE", reason);
    }

    @PostMapping("/{userId}/deactivate")
    public InternalUser deactivate(@PathVariable Long userId,
                                   @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return userService.setStatus(userId, "INACTIVE", reason);
    }
}
