package com.example.MiniProject.controller;

import com.example.MiniProject.dto.*;
import com.example.MiniProject.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Customer
    @PostMapping("/customer/register")
    public String register(@Valid @RequestBody RegisterRequest req) {
        return authService.registerCustomer(req);
    }

    @PostMapping("/customer/verify-otp")
    public void verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
        authService.verifyRegistrationOtp(req);
    }

    @PostMapping("/customer/resend-otp")
    public String resendOtp(@Valid @RequestBody ForgotPasswordRequest req) {
        return authService.resendRegistrationOtp(req);
    }

    @PostMapping("/customer/login")
    public AuthResponse loginCustomer(@Valid @RequestBody LoginRequest req) {
        return authService.loginCustomer(req);
    }

    @PostMapping("/customer/forgot-password/request-otp")
    public String forgotPasswordOtp(@Valid @RequestBody ForgotPasswordRequest req) {
        return authService.requestForgotPasswordOtp(req);
    }

    @PostMapping("/customer/forgot-password/reset")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
    }

    // Internal
    @PostMapping("/internal/login")
    public AuthResponse loginInternal(@Valid @RequestBody LoginRequest req) {
        return authService.loginInternal(req);
    }
}