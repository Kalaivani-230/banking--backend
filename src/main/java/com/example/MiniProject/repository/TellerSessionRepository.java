package com.example.MiniProject.repository;

import com.example.MiniProject.entity.TellerSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TellerSessionRepository extends JpaRepository<TellerSession, Long> {
    Optional<TellerSession> findByTellerIdAndSessionDate(Long tellerId, LocalDate date);
}
