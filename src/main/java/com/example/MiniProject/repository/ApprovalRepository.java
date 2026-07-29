package com.example.MiniProject.repository;

import com.example.MiniProject.entity.Approval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    List<Approval> findByEntityTypeAndStatusOrderBySubmittedAtDesc(String entityType, String status);
    Optional<Approval> findTopByEntityIdAndEntityTypeAndStatusOrderBySubmittedAtDesc(
            Long entityId, String entityType, String status);
}