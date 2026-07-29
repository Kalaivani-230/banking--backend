package com.example.MiniProject.dto;

import jakarta.validation.constraints.NotBlank;

public class KycSubmitRequest {
    @NotBlank
    private String pan;

    @NotBlank
    private String aadhaar;

    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }

    public String getAadhaar() { return aadhaar; }
    public void setAadhaar(String aadhaar) { this.aadhaar = aadhaar; }
}