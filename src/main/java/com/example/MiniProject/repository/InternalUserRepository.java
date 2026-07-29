package com.example.MiniProject.repository;

import com.example.MiniProject.entity.InternalUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InternalUserRepository extends JpaRepository<InternalUser, Long> {
    Optional<InternalUser> findByEmployeeId(String employeeId);
    
    boolean existsByEmployeeId(String employeeId);
}
