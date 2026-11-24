package com.debanjan.repository;

import java.util.Optional;
import java.math.BigDecimal;
import com.debanjan.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findBySaleDateBetween(LocalDateTime start, LocalDateTime end);

    Optional<Sale> findByCheckoutKey(String checkoutKey);

    @Query("SELECT SUM(s.totalAmount) FROM Sale s WHERE s.saleDate BETWEEN ?1 AND ?2")
    BigDecimal getTotalSalesAmount(LocalDateTime start, LocalDateTime end);
}