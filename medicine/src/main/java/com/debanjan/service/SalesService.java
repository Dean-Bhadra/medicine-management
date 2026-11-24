package com.debanjan.service;

import java.time.LocalDate;
import org.springframework.util.StringUtils;
import java.util.Comparator;
import java.math.BigDecimal;
import com.debanjan.model.Sale;
import com.debanjan.model.SaleItem;
import com.debanjan.model.Medicine;
import com.debanjan.dto.SaleDTO;
import com.debanjan.dto.SaleItemDTO;
import com.debanjan.repository.SaleRepository;
import com.debanjan.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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

    @Autowired
    private CustomerService customerService;

    @Transactional
    public Sale createSale(SaleDTO saleDTO) {
        if (saleDTO.getCheckoutKey() != null) {
            var existing = saleRepository.findByCheckoutKey(saleDTO.getCheckoutKey());
            if (existing.isPresent()) return existing.get();
        }
        Sale sale = new Sale();
        sale.setCheckoutKey(saleDTO.getCheckoutKey());
        sale.setSaleDate(LocalDateTime.now());
        sale.setCustomerName(saleDTO.getCustomerName());
        sale.setCustomerPhone(saleDTO.getCustomerPhone());
        sale.setPrescriptionNumber(saleDTO.getPrescriptionNumber());
        sale.setPaymentMethod(saleDTO.getPaymentMethod());

        List<SaleItem> saleItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (SaleItemDTO itemDTO : saleDTO.getItems().stream()
                .sorted(Comparator.comparing(SaleItemDTO::getMedicineId)).toList()) {
            Medicine medicine = medicineRepository.lockBatch(itemDTO.getMedicineId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicine not found"));

            if (itemDTO.getExpectedUnitPrice() != null
                    && medicine.getUnitPrice().compareTo(itemDTO.getExpectedUnitPrice()) != 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Price changed. Refresh and review the sale.");
            }
            if (medicine.getExpiryDate() == null || medicine.getExpiryDate().isBefore(LocalDate.now())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Check expiry for " + medicine.getName());
            }
            if (medicine.isRequiresPrescription() && !StringUtils.hasText(saleDTO.getPrescriptionNumber())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a prescription reference for " + medicine.getName());
            }
            if (medicine.getStockQuantity() == null || medicine.getStockQuantity() < itemDTO.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for " + medicine.getName());
            }

            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setMedicine(medicine);
            item.setMedicineName(medicine.getName());
            item.setBatchNumber(medicine.getBatchNumber());
            item.setQuantity(itemDTO.getQuantity());
            item.setUnitPrice(medicine.getUnitPrice());
            item.setTotalPrice(medicine.getUnitPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));

            medicine.setStockQuantity(medicine.getStockQuantity() - itemDTO.getQuantity());
            medicineRepository.save(medicine);

            totalAmount = totalAmount.add(item.getTotalPrice());
            saleItems.add(item);
        }

        if (StringUtils.hasText(saleDTO.getCustomerPhone())) {
            sale.setCustomer(customerService.getOrCreateCustomer(saleDTO.getCustomerPhone().trim(), saleDTO.getCustomerName()));
        }
        sale.setItems(saleItems);
        sale.setTotalAmount(totalAmount);

        return saleRepository.save(sale);
    }

    public List<Sale> getSalesByDateRange(LocalDateTime start, LocalDateTime end) {
        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date precedes start date");
        }
        return saleRepository.findBySaleDateBetween(start, end);
    }

    public BigDecimal getTotalSales(LocalDateTime start, LocalDateTime end) {
        return saleRepository.getTotalSalesAmount(start, end);
    }
}
