package com.shortlinkpro.service;

import com.shortlinkpro.dto.UrlRequestDto;
import com.shortlinkpro.dto.UrlResponseDto;
import com.shortlinkpro.entity.Url;
import com.shortlinkpro.entity.User;
import com.shortlinkpro.repository.UrlRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final QrCodeService qrCodeService;
    private final PasswordEncoder passwordEncoder;
    private static final String BASE62_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 7;
    private final Random random = new Random();

    public UrlService(UrlRepository urlRepository, QrCodeService qrCodeService, PasswordEncoder passwordEncoder) {
        this.urlRepository = urlRepository;
        this.qrCodeService = qrCodeService;
        this.passwordEncoder = passwordEncoder;
    }

    private String generateShortCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(BASE62_CHARACTERS.charAt(random.nextInt(BASE62_CHARACTERS.length())));
        }
        return sb.toString();
    }

    private String getUniqueShortCode() {
        String code;
        do {
            code = generateShortCode();
        } while (urlRepository.findByShortCode(code).isPresent());
        return code;
    }

    @Transactional
    public UrlResponseDto createShortUrl(UrlRequestDto request, User user, String baseUrl) {
        Url url = new Url();
        url.setOriginalUrl(request.getOriginalUrl());
        url.setUser(user);

        // Check custom alias
        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            String alias = request.getCustomAlias().trim();
            // Validate alias characters
            if (!alias.matches("^[a-zA-Z0-9_-]+$")) {
                throw new IllegalArgumentException("Custom alias contains invalid characters. Only alphanumeric, dashes, and underscores allowed.");
            }
            if (urlRepository.findByShortCode(alias).isPresent() || urlRepository.findByCustomAlias(alias).isPresent()) {
                throw new IllegalArgumentException("This custom alias or short code is already taken.");
            }
            url.setCustomAlias(alias);
            url.setShortCode(alias); // For easy query, short_code serves as the identifier in the URL path
        } else {
            url.setShortCode(getUniqueShortCode());
        }

        // Configure expiration conditions
        if (request.getExpiryDate() != null) {
            if (request.getExpiryDate().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Expiry date must be in the future.");
            }
            url.setExpiryDate(request.getExpiryDate());
        }

        if (request.getClickLimit() != null) {
            if (request.getClickLimit() <= 0) {
                throw new IllegalArgumentException("Click limit must be greater than zero.");
            }
            url.setClickLimit(request.getClickLimit());
        }

        // Lock link with password if supplied
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            url.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Save first to get the generated ID and stable short code path
        url = urlRepository.save(url);

        // Generate and save QR Code
        String targetRedirectUrl = baseUrl + "/r/" + url.getShortCode();
        String qrCodeBase64 = qrCodeService.generateQrCodeBase64(targetRedirectUrl, 300, 300);
        url.setQrCode(qrCodeBase64);

        url = urlRepository.save(url);
        return convertToDto(url, baseUrl);
    }

    public Page<UrlResponseDto> getUrls(User user, String search, Pageable pageable, String baseUrl) {
        Page<Url> urlPage;
        if (search != null && !search.isBlank()) {
            urlPage = urlRepository.searchUrls(user, search.trim(), pageable);
        } else {
            urlPage = urlRepository.findByUser(user, pageable);
        }

        // Update expiry state on read to be consistent
        return urlPage.map(url -> {
            if (url.isExpired() && !"EXPIRED".equalsIgnoreCase(url.getStatus())) {
                url.setStatus("EXPIRED");
                urlRepository.save(url);
            }
            return convertToDto(url, baseUrl);
        });
    }

    @Transactional
    public UrlResponseDto updateUrl(Long id, UrlRequestDto request, User user, String baseUrl) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("URL not found."));

        if (url.getUser() == null || !url.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized action.");
        }

        // Modify expiry
        if (request.getExpiryDate() != null) {
            if (request.getExpiryDate().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Expiry date must be in the future.");
            }
            url.setExpiryDate(request.getExpiryDate());
            if (url.getStatus().equals("EXPIRED")) {
                url.setStatus("ACTIVE"); // Reactivate if future date set
            }
        } else {
            url.setExpiryDate(null);
        }

        // Modify click limits
        if (request.getClickLimit() != null) {
            if (request.getClickLimit() <= 0) {
                throw new IllegalArgumentException("Click limit must be greater than zero.");
            }
            url.setClickLimit(request.getClickLimit());
        } else {
            url.setClickLimit(null);
        }

        // Modify password
        if (request.getPassword() != null) {
            if (request.getPassword().isBlank()) {
                url.setPassword(null); // Clear password protection
            } else {
                url.setPassword(passwordEncoder.encode(request.getPassword()));
            }
        }

        url = urlRepository.save(url);
        return convertToDto(url, baseUrl);
    }

    @Transactional
    public void deleteUrl(Long id, User user) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("URL not found."));

        if (url.getUser() == null || !url.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized action.");
        }

        urlRepository.delete(url);
    }

    @Transactional
    public void toggleUrlStatus(Long id, User user) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("URL not found."));

        if (url.getUser() == null || !url.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized action.");
        }

        if ("ACTIVE".equalsIgnoreCase(url.getStatus())) {
            url.setStatus("DISABLED");
        } else {
            // Re-evaluating status
            if (url.isExpired()) {
                url.setStatus("EXPIRED");
            } else {
                url.setStatus("ACTIVE");
            }
        }
        urlRepository.save(url);
    }

    public String checkLinkHealth(String originalUrl) {
        try {
            URL url = new URL(originalUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000); // 3 seconds timeout
            connection.setReadTimeout(3000);
            connection.setRequestProperty("User-Agent", "ShortLinkPro-HealthMonitor/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 400) {
                return "HEALTHY";
            } else {
                return "BROKEN (" + responseCode + ")";
            }
        } catch (IOException e) {
            return "BROKEN (Unreachable)";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    @Transactional
    public String getUrlHealth(Long id, User user) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("URL not found."));
        if (url.getUser() == null || !url.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized action.");
        }
        return checkLinkHealth(url.getOriginalUrl());
    }

    public UrlResponseDto convertToDto(Url url, String baseUrl) {
        UrlResponseDto dto = new UrlResponseDto();
        dto.setId(url.getId());
        dto.setOriginalUrl(url.getOriginalUrl());
        dto.setShortCode(url.getShortCode());
        dto.setShortUrl(baseUrl + "/r/" + url.getShortCode());
        dto.setCustomAlias(url.getCustomAlias());
        dto.setClicks(url.getClicks());
        dto.setQrCode(url.getQrCode());
        dto.setExpiryDate(url.getExpiryDate());
        dto.setClickLimit(url.getClickLimit());
        dto.setPasswordProtected(url.getPassword() != null && !url.getPassword().isBlank());
        
        // Auto update status representation
        if (url.isExpired() && !"EXPIRED".equalsIgnoreCase(url.getStatus())) {
            dto.setStatus("EXPIRED");
        } else {
            dto.setStatus(url.getStatus());
        }
        dto.setCreatedAt(url.getCreatedAt());
        return dto;
    }
}
