# Spring Boot Backend - Email Sales Report Integration

This document contains the Spring Boot backend code needed to complete the email sales report feature.

## Files to Create/Update

### 1. Create: `src/main/java/com/loretacafe/pos/dto/SalesReportEmailRequest.java`

```java
package com.loretacafe.pos.dto;

public class SalesReportEmailRequest {
    private String recipientEmail;
    private String date;
    private String reportBody;
    
    // Default constructor
    public SalesReportEmailRequest() {}
    
    // Constructor with parameters
    public SalesReportEmailRequest(String recipientEmail, String date, String reportBody) {
        this.recipientEmail = recipientEmail;
        this.date = date;
        this.reportBody = reportBody;
    }
    
    // Getters and Setters
    public String getRecipientEmail() {
        return recipientEmail;
    }
    
    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public String getReportBody() {
        return reportBody;
    }
    
    public void setReportBody(String reportBody) {
        this.reportBody = reportBody;
    }
}
```

### 2. Create: `src/main/java/com/loretacafe/pos/controller/SalesReportController.java`

```java
package com.loretacafe.pos.controller;

import com.loretacafe.pos.dto.SalesReportEmailRequest;
import com.loretacafe.pos.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SalesReportController {
    
    @Autowired
    private EmailService emailService;
    
    /**
     * Send sales report via email
     * POST /api/send-sales-report
     * 
     * @param request Sales report email request containing recipient email, date, and report body
     * @return Success or error response
     */
    @PostMapping("/send-sales-report")
    public ResponseEntity<?> sendSalesReport(@RequestBody SalesReportEmailRequest request) {
        try {
            // Validate request
            if (request.getRecipientEmail() == null || request.getRecipientEmail().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Recipient email is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (request.getReportBody() == null || request.getReportBody().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Report body is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Create email subject
            String subject = "Loreta's Café - Sales Report for " + 
                (request.getDate() != null ? request.getDate() : "Selected Date");
            
            // Send email using existing EmailService
            emailService.sendEmail(
                request.getRecipientEmail(),
                subject,
                request.getReportBody()
            );
            
            // Return success response
            Map<String, String> response = new HashMap<>();
            response.put("message", "Sales report sent successfully to " + request.getRecipientEmail());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // Log error
            System.err.println("Error sending sales report email: " + e.getMessage());
            e.printStackTrace();
            
            // Return error response
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to send sales report: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
```

### 3. Verify: `src/main/java/com/loretacafe/pos/service/EmailService.java`

Make sure your `EmailService` has a `sendEmail` method that accepts:
- `String to` (recipient email)
- `String subject` (email subject)
- `String body` (email body)

If it doesn't exist, add this method:

```java
/**
 * Send a plain text email
 * 
 * @param to Recipient email address
 * @param subject Email subject
 * @param body Email body (plain text)
 */
public void sendEmail(String to, String subject, String body) {
    try {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(appProperties.getEmailFrom());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, false); // false = plain text, true = HTML
        
        javaMailSender.send(message);
        
        System.out.println("Email sent successfully to: " + to);
    } catch (MessagingException e) {
        System.err.println("Error sending email: " + e.getMessage());
        throw new RuntimeException("Failed to send email", e);
    }
}
```

## Testing the Integration

1. **Start your Spring Boot backend server**
2. **Ensure email configuration is set** (Gmail SMTP credentials in `application.properties`)
3. **Test from Android app:**
   - Open Sales Report screen (Admin only)
   - Click the Print/Email button (📧 icon)
   - Enter recipient email address
   - Click "Send"
   - Check recipient's inbox for the sales report email

## API Endpoint Details

- **URL**: `POST http://your-server-ip:8080/api/send-sales-report`
- **Content-Type**: `application/json`
- **Request Body**:
```json
{
  "recipientEmail": "admin@loreta.com",
  "date": "2025-01-15",
  "reportBody": "SALES REPORT SUMMARY\n====================\n\n..."
}
```

- **Success Response** (200 OK):
```json
{
  "message": "Sales report sent successfully to admin@loreta.com"
}
```

- **Error Response** (400/500):
```json
{
  "message": "Error message here"
}
```

## Notes

- The email will be sent using the same `EmailService` and Gmail SMTP configuration used for OTP emails
- Make sure your Spring Boot backend has the email service properly configured
- The Android app will handle network errors gracefully and show appropriate error messages

