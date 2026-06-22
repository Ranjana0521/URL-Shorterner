package com.shortlinkpro.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "click_analytics")
public class ClickAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_id", nullable = false)
    private Url url;

    @Column(name = "click_time", nullable = false)
    private LocalDateTime clickTime;

    @Column(name = "referrer", length = 255)
    private String referrer;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "device", length = 50)
    private String device;

    @Column(name = "browser", length = 50)
    private String browser;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        this.clickTime = LocalDateTime.now();
    }

    // Default Constructor
    public ClickAnalytics() {}

    // Parameterized Constructor
    public ClickAnalytics(Url url, String referrer, String country, String device, String browser, String ipAddress) {
        this.url = url;
        this.referrer = referrer;
        this.country = country;
        this.device = device;
        this.browser = browser;
        this.ipAddress = ipAddress;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Url getUrl() { return url; }
    public void setUrl(Url url) { this.url = url; }

    public LocalDateTime getClickTime() { return clickTime; }
    public void setClickTime(LocalDateTime clickTime) { this.clickTime = clickTime; }

    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
