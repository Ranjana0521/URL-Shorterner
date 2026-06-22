package com.shortlinkpro.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class UrlRequestDto {

    @NotBlank(message = "Original URL is required")
    private String originalUrl;

    private String customAlias;
    private LocalDateTime expiryDate;
    private Integer clickLimit;
    private String password;

    // Getters and Setters
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public String getCustomAlias() { return customAlias; }
    public void setCustomAlias(String customAlias) { this.customAlias = customAlias; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public Integer getClickLimit() { return clickLimit; }
    public void setClickLimit(Integer clickLimit) { this.clickLimit = clickLimit; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
