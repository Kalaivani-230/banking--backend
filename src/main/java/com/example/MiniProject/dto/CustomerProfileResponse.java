package com.example.MiniProject.dto;

import java.time.Instant;

public class CustomerProfileResponse {
    public Long id;
    public String customerCode;
    public String fullName;
    public String email;
    public String mobile;
    public String address;
    public String status;
    public String customerTypeCode;
    public String preferredAccountType;
    public Instant createdAt;
}
