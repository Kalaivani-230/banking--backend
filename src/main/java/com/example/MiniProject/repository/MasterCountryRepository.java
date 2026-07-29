package com.example.MiniProject.repository;

import com.example.MiniProject.entity.MasterCountry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterCountryRepository extends JpaRepository<MasterCountry, String> {}