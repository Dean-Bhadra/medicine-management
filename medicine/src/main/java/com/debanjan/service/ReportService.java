package com.debanjan.service;

import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import com.debanjan.model.Medicine;
import com.debanjan.model.Sale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private SalesService salesService;

    @Autowired
    private MedicineService medicineService;

    @Transactional(readOnly = true)
    public Map<String, Object> generateSalesReport(LocalDateTime start, LocalDateTime end) {
        List<Sale> sales = salesService.getSalesByDateRange(start, end);
        var totalSales = sales.stream().map(Sale::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", start);
        report.put("endDate", end);
        report.put("totalSales", totalSales);
        report.put("numberOfTransactions", sales.size());

        // Calculate top selling medicines
        Map<Medicine, Integer> medicineQuantities = new HashMap<>();
        Map<Medicine, BigDecimal> revenue = new HashMap<>();
        for (Sale sale : sales) {
            for (var item : sale.getItems()) {
                medicineQuantities.merge(item.getMedicine(), item.getQuantity(), Integer::sum);
                revenue.merge(item.getMedicine(), item.getTotalPrice(), BigDecimal::add);
            }
        }

        List<Map<String, Object>> topMedicines = medicineQuantities.entrySet().stream()
                .sorted(Map.Entry.<Medicine, Integer>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> medicineData = new HashMap<>();
                    medicineData.put("name", entry.getKey().getName());
                    medicineData.put("quantity", entry.getValue());
                    medicineData.put("revenue", revenue.get(entry.getKey()));
                    return medicineData;
                })
                .collect(Collectors.toList());

        report.put("topSellingMedicines", topMedicines);

        return report;
    }

    public Map<String, Object> generateInventoryReport() {
        List<Medicine> allMedicines = medicineService.getAllMedicines();
        List<Medicine> lowStockMedicines = medicineService.getLowStockMedicines(10);
        List<Medicine> expiringMedicines = medicineService.getExpiringMedicines(LocalDateTime.now().plusMonths(3).toLocalDate());

        Map<String, Object> report = new HashMap<>();
        report.put("totalMedicines", allMedicines.size());
        report.put("totalValue", allMedicines.stream()
                .filter(m -> m.getStockQuantity() != null)
                .map(m -> m.getUnitPrice().multiply(BigDecimal.valueOf(m.getStockQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.put("lowStockMedicines", lowStockMedicines);
        report.put("expiringMedicines", expiringMedicines);

        return report;
    }
}