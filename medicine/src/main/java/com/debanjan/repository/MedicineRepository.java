package com.debanjan.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import com.debanjan.model.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Medicine m where m.id = ?1")
    Optional<Medicine> lockBatch(Long id);

    List<Medicine> findByNameContainingIgnoreCase(String name);
    List<Medicine> findByExpiryDateBefore(LocalDate date);
    List<Medicine> findByStockQuantityLessThan(Integer quantity);
}
