package com.example.MiniProject.repository;
import java.util.Optional;
import com.example.MiniProject.entity.CustomerOtp;

import org.springframework.data.jpa.repository.JpaRepository;


public interface CustomerOtpRepository extends JpaRepository<CustomerOtp, Long> {
    Optional<CustomerOtp> findTopByCustomerIdAndPurposeOrderByCreatedAtDesc(Long customerId, String purpose);
}

