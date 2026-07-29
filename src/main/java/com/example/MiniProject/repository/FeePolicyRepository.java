package com.example.MiniProject.repository;

import com.example.MiniProject.entity.FeePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeePolicyRepository extends JpaRepository<FeePolicy, Long> {
    List<FeePolicy> findByStatusOrderByUpdatedAtDesc(String status);
    List<FeePolicy> findAllByOrderByUpdatedAtDesc();
    List<FeePolicy> findByCorridorIdAndChannelCodeAndCustomerTypeCodeAndStatus(
            Long corridorId, String channelCode, String customerTypeCode, String status);
}