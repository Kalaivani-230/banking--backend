package com.example.MiniProject.service;

import com.example.MiniProject.dto.KycSubmitRequest;
import com.example.MiniProject.entity.Customer;
import com.example.MiniProject.entity.KycRequest;
import com.example.MiniProject.entity.Account;
import com.example.MiniProject.repository.AccountRepository;
import com.example.MiniProject.repository.CustomerRepository;
import com.example.MiniProject.repository.KycRequestRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class KycService {

    private final KycRequestRepository kycRepo;
    private final CustomerRepository customerRepo;


    private final AccountRepository accountRepo;

    public KycService(KycRequestRepository kycRepo,
                  		CustomerRepository customerRepo,
                  		AccountRepository accountRepo) {
    	this.kycRepo = kycRepo;
    	this.customerRepo = customerRepo;
    	this.accountRepo = accountRepo;
    }


    /** Customer submits KYC (creates a new record each time -> keeps history) */
    public KycRequest submitKyc(Long customerId, KycSubmitRequest req) {
        Customer c = customerRepo.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // Block re-submission if already SUBMITTED or VERIFIED
        KycRequest existing = kycRepo.findTopByCustomerIdOrderByCreatedAtDesc(customerId).orElse(null);
        if (existing != null && ("SUBMITTED".equals(existing.getStatus()) || "VERIFIED".equals(existing.getStatus()))) {
            throw new RuntimeException("KYC already " + existing.getStatus().toLowerCase() + ". No further submission required.");
        }

        // BUG-008: Duplicate PAN/Aadhaar detection
        String pan = req.getPan().trim().toUpperCase();
        String aadhaar = req.getAadhaar().trim();

        boolean panDuplicate = !kycRepo.findByPanAndCustomerIdNot(pan, customerId).isEmpty();
        if (panDuplicate)
            throw new RuntimeException("This PAN number is already registered with another account.");

        boolean aadhaarDuplicate = !kycRepo.findByAadhaarAndCustomerIdNot(aadhaar, customerId).isEmpty();
        if (aadhaarDuplicate)
            throw new RuntimeException("This Aadhaar number is already registered with another account.");

        KycRequest kyc = new KycRequest();
        kyc.setCustomerId(customerId);
        kyc.setPan(pan);
        kyc.setAadhaar(aadhaar);
        kyc.setStatus("SUBMITTED");
        kyc.setSubmittedAt(Instant.now());

        return kycRepo.save(kyc);
    }

    /** Customer views latest KYC status */
    public KycRequest getLatestKyc(Long customerId) {
        return kycRepo.findTopByCustomerIdOrderByCreatedAtDesc(customerId).orElse(null);
    }

    /** Compliance verifies KYC */

	public KycRequest verifyKyc(Long kycId, Long internalUserId) {
	    KycRequest kyc = kycRepo.findById(kycId)
	            .orElseThrow(() -> new RuntimeException("KYC record not found"));
	
	    // 1) Verify KYC
	    kyc.setStatus("VERIFIED");
	    kyc.setRejectedReason(null);
	    kyc.setVerifiedBy(internalUserId);
	    kyc.setVerifiedAt(Instant.now());
	    KycRequest savedKyc = kycRepo.save(kyc);
	
	    
	    // 2) Create account if not exists (one account per customer)
	    Long customerId = kyc.getCustomerId();
	    
	    Customer cust = customerRepo.findById(customerId)
	            .orElseThrow(() -> new RuntimeException("Customer not found"));

	    String acctType = cust.getPreferredAccountType();
	    if (acctType == null || acctType.isBlank()) acctType = "SAVINGS";
	    acctType = acctType.trim().toUpperCase();

	    if ("CURRENT".equals(acctType) && !"CORPORATE".equalsIgnoreCase(cust.getCustomerTypeCode())) {
	        acctType = "SAVINGS"; // fallback
	    }

	    
	    
	    
	    if (!accountRepo.existsByCustomerId(customerId)) {
	    	Account a = new Account();
		    a.setCustomerId(customerId);
		    a.setAccountNo("ACCT" + (100000 + customerId));
		    a.setAccountType(acctType);        // ✅ from customer preference
		    a.setCurrencyCode("INR");
		    a.setCountryCode("IN");
		    a.setStatus("ACTIVE");
		    a.setLedgerBalance(BigDecimal.ZERO);
		    a.setAvailableBalance(BigDecimal.ZERO);

		    accountRepo.save(a);
	    }
	
	    return savedKyc;
	}


    /** Compliance rejects KYC */
    public KycRequest rejectKyc(Long kycId, Long internalUserId, String reason) {
        KycRequest kyc = kycRepo.findById(kycId)
                .orElseThrow(() -> new RuntimeException("KYC record not found"));

        kyc.setStatus("REJECTED");
        kyc.setRejectedReason(reason);
        kyc.setVerifiedBy(internalUserId);
        kyc.setVerifiedAt(Instant.now());

        return kycRepo.save(kyc);
    }
}