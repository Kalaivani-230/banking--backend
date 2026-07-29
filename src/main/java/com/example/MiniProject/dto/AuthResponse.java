package com.example.MiniProject.dto;

public record AuthResponse(String accessToken, String role, String subject) {}