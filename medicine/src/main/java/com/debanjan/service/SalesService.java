package com.debanjan.service;

import com.debanjan.model.Sale;
import com.debanjan.model.SaleItem;
import com.debanjan.model.Medicine;
import com.debanjan.dto.SaleDTO;
import com.debanjan.dto.SaleItemDTO;
import com.debanjan.repository.SaleRepository;
import com.debanjan.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
public class SalesService {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Transactional
    public Sale createSale(SaleDTO saleDTO) {
        Sale sale = new Sale();
        sale.setSaleDate(LocalDateTime.now());
        sale.setCustomerName(saleDTO.getCustomerName());
        sale.setCustomerPhone(saleDTO.getCustomerPhone());
        sale.setPrescriptionNumber(saleDTO.getPrescriptionNumber());
        sale.setPaymentMethod(saleDTO.getPaymentMethod());

        List<SaleItem> saleItems = new ArrayList<>();
        double totalAmount = 0.0;

        for (SaleItemDTO itemDTO : saleDTO.getItems()) {
            Medicine medicine = medicineRepository.findById(itemDTO.getMedicineId())
                    .orElseThrow(() -> new RuntimeException("Medicine not found"));

            if (medicine.getStockQuantity() < itemDTO.getQuantity()) {
                throw new RuntimeException("Insufficient stock for " + medicine.getName());
            }

            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setMedicine(medicine);
            item.setQuantity(itemDTO.getQuantity());
            item.setUnitPrice(medicine.getUnitPrice());
            item.setTotalPrice(medicine.getUnitPrice() * itemDTO.getQuantity());

            medicine.setStockQuantity(medicine.getStockQuantity() - itemDTO.getQuantity());
            medicineRepository.save(medicine);

            totalAmount += item.getTotalPrice();
            saleItems.add(item);
        }

        sale.setItems(saleItems);
        sale.setTotalAmount(totalAmount);

        return saleRepository.save(sale);
    }

    public List<Sale> getSalesByDateRange(LocalDateTime start, LocalDateTime end) {
        return saleRepository.findBySaleDateBetween(start, end);
    }

    public Double getTotalSales(LocalDateTime start, LocalDateTime end) {
        return saleRepository.getTotalSalesAmount(start, end);
    }
}
