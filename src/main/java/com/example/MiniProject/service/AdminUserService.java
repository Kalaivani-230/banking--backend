package com.example.MiniProject.service;

import com.example.MiniProject.dto.InternalUserRequest;
import com.example.MiniProject.entity.InternalUser;
import com.example.MiniProject.repository.InternalUserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AdminUserService {

    private final InternalUserRepository userRepo;
    private final BCryptPasswordEncoder encoder;

    public AdminUserService(InternalUserRepository userRepo, BCryptPasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    public List<InternalUser> listAll() {
        return userRepo.findAll();
    }

    public InternalUser create(InternalUserRequest req) {
        if (userRepo.existsByEmployeeId(req.getEmployeeId()))
            throw new RuntimeException("Employee ID already exists");

        // Auto-generate a temporary password
        String tempPassword = "Temp@" + UUID.randomUUID().toString().substring(0, 6);
        System.out.println("TEMP PASSWORD for " + req.getEmployeeId() + " = " + tempPassword);

        InternalUser u = new InternalUser();
        u.setEmployeeId(req.getEmployeeId());
        u.setFullName(req.getFullName());
        u.setEmail(req.getEmail());
        u.setMobile(req.getMobile());
        u.setRole(req.getRole());
        u.setStatus("ACTIVE");
        u.setFailedLoginAttempts(0);
        u.setPasswordHash(encoder.encode(tempPassword));
        return userRepo.save(u);
    }

    public InternalUser update(Long userId, InternalUserRequest req) {
        InternalUser u = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // If employeeId changed, check uniqueness
        if (!u.getEmployeeId().equals(req.getEmployeeId()) &&
                userRepo.existsByEmployeeId(req.getEmployeeId()))
            throw new RuntimeException("Employee ID already exists");

        u.setEmployeeId(req.getEmployeeId());
        u.setFullName(req.getFullName());
        u.setEmail(req.getEmail());
        u.setMobile(req.getMobile());
        u.setRole(req.getRole());
        return userRepo.save(u);
    }

    public InternalUser setStatus(Long userId, String status, String reason) {
        InternalUser u = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        u.setStatus(status);
        // Reset failed attempts on activation
        if ("ACTIVE".equals(status)) u.setFailedLoginAttempts(0);
        System.out.println("User " + u.getEmployeeId() + " status changed to " + status +
                (reason != null ? " | Reason: " + reason : ""));
        return userRepo.save(u);
    }
}
