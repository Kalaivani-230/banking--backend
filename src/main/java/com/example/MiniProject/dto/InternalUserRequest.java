package com.example.MiniProject.dto;

import jakarta.validation.constraints.NotBlank;

public class InternalUserRequest {

    @NotBlank
    private String employeeId;

    @NotBlank
    private String fullName;

    private String email;
    private String mobile;

    @NotBlank
    private String role; // ADMIN, COMPLIANCE_OPS, TELLER

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
