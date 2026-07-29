package com.example.MiniProject.repository;

import com.example.MiniProject.entity.FeeComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeComponentRepository extends JpaRepository<FeeComponent, Long> {
    List<FeeComponent> findByPolicyId(Long policyId);
    void deleteByPolicyId(Long policyId);
}
