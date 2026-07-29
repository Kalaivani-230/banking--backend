package com.example.MiniProject.repository;

import com.example.MiniProject.entity.CustomerLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerLimitRepository extends JpaRepository<CustomerLimit, Long> {
    Optional<CustomerLimit> findTopByCustomerIdOrderByEffectiveFromDesc(Long customerId);
}