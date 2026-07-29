package com.example.MiniProject.repository;

import com.example.MiniProject.entity.FxRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FxRateRepository extends JpaRepository<FxRate, Long> {

    // Used by fee engine — fetch current active rate for a pair
    Optional<FxRate> findTopByFromCurrencyAndToCurrencyAndStatusOrderByEffectiveFromDesc(
            String fromCurrency, String toCurrency, String status);

    // All active rates for a pair (for deactivation on new activation)
    List<FxRate> findByFromCurrencyAndToCurrencyAndStatus(
            String fromCurrency, String toCurrency, String status);

    // History — all rates for a pair ordered newest first
    List<FxRate> findByFromCurrencyAndToCurrencyOrderByCreatedAtDesc(
            String fromCurrency, String toCurrency);

    // All rates ordered newest first (admin full list)
    List<FxRate> findAllByOrderByCreatedAtDesc();
}
