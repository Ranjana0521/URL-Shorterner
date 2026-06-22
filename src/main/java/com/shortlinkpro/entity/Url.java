package com.shortlinkpro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "urls")
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Original URL is required")
    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "short_code", nullable = false, unique = true, length = 20)
    private String shortCode;

    @Column(name = "custom_alias", unique = true, length = 50)
    private String customAlias;

    @Column(nullable = false)
    private int clicks = 0;

    @Lob
    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "click_limit")
    private Integer clickLimit;

    @Column(length = 100)
    private String password; // BCrypt hash of URL password if protected

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, EXPIRED, DISABLED

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "url", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClickAnalytics> analytics = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    // Default Constructor
    public Url() {}

    // Parameterized Constructor
    public Url(String originalUrl, String shortCode, User user) {
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.user = user;
    }

    // Check if the URL is expired based on current time or click limits
    public boolean isExpired() {
        if ("EXPIRED".equalsIgnoreCase(status)) {
            return true;
        }
        if (expiryDate != null && expiryDate.isBefore(LocalDateTime.now())) {
            return true;
        }
        if (clickLimit != null && clicks >= clickLimit) {
            return true;
        }
        return false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getCustomAlias() { return customAlias; }
    public void setCustomAlias(String customAlias) { this.customAlias = customAlias; }

    public int getClicks() { return clicks; }
    public void setClicks(int clicks) { this.clicks = clicks; }
    public void incrementClicks() { this.clicks++; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public Integer getClickLimit() { return clickLimit; }
    public void setClickLimit(Integer clickLimit) { this.clickLimit = clickLimit; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<ClickAnalytics> getAnalytics() { return analytics; }
    public void setAnalytics(List<ClickAnalytics> analytics) { this.analytics = analytics; }
}
