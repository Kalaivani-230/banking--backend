package com.example.MiniProject.repository;

import com.example.MiniProject.entity.FeeQuote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeeQuoteRepository extends JpaRepository<FeeQuote, Long> {
    Optional<FeeQuote> findByQuoteRef(String quoteRef);
}
