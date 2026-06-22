package com.shortlinkpro.dto;

public class UrlClicksDto {
    private String shortCode;
    private String originalUrl;
    private int clicks;

    public UrlClicksDto() {}

    public UrlClicksDto(String shortCode, String originalUrl, int clicks) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.clicks = clicks;
    }

    // Getters and Setters
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public int getClicks() { return clicks; }
    public void setClicks(int clicks) { this.clicks = clicks; }
}
