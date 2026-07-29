package com.example.MiniProject.controller;

import com.example.MiniProject.dto.CustomerProfileResponse;
import com.example.MiniProject.dto.UpdateContactRequest;
import com.example.MiniProject.dto.UpdateProfileRequest;
import com.example.MiniProject.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/customer/profile")
public class CustomerProfileController {

    private final ProfileService profileService;

    public CustomerProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/{customerId}")
    public CustomerProfileResponse getProfile(@PathVariable Long customerId) {
        return profileService.getProfile(customerId);
    }

    @PutMapping("/{customerId}")
    public CustomerProfileResponse updateProfile(@PathVariable Long customerId,
                                                 @Valid @RequestBody UpdateProfileRequest req) {
        return profileService.updateProfile(customerId, req);
    }

    @PostMapping("/{customerId}/request-contact-otp")
    public Map<String, String> requestContactOtp(@PathVariable Long customerId,
                                                  @RequestBody Map<String, String> body) {
        profileService.requestContactChangeOtp(customerId, body.get("field"), body.get("newValue"));
        return Map.of("message", "OTP sent. Please verify to apply the change.");
    }

    @PostMapping("/{customerId}/verify-contact-change")
    public CustomerProfileResponse verifyContactChange(@PathVariable Long customerId,
                                                       @Valid @RequestBody UpdateContactRequest req) {
        return profileService.verifyAndUpdateContact(customerId, req);
    }
}
