package com.example.MiniProject.repository;

import com.example.MiniProject.entity.InternationalTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InternationalTransactionRepository extends JpaRepository<InternationalTransaction, Long> {

    Optional<InternationalTransaction> findByReferenceId(String referenceId);

    List<InternationalTransaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<InternationalTransaction> findByStatusOrderByCreatedAtDesc(String status);

    // For daily limit check
    List<InternationalTransaction> findByFromAccountNoAndStatusInAndCreatedAtAfter(
            String accountNo, List<String> statuses, Instant after);

    List<InternationalTransaction> findByAdminReviewedFalseAndAdminFlagReasonIsNotNullOrderByCreatedAtDesc();

    // Filtered history for customer
    @Query("SELECT t FROM InternationalTransaction t WHERE t.customerId = :customerId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:from IS NULL OR t.createdAt >= :from) " +
           "AND (:to IS NULL OR t.createdAt <= :to) " +
           "ORDER BY t.createdAt DESC")
    List<InternationalTransaction> findFiltered(
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
