package com.example.MiniProject.repository;

import com.example.MiniProject.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    List<Beneficiary> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
