package com.example.MiniProject.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OtpVerifyRequest {
    @NotBlank
    private String customerIdentifier; // email or mobile

    @NotBlank
    private String otpCode;

    @NotBlank
    private String purpose; // REGISTRATION / LOGIN_2FA / TRANSFER_2FA

	public String getCustomerIdentifier() {
		return customerIdentifier;
	}

	public void setCustomerIdentifier(String customerIdentifier) {
		this.customerIdentifier = customerIdentifier;
	}

	public String getOtpCode() {
		return otpCode;
	}

	public void setOtpCode(String otpCode) {
		this.otpCode = otpCode;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}


}