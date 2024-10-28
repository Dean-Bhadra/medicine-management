package com.debanjan.service;

import com.debanjan.model.Medicine;
import com.debanjan.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class MedicineService {

    @Autowired
    private MedicineRepository medicineRepository;

    public Medicine addMedicine(Medicine medicine) {
        return medicineRepository.save(medicine);
    }

    public Medicine updateMedicine(Long id, Medicine medicine) {
        if (medicineRepository.existsById(id)) {
            medicine.setId(id);
            return medicineRepository.save(medicine);
        }
        return null;
    }

    public void deleteMedicine(Long id) {
        medicineRepository.deleteById(id);
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

    public void updateStock(Long id, Integer quantity) {
        Medicine medicine = getMedicineById(id);
        if (medicine != null) {
            medicine.setStockQuantity(medicine.getStockQuantity() + quantity);
            medicineRepository.save(medicine);
        }
    }
}
