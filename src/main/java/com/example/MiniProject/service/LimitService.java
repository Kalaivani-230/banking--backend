package com.example.MiniProject.service;

import com.example.MiniProject.dto.LimitRequest;
import com.example.MiniProject.entity.Account;
import com.example.MiniProject.entity.AccountLimit;
import com.example.MiniProject.entity.CustomerLimit;
import com.example.MiniProject.repository.AccountLimitRepository;
import com.example.MiniProject.repository.AccountRepository;
import com.example.MiniProject.repository.CustomerLimitRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class LimitService {

    private final CustomerLimitRepository customerLimitRepo;
    private final AccountLimitRepository accountLimitRepo;
    private final AccountRepository accountRepo;

    public LimitService(CustomerLimitRepository customerLimitRepo,
                        AccountLimitRepository accountLimitRepo,
                        AccountRepository accountRepo) {
        this.customerLimitRepo = customerLimitRepo;
        this.accountLimitRepo = accountLimitRepo;
        this.accountRepo = accountRepo;
    }

    public CustomerLimit setCustomerLimits(Long customerId, Long internalUserId, LimitRequest req) {
        CustomerLimit cl = new CustomerLimit();
        cl.setCustomerId(customerId);
        cl.setPerTxnLimit(req.getPerTxnLimit());
        cl.setDailyLimit(req.getDailyLimit());
        cl.setMonthlyLimit(req.getMonthlyLimit());
        cl.setEffectiveFrom(req.getEffectiveFrom() == null ? Instant.now() : req.getEffectiveFrom());
        cl.setEffectiveTo(req.getEffectiveTo());
        cl.setUpdatedBy(internalUserId);
        return customerLimitRepo.save(cl);
    }

    public AccountLimit setAccountLimits(Long accountId, Long internalUserId, LimitRequest req) {
        Account a = accountRepo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        AccountLimit al = new AccountLimit();
        al.setAccountId(a.getId());
        al.setPerTxnLimit(req.getPerTxnLimit());
        al.setDailyLimit(req.getDailyLimit());
        al.setMonthlyLimit(req.getMonthlyLimit());
        al.setEffectiveFrom(req.getEffectiveFrom() == null ? Instant.now() : req.getEffectiveFrom());
        al.setEffectiveTo(req.getEffectiveTo());
        al.setUpdatedBy(internalUserId);
        return accountLimitRepo.save(al);
    }

    public CustomerLimit getLatestCustomerLimit(Long customerId) {
        return customerLimitRepo.findTopByCustomerIdOrderByEffectiveFromDesc(customerId).orElse(null);
    }

    public AccountLimit getLatestAccountLimit(Long accountId) {
        return accountLimitRepo.findTopByAccountIdOrderByEffectiveFromDesc(accountId).orElse(null);
    }
    public void validatePerTxnLimit(Long customerId, Long accountId, java.math.BigDecimal amount) {
        CustomerLimit cl = getLatestCustomerLimit(customerId);
        AccountLimit al = getLatestAccountLimit(accountId);

        java.math.BigDecimal perTxn = null;

        // account limit overrides customer if exists
        if (al != null) perTxn = al.getPerTxnLimit();
        else if (cl != null) perTxn = cl.getPerTxnLimit();

        if (perTxn != null && amount.compareTo(perTxn) > 0) {
            throw new RuntimeException("Per transaction limit exceeded");
        }
    }

}