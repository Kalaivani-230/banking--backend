package com.example.MiniProject.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class RegisterRequest {
    @NotBlank @Size(min=3)
    private String fullName;

    @NotBlank @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile must be 10 digits starting with 6, 7, 8, or 9")
    private String mobile;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
        message = "Password must include uppercase, lowercase, number and special character"
    )
    private String password;

    @NotBlank
    private String customerTypeCode; // RETAIL/CORPORATE
    
    @NotBlank
    private String preferredAccountType; // SAVINGS/CURRENT

	public String getPreferredAccountType() {
		return preferredAccountType;
	}

	public void setPreferredAccountType(String preferredAccountType) {
		this.preferredAccountType = preferredAccountType;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getCustomerTypeCode() {
		return customerTypeCode;
	}

	public void setCustomerTypeCode(String customerTypeCode) {
		this.customerTypeCode = customerTypeCode;
	}


}