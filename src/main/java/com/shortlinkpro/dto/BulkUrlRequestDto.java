package com.shortlinkpro.dto;

import java.util.List;

public class BulkUrlRequestDto {

    private List<String> originalUrls;

    public List<String> getOriginalUrls() {
        return originalUrls;
    }

    public void setOriginalUrls(List<String> originalUrls) {
        this.originalUrls = originalUrls;
    }
}
