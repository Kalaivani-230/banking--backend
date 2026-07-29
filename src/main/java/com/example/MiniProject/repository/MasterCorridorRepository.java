package com.example.MiniProject.repository;

import com.example.MiniProject.entity.MasterCorridor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MasterCorridorRepository extends JpaRepository<MasterCorridor, Long> {
    Optional<MasterCorridor> findByFromCountryCodeAndToCountryCodeAndIsActiveTrue(String from, String to);
}