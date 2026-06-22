package com.shortlinkpro.dto;

import java.util.List;
import java.util.Map;

public class AnalyticsResponseDto {

    private long totalClicks;
    private long totalLinks;
    private long activeLinks;
    private long expiredLinks;
    
    private Map<String, Long> clicksByDate;
    private Map<String, Long> clicksByMonth;
    private Map<String, Long> clicksByDevice;
    private Map<String, Long> clicksByBrowser;
    private Map<String, Long> clicksByCountry;
    private Map<String, Long> clicksByReferrer;
    
    private List<UrlClicksDto> topLinks;
    private List<ClickLogDto> recentClicks;

    // Getters and Setters
    public List<ClickLogDto> getRecentClicks() { return recentClicks; }
    public void setRecentClicks(List<ClickLogDto> recentClicks) { this.recentClicks = recentClicks; }
    public long getTotalClicks() { return totalClicks; }
    public void setTotalClicks(long totalClicks) { this.totalClicks = totalClicks; }

    public long getTotalLinks() { return totalLinks; }
    public void setTotalLinks(long totalLinks) { this.totalLinks = totalLinks; }

    public long getActiveLinks() { return activeLinks; }
    public void setActiveLinks(long activeLinks) { this.activeLinks = activeLinks; }

    public long getExpiredLinks() { return expiredLinks; }
    public void setExpiredLinks(long expiredLinks) { this.expiredLinks = expiredLinks; }

    public Map<String, Long> getClicksByDate() { return clicksByDate; }
    public void setClicksByDate(Map<String, Long> clicksByDate) { this.clicksByDate = clicksByDate; }

    public Map<String, Long> getClicksByMonth() { return clicksByMonth; }
    public void setClicksByMonth(Map<String, Long> clicksByMonth) { this.clicksByMonth = clicksByMonth; }

    public Map<String, Long> getClicksByDevice() { return clicksByDevice; }
    public void setClicksByDevice(Map<String, Long> clicksByDevice) { this.clicksByDevice = clicksByDevice; }

    public Map<String, Long> getClicksByBrowser() { return clicksByBrowser; }
    public void setClicksByBrowser(Map<String, Long> clicksByBrowser) { this.clicksByBrowser = clicksByBrowser; }

    public Map<String, Long> getClicksByCountry() { return clicksByCountry; }
    public void setClicksByCountry(Map<String, Long> clicksByCountry) { this.clicksByCountry = clicksByCountry; }

    public Map<String, Long> getClicksByReferrer() { return clicksByReferrer; }
    public void setClicksByReferrer(Map<String, Long> clicksByReferrer) { this.clicksByReferrer = clicksByReferrer; }

    public List<UrlClicksDto> getTopLinks() { return topLinks; }
    public void setTopLinks(List<UrlClicksDto> topLinks) { this.topLinks = topLinks; }
}
