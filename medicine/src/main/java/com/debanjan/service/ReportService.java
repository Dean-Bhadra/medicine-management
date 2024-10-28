package com.debanjan.service;

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

    public Map<String, Object> generateSalesReport(LocalDateTime start, LocalDateTime end) {
        List<Sale> sales = salesService.getSalesByDateRange(start, end);
        Double totalSales = salesService.getTotalSales(start, end);

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", start);
        report.put("endDate", end);
        report.put("totalSales", totalSales);
        report.put("numberOfTransactions", sales.size());

        // Calculate top selling medicines
        Map<Medicine, Integer> medicineQuantities = new HashMap<>();
        sales.forEach(sale ->
                sale.getItems().forEach(item ->
                        medicineQuantities.merge(item.getMedicine(), item.getQuantity(), Integer::sum)
                )
        );

        List<Map<String, Object>> topMedicines = medicineQuantities.entrySet().stream()
                .sorted(Map.Entry.<Medicine, Integer>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    Map<String, Object> medicineData = new HashMap<>();
                    medicineData.put("name", entry.getKey().getName());
                    medicineData.put("quantity", entry.getValue());
                    medicineData.put("revenue", entry.getValue() * entry.getKey().getUnitPrice());
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
                .mapToDouble(m -> m.getUnitPrice() * m.getStockQuantity())
                .sum());
        report.put("lowStockMedicines", lowStockMedicines);
        report.put("expiringMedicines", expiringMedicines);

        return report;
    }
}