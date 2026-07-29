package com.example.MiniProject.repository;

import com.example.MiniProject.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByEmail(String email);
    boolean existsByMobile(String mobile);
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByMobile(String mobile);
    List<Customer> findByFullNameContainingIgnoreCase(String name);
}