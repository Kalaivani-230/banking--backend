package com.example.MiniProject.service;

import com.example.MiniProject.dto.CustomerProfileResponse;
import com.example.MiniProject.dto.UpdateContactRequest;
import com.example.MiniProject.dto.UpdateProfileRequest;
import com.example.MiniProject.entity.Customer;
import com.example.MiniProject.repository.CustomerRepository;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final CustomerRepository customerRepo;
    private final OtpService otpService;

    public ProfileService(CustomerRepository customerRepo, OtpService otpService) {
        this.customerRepo = customerRepo;
        this.otpService = otpService;
    }

    public Customer getCustomer(Long customerId) {
        return customerRepo.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    public CustomerProfileResponse getProfile(Long customerId) {
        return toResponse(getCustomer(customerId));
    }

    public CustomerProfileResponse updateProfile(Long customerId, UpdateProfileRequest req) {
        Customer c = getCustomer(customerId);
        c.setFullName(req.getFullName());
        if (req.getAddress() != null) c.setAddress(req.getAddress());
        return toResponse(customerRepo.save(c));
    }

    public void requestContactChangeOtp(Long customerId, String field, String newValue) {
        getCustomer(customerId); // ensure exists
        if ("EMAIL".equalsIgnoreCase(field)) {
            if (customerRepo.findByEmail(newValue).isPresent())
                throw new RuntimeException("Email already registered");
        } else if ("MOBILE".equalsIgnoreCase(field)) {
            if (customerRepo.findByMobile(newValue).isPresent())
                throw new RuntimeException("Mobile already registered");
        } else {
            throw new RuntimeException("Invalid field. Use EMAIL or MOBILE");
        }
        var otp = otpService.createOtp(customerId, "CONTACT_CHANGE");
        System.out.println("CONTACT_CHANGE OTP for customerId " + customerId + " = " + otp.getOtpCode());
    }

    public CustomerProfileResponse verifyAndUpdateContact(Long customerId, UpdateContactRequest req) {
        boolean ok = otpService.verifyOtp(customerId, "CONTACT_CHANGE", req.getOtpCode());
        if (!ok) throw new RuntimeException("Invalid or expired OTP");
        Customer c = getCustomer(customerId);
        if ("EMAIL".equalsIgnoreCase(req.getField())) {
            if (customerRepo.findByEmail(req.getNewValue()).isPresent())
                throw new RuntimeException("Email already registered");
            c.setEmail(req.getNewValue());
        } else if ("MOBILE".equalsIgnoreCase(req.getField())) {
            if (customerRepo.findByMobile(req.getNewValue()).isPresent())
                throw new RuntimeException("Mobile already registered");
            c.setMobile(req.getNewValue());
        } else {
            throw new RuntimeException("Invalid field. Use EMAIL or MOBILE");
        }
        return toResponse(customerRepo.save(c));
    }

    private CustomerProfileResponse toResponse(Customer c) {
        CustomerProfileResponse r = new CustomerProfileResponse();
        r.id = c.getId();
        r.customerCode = c.getCustomerCode();
        r.fullName = c.getFullName();
        r.email = c.getEmail();
        r.mobile = c.getMobile();
        r.address = c.getAddress();
        r.status = c.getStatus();
        r.customerTypeCode = c.getCustomerTypeCode();
        r.preferredAccountType = c.getPreferredAccountType();
        r.createdAt = c.getCreatedAt();
        return r;
    }
}
