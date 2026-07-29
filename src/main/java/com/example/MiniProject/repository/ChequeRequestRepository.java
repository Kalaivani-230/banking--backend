package com.example.MiniProject.repository;

import com.example.MiniProject.entity.ChequeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChequeRequestRepository extends JpaRepository<ChequeRequest, Long> {
    List<ChequeRequest> findByAccountNoOrderByCreatedAtDesc(String accountNo);
    List<ChequeRequest> findByTellerIdOrderByCreatedAtDesc(Long tellerId);
}
