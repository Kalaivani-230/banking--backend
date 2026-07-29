package com.example.MiniProject.repository;

import com.example.MiniProject.entity.DomesticTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DomesticTransactionRepository extends JpaRepository<DomesticTransaction, Long> {
    List<DomesticTransaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    Optional<DomesticTransaction> findByReferenceId(String referenceId);
    List<DomesticTransaction> findByFromAccountNoAndStatusAndCreatedAtAfter(
            String accountNo, String status, Instant after);

    List<DomesticTransaction> findByFromAccountNoAndStatusInAndCreatedAtAfter(
            String accountNo, List<String> statuses, Instant after);

    @Query("SELECT t FROM DomesticTransaction t WHERE t.customerId = :cid " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:from IS NULL OR t.createdAt >= :from) " +
           "AND (:to IS NULL OR t.createdAt <= :to) " +
           "ORDER BY t.createdAt DESC")
    List<DomesticTransaction> findFiltered(
            @Param("cid") Long customerId,
            @Param("status") String status,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
