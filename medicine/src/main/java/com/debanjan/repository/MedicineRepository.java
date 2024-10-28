package com.debanjan.repository;

import com.debanjan.model.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    List<Medicine> findByNameContainingIgnoreCase(String name);
    List<Medicine> findByExpiryDateBefore(LocalDate date);
    List<Medicine> findByStockQuantityLessThan(Integer quantity);
}
