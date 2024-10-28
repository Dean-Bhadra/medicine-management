package com.debanjan.controller;

import com.debanjan.model.Sale;
import com.debanjan.dto.SaleDTO;
import com.debanjan.service.SalesService;
import com.debanjan.service.CustomerService;
import com.debanjan.service.ReportService;
import com.debanjan.service.PDFReportGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
public class SalesController {

    @Autowired
    private SalesService salesService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private PDFReportGenerator pdfReportGenerator;

    @PostMapping
    public ResponseEntity<Sale> createSale(@RequestBody SaleDTO saleDTO) {
        return ResponseEntity.ok(salesService.createSale(saleDTO));
    }

    @GetMapping
    public ResponseEntity<List<Sale>> getSales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(salesService.getSalesByDateRange(start, end));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Sale>> getCustomerPurchaseHistory(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerService.getCustomerPurchaseHistory(customerId));
    }

    @GetMapping("/report/pdf")
    public ResponseEntity<byte[]> getSalesPdfReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Map<String, Object> reportData = reportService.generateSalesReport(start, end);
        byte[] pdfBytes = pdfReportGenerator.generateSalesReport(reportData);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=sales-report.pdf")
                .body(pdfBytes);
    }
}