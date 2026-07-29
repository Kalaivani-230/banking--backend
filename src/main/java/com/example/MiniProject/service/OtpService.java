package com.example.MiniProject.service;

import com.example.MiniProject.entity.CustomerOtp;
import com.example.MiniProject.repository.CustomerOtpRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

@Service
public class OtpService {

    private final CustomerOtpRepository otpRepo;
    private final int expirySeconds;
    private final SecureRandom random = new SecureRandom();

    private final int resendCooldownSeconds;

    public OtpService(CustomerOtpRepository otpRepo,
                      @Value("${app.otp.expirySeconds}") int expirySeconds,
                      @Value("${app.otp.resendCooldownSeconds:30}") int resendCooldownSeconds) {
        this.otpRepo = otpRepo;
        this.expirySeconds = expirySeconds;
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    public String generateOtp() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public CustomerOtp createOtp(Long customerId, String purpose) {
        String code = generateOtp();

        CustomerOtp otp = new CustomerOtp();
        otp.setCustomerId(customerId);
        otp.setPurpose(purpose);
        otp.setOtpCode(code);
        otp.setExpiresAt(Instant.now().plusSeconds(expirySeconds));
        otp.setAttempts(0);
        otp.setVerifiedAt(null);

        // createdAt will be set by @PrePersist if you kept it in entity.
        return otpRepo.save(otp);
    }

    public CustomerOtp resendOtp(Long customerId, String purpose) {
        otpRepo.findTopByCustomerIdAndPurposeOrderByCreatedAtDesc(customerId, purpose)
                .ifPresent(latest -> {
                    if (latest.getVerifiedAt() == null &&
                        Instant.now().isBefore(latest.getCreatedAt().plusSeconds(resendCooldownSeconds))) {
                        long secondsLeft = latest.getCreatedAt().plusSeconds(resendCooldownSeconds)
                                .getEpochSecond() - Instant.now().getEpochSecond();
                        throw new RuntimeException("Please wait " + secondsLeft + " seconds before resending OTP.");
                    }
                });
        return createOtp(customerId, purpose);
    }

    private static final int MAX_OTP_ATTEMPTS = 5;

    public boolean verifyOtp(Long customerId, String purpose, String otpCode) {
        CustomerOtp latest = otpRepo.findTopByCustomerIdAndPurposeOrderByCreatedAtDesc(customerId, purpose)
                .orElse(null);

        if (latest == null) return false;
        if (latest.getVerifiedAt() != null) return false;
        if (Instant.now().isAfter(latest.getExpiresAt())) return false;

        // Max attempt limit — block brute force
        Integer attempts = latest.getAttempts() == null ? 0 : latest.getAttempts();
        if (attempts >= MAX_OTP_ATTEMPTS) {
            throw new RuntimeException("Maximum OTP attempts exceeded. Please request a new OTP.");
        }

        latest.setAttempts(attempts + 1);

        if (!latest.getOtpCode().equals(otpCode)) {
            otpRepo.save(latest);
            return false;
        }

        latest.setVerifiedAt(Instant.now());
        otpRepo.save(latest);
        return true;
    }
}
