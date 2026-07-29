package com.example.MiniProject.service;

import com.example.MiniProject.dto.*;
import com.example.MiniProject.entity.Customer;
import com.example.MiniProject.entity.CustomerLimit;
import com.example.MiniProject.entity.InternalUser;
import com.example.MiniProject.repository.CustomerLimitRepository;
import com.example.MiniProject.repository.CustomerRepository;
import com.example.MiniProject.repository.InternalUserRepository;
import com.example.MiniProject.security.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final CustomerRepository customerRepo;
    private final InternalUserRepository internalRepo;
    private final BCryptPasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final CustomerLimitRepository customerLimitRepo;

    public AuthService(CustomerRepository customerRepo,
                       InternalUserRepository internalRepo,
                       BCryptPasswordEncoder encoder,
                       JwtUtil jwtUtil,
                       OtpService otpService,
                       CustomerLimitRepository customerLimitRepo) {
        this.customerRepo = customerRepo;
        this.internalRepo = internalRepo;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
        this.customerLimitRepo = customerLimitRepo;
    }

    public String registerCustomer(RegisterRequest req) {
        if (customerRepo.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email already registered");
        if (customerRepo.existsByMobile(req.getMobile()))
            throw new RuntimeException("Mobile already registered");

        Customer c = new Customer();
        c.setCustomerCode("CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        c.setFullName(req.getFullName());
        c.setEmail(req.getEmail());
        c.setMobile(req.getMobile());
        c.setPasswordHash(encoder.encode(req.getPassword()));
        c.setStatus("ACTIVE");
        c.setCustomerTypeCode(req.getCustomerTypeCode());

        // ✅ NEW
        String preferredType = validatePreferredAccountType(req.getPreferredAccountType(), req.getCustomerTypeCode());
        c.setPreferredAccountType(preferredType);

        Customer saved = customerRepo.save(c);

        // Set default transaction limits for new customer
        CustomerLimit defaultLimit = new CustomerLimit();
        defaultLimit.setCustomerId(saved.getId());
        defaultLimit.setPerTxnLimit(new BigDecimal("100000"));
        defaultLimit.setDailyLimit(new BigDecimal("200000"));
        defaultLimit.setMonthlyLimit(new BigDecimal("1000000"));
        defaultLimit.setEffectiveFrom(Instant.now());
        customerLimitRepo.save(defaultLimit);

        // Create OTP for REGISTRATION
        var otp = otpService.createOtp(saved.getId(), "REGISTRATION");
        System.out.println("REGISTRATION OTP for customerId " + saved.getId() + " = " + otp.getOtpCode());

        // Return OTP in response for dev/demo mode
        return otp.getOtpCode();
    }

    public String resendRegistrationOtp(ForgotPasswordRequest req) {
        Customer c = findCustomerByIdentifier(req.getCustomerIdentifier());
        var otp = otpService.resendOtp(c.getId(), "REGISTRATION");
        System.out.println("RESEND REGISTRATION OTP for customerId " + c.getId() + " = " + otp.getOtpCode());
        return otp.getOtpCode();
    }

    public void verifyRegistrationOtp(OtpVerifyRequest req) {
        Customer c = findCustomerByIdentifier(req.getCustomerIdentifier());
        boolean ok = otpService.verifyOtp(c.getId(), req.getPurpose(), req.getOtpCode());
        if (!ok) throw new RuntimeException("Invalid or expired OTP");
    }

    public AuthResponse loginCustomer(LoginRequest req) {
        Customer c = findCustomerByIdentifier(req.getUsername());

        if ("LOCKED".equals(c.getStatus()))
            throw new RuntimeException("Account locked. Contact support.");

        if (!encoder.matches(req.getPassword(), c.getPasswordHash())) {
            int attempts = (c.getFailedLoginAttempts() == null ? 0 : c.getFailedLoginAttempts()) + 1;
            c.setFailedLoginAttempts(attempts);
            if (attempts >= 3) {
                c.setStatus("LOCKED");
                c.setLockedAt(java.time.Instant.now());
                customerRepo.save(c);
                throw new RuntimeException("Account locked. Contact support.");
            }
            customerRepo.save(c);
            throw new RuntimeException("Invalid username or password.");
        }

        // Reset on success
        c.setFailedLoginAttempts(0);
        customerRepo.save(c);

        String subject = "customer:" + c.getId();
        String token = jwtUtil.generateToken(subject, Map.of(
                "role", "CUSTOMER",
                "userType", "CUSTOMER",
                "customerId", c.getId()
        ));
        return new AuthResponse(token, "CUSTOMER", subject);
    }
    
    private String validatePreferredAccountType(String preferredType, String customerTypeCode) {
        String t = preferredType == null ? "" : preferredType.trim().toUpperCase();
        String ct = customerTypeCode == null ? "" : customerTypeCode.trim().toUpperCase();

        if (!t.equals("SAVINGS") && !t.equals("CURRENT")) {
            throw new RuntimeException("Invalid preferredAccountType. Allowed: SAVINGS or CURRENT");
        }

        // CURRENT only for CORPORATE
        if (t.equals("CURRENT") && !ct.equals("CORPORATE")) {
            throw new RuntimeException("CURRENT account is allowed only for CORPORATE customers");
        }

        return t;
    }

    public AuthResponse loginInternal(LoginRequest req) {
        InternalUser u = internalRepo.findByEmployeeId(req.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password."));

        if ("LOCKED".equals(u.getStatus()) || "INACTIVE".equals(u.getStatus()))
            throw new RuntimeException("Account locked. Contact support.");

        if (!encoder.matches(req.getPassword(), u.getPasswordHash())) {
            int attempts = (u.getFailedLoginAttempts() == null ? 0 : u.getFailedLoginAttempts()) + 1;
            u.setFailedLoginAttempts(attempts);
            if (attempts >= 3) {
                u.setStatus("LOCKED");
                internalRepo.save(u);
                throw new RuntimeException("Account locked. Contact support.");
            }
            internalRepo.save(u);
            throw new RuntimeException("Invalid username or password.");
        }

        // Reset on success
        u.setFailedLoginAttempts(0);
        internalRepo.save(u);

        String subject = "internal:" + u.getId();
        String token = jwtUtil.generateToken(subject, Map.of(
                "role", u.getRole(),
                "userType", "INTERNAL",
                "internalUserId", u.getId()
        ));
        return new AuthResponse(token, u.getRole(), subject);
    }

    public String requestForgotPasswordOtp(ForgotPasswordRequest req) {
        Customer c = findCustomerByIdentifier(req.getCustomerIdentifier());
        var otp = otpService.createOtp(c.getId(), "LOGIN_2FA");
        System.out.println("FORGOT PASSWORD OTP for customerId " + c.getId() + " = " + otp.getOtpCode());
        return otp.getOtpCode();
    }

    public void resetPassword(ResetPasswordRequest req) {
        Customer c = findCustomerByIdentifier(req.getCustomerIdentifier());
        boolean ok = otpService.verifyOtp(c.getId(), "LOGIN_2FA", req.getOtpCode());
        if (!ok) throw new RuntimeException("Invalid or expired OTP");
        c.setPasswordHash(encoder.encode(req.getNewPassword()));
        customerRepo.save(c);
    }

    private Customer findCustomerByIdentifier(String identifier) {
        return customerRepo.findByEmail(identifier)
                .or(() -> customerRepo.findByMobile(identifier))
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }
}