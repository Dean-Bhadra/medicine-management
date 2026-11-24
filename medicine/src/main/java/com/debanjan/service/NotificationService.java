package com.debanjan.service;

import org.springframework.beans.factory.annotation.Value;
import com.debanjan.model.Medicine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${ALERT_RECIPIENT:}")
    private String recipient;

    public void sendLowStockAlert(Medicine medicine) {
        String subject = "Low Stock Alert: " + medicine.getName();
        String body = String.format("""
            Low stock alert for medicine:
            Name: %s
            Current Stock: %d
            Minimum Required: 10
            Please reorder soon.
            """, medicine.getName(), medicine.getStockQuantity());

        sendEmail(recipient, subject, body);
    }

    public void sendExpiryAlert(Medicine medicine) {
        String subject = "Medicine Expiry Alert: " + medicine.getName();
        String body = String.format("""
            Medicine expiry alert:
            Name: %s
            Expiry Date: %s
            Current Stock: %d
            Please check and take necessary action.
            """, medicine.getName(), medicine.getExpiryDate(), medicine.getStockQuantity());

        sendEmail(recipient, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
    public void sendWeeklyReport(byte[] pdfReport) {
        String subject = "Weekly Sales Report";
        String body = "Please find attached the weekly sales report.";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body, false);
            helper.addAttachment("weekly-report.pdf", new ByteArrayResource(pdfReport));
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}