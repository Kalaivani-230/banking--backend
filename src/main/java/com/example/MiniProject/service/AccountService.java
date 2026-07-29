package com.example.MiniProject.service;

import com.example.MiniProject.dto.CreateAccountRequest;
import com.example.MiniProject.entity.Account;
import com.example.MiniProject.repository.AccountRepository;
import com.example.MiniProject.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountService {

    private final AccountRepository accountRepo;
    private final CustomerRepository customerRepo;

    public AccountService(AccountRepository accountRepo, CustomerRepository customerRepo) {
        this.accountRepo = accountRepo;
        this.customerRepo = customerRepo;
    }

    public Account createAccount(CreateAccountRequest req) {

        if (!customerRepo.existsById(req.getCustomerId())) {
            throw new RuntimeException("Customer not found");
        }

        // ✅ Enforce one account per customer (your Q1=B and Q2=A)
        if (accountRepo.existsByCustomerId(req.getCustomerId())) {
            throw new RuntimeException("Account already exists for this customer");
        }

        Account a = new Account();
        a.setCustomerId(req.getCustomerId());
        a.setAccountNo(req.getAccountNo());
        a.setAccountType(req.getAccountType());
        a.setCurrencyCode(req.getCurrencyCode());
        a.setCountryCode(req.getCountryCode());
        a.setStatus("ACTIVE");

        BigDecimal bal = req.getOpeningBalance() == null ? BigDecimal.ZERO : req.getOpeningBalance();
        a.setLedgerBalance(bal);
        a.setAvailableBalance(bal);

        return accountRepo.save(a);
    }

    public Account getCustomerAccount(Long customerId) {
        return accountRepo.findByCustomerId(customerId)
                .orElseThrow(() -> new RuntimeException("No account found for customer"));
    }
}