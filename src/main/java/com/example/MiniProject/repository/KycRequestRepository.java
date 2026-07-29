package com.example.MiniProject.repository;

import com.example.MiniProject.entity.KycRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycRequestRepository extends JpaRepository<KycRequest, Long> {

    Optional<KycRequest> findTopByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<KycRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<KycRequest> findByPanAndCustomerIdNot(String pan, Long customerId);

    List<KycRequest> findByAadhaarAndCustomerIdNot(String aadhaar, Long customerId);
}