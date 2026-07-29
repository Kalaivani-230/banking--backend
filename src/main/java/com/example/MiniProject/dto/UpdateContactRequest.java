package com.example.MiniProject.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateContactRequest {

    @NotBlank
    private String field; // EMAIL or MOBILE

    @NotBlank
    private String newValue;

    @NotBlank
    private String otpCode;

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
}
