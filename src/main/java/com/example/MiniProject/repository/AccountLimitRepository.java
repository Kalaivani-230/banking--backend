package com.example.MiniProject.repository;

import com.example.MiniProject.entity.AccountLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountLimitRepository extends JpaRepository<AccountLimit, Long> {
    Optional<AccountLimit> findTopByAccountIdOrderByEffectiveFromDesc(Long accountId);
}