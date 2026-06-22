package com.shortlinkpro.dto;

public class ClickLogDto {

    private String shortCode;
    private String country;
    private String device;
    private String browser;
    private String clickTime;

    public ClickLogDto() {}

    public ClickLogDto(String shortCode, String country, String device, String browser, String clickTime) {
        this.shortCode = shortCode;
        this.country = country;
        this.device = device;
        this.browser = browser;
        this.clickTime = clickTime;
    }

    // Getters and Setters
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    public String getClickTime() { return clickTime; }
    public void setClickTime(String clickTime) { this.clickTime = clickTime; }
}
