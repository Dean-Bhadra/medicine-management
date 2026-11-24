package com.debanjan.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import com.debanjan.model.Medicine;
import com.debanjan.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@Service
public class MedicineService {

    @Autowired
    private MedicineRepository medicineRepository;

    public Medicine addMedicine(Medicine medicine) {
        medicine.setId(null);
        medicine.setVersion(null);
        return medicineRepository.save(medicine);
    }

    @Transactional
    public Medicine updateMedicine(Long id, Medicine medicine) {
        Medicine current = medicineRepository.lockBatch(id).orElse(null);
        if (current == null) return null;
        if (medicine.getVersion() != null && !medicine.getVersion().equals(current.getVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This batch changed. Close and refresh before editing.");
        }
        medicine.setId(id);
        medicine.setVersion(current.getVersion());
        return medicineRepository.save(medicine);
    }

    public void deleteMedicine(Long id) {
        try {
            medicineRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A batch with sales history must be retained");
        }
    }

    public Medicine getMedicineById(Long id) {
        return medicineRepository.findById(id).orElse(null);
    }

    public List<Medicine> getAllMedicines() {
        return medicineRepository.findAll();
    }

    public List<Medicine> searchMedicinesByName(String name) {
        return medicineRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Medicine> getExpiringMedicines(LocalDate date) {
        return medicineRepository.findByExpiryDateBefore(date);
    }

    public List<Medicine> getLowStockMedicines(Integer threshold) {
        return medicineRepository.findByStockQuantityLessThan(threshold);
    }

    @Transactional
    public void updateStock(Long id, Integer quantity) {
        Medicine medicine = medicineRepository.lockBatch(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch not found"));
        long stock = (long) (medicine.getStockQuantity() == null ? 0 : medicine.getStockQuantity()) + quantity;
        if (stock < 0 || stock > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adjustment exceeds stock limits");
        }
        medicine.setStockQuantity((int) stock);
    }
}
