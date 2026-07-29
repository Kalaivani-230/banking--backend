package com.example.MiniProject.dto;

import jakarta.validation.constraints.NotBlank;

public class KycRejectRequest {
    @NotBlank
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}