package com.loretacafe.pos.data.remote.dto;

public class SalesReportEmailRequestDto {
    private String recipientEmail;
    private String date;
    private String reportBody;
    
    public SalesReportEmailRequestDto(String recipientEmail, String date, String reportBody) {
        this.recipientEmail = recipientEmail;
        this.date = date;
        this.reportBody = reportBody;
    }
    
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






