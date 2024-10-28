package com.debanjan.service;

import com.debanjan.model.Medicine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class ScheduledTasks {

    @Autowired
    private MedicineService medicineService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private PDFReportGenerator pdfReportGenerator;


    // Check for low stock daily at 9 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void checkLowStock() {
        List<Medicine> lowStockMedicines = medicineService.getLowStockMedicines(10);
        for (Medicine medicine : lowStockMedicines) {
            notificationService.sendLowStockAlert(medicine);
        }
    }

    // Check for expiring medicines daily at 9 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void checkExpiringMedicines() {
        List<Medicine> expiringMedicines =
                medicineService.getExpiringMedicines(LocalDateTime.now().plusMonths(1).toLocalDate());
        for (Medicine medicine : expiringMedicines) {
            notificationService.sendExpiryAlert(medicine);
        }
    }

    // Generate weekly report on Sunday at midnight
    @Scheduled(cron = "0 0 0 * * SUN")
    public void generateWeeklyReport() {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusWeeks(1);

        Map<String, Object> report = reportService.generateSalesReport(startDate, endDate);
        byte[] pdfReport = pdfReportGenerator.generateSalesReport(report);

        // Send report via email
        notificationService.sendWeeklyReport(pdfReport);
    }
}
