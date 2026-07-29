package com.example.MiniProject.repository;

import com.example.MiniProject.entity.MasterCurrency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterCurrencyRepository extends JpaRepository<MasterCurrency, String> {}