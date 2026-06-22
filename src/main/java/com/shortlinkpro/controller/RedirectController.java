package com.shortlinkpro.controller;

import com.shortlinkpro.entity.Url;
import com.shortlinkpro.repository.UrlRepository;
import com.shortlinkpro.service.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Controller
public class RedirectController {

    private final UrlRepository urlRepository;
    private final AnalyticsService analyticsService;
    private final PasswordEncoder passwordEncoder;

    public RedirectController(UrlRepository urlRepository, AnalyticsService analyticsService, PasswordEncoder passwordEncoder) {
        this.urlRepository = urlRepository;
        this.analyticsService = analyticsService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/r/{shortCode}")
    public void handleRedirect(@PathVariable String shortCode,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        Optional<Url> urlOpt = urlRepository.findByShortCode(shortCode);
        if (urlOpt.isEmpty()) {
            response.sendRedirect("/index.html?error=not_found");
            return;
        }

        Url url = urlOpt.get();

        if (url.isExpired()) {
            url.setStatus("EXPIRED");
            urlRepository.save(url);
            response.sendRedirect("/index.html?error=expired");
            return;
        }

        if ("DISABLED".equalsIgnoreCase(url.getStatus())) {
            response.sendRedirect("/index.html?error=disabled");
            return;
        }

        // If URL requires a password, redirect to password-protect entry form page
        if (url.getPassword() != null && !url.getPassword().isBlank()) {
            response.sendRedirect("/password-protect.html?code=" + shortCode);
            return;
        }

        // Standard Direct Click Logging
        String ipAddress = request.getRemoteAddr();
        String referrer = request.getHeader("Referer");
        String userAgent = request.getHeader("User-Agent");

        analyticsService.logClick(url, referrer, ipAddress, userAgent);

        // 302 Found Redirection
        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        response.setHeader("Location", url.getOriginalUrl());
    }

    @PostMapping("/r/{shortCode}/auth")
    @ResponseBody
    public ResponseEntity<?> verifyPassword(@PathVariable String shortCode,
                                            @RequestBody Map<String, String> payload,
                                            HttpServletRequest request) {
        Optional<Url> urlOpt = urlRepository.findByShortCode(shortCode);
        if (urlOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "URL not found"));
        }

        Url url = urlOpt.get();
        if (url.isExpired()) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", "URL has expired"));
        }
        if ("DISABLED".equalsIgnoreCase(url.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "URL is disabled"));
        }

        String password = payload.get("password");
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
        }

        if (passwordEncoder.matches(password, url.getPassword())) {
            String ipAddress = request.getRemoteAddr();
            String referrer = request.getHeader("Referer");
            String userAgent = request.getHeader("User-Agent");
            analyticsService.logClick(url, referrer, ipAddress, userAgent);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "originalUrl", url.getOriginalUrl()
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Incorrect password"));
        }
    }
}
