package com.debanjan.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PDFReportGenerator {

    public byte[] generateSalesReport(Map<String, Object> reportData) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Sales Report")
                    .setFontSize(20)
                    .setBold());

            document.add(new Paragraph("Period: " +
                    reportData.get("startDate") + " to " + reportData.get("endDate")));

            document.add(new Paragraph("Total Sales: $" + reportData.get("totalSales")));
            document.add(new Paragraph("Number of Transactions: " +
                    reportData.get("numberOfTransactions")));

            // Add top-selling medicines table
            Table table = new Table(3);
            table.addCell("Medicine");
            table.addCell("Quantity");
            table.addCell("Revenue");

            List<Map<String, Object>> topMedicines =
                    (List<Map<String, Object>>) reportData.get("topSellingMedicines");

            for (Map<String, Object> medicine : topMedicines) {
                table.addCell(medicine.get("name").toString());
                table.addCell(medicine.get("quantity").toString());
                table.addCell("$" + medicine.get("revenue").toString());
            }

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }
}