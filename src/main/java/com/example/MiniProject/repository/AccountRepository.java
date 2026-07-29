package com.example.MiniProject.repository;

import com.example.MiniProject.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByCustomerId(Long customerId);
    boolean existsByCustomerId(Long customerId);
    Optional<Account> findByAccountNo(String accountNo);

    @Query("SELECT a FROM Account a WHERE " +
           "(:accountNo IS NULL OR a.accountNo LIKE %:accountNo%) OR " +
           "(:customerId IS NULL OR CAST(a.customerId AS string) = :customerId)")
    List<Account> searchAccounts(@Param("accountNo") String accountNo,
                                  @Param("customerId") String customerId);
}
