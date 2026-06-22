package com.shortlinkpro.controller;

import com.shortlinkpro.dto.BulkUrlRequestDto;
import com.shortlinkpro.dto.UrlRequestDto;
import com.shortlinkpro.dto.UrlResponseDto;
import com.shortlinkpro.entity.User;
import com.shortlinkpro.service.UrlService;
import com.shortlinkpro.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/urls")
public class UrlController {

    private final UrlService urlService;
    private final UserService userService;

    public UrlController(UrlService urlService, UserService userService) {
        this.urlService = urlService;
        this.userService = userService;
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        
        StringBuilder builder = new StringBuilder();
        builder.append(scheme).append("://").append(serverName);
        if (serverPort != 80 && serverPort != 443) {
            builder.append(":").append(serverPort);
        }
        return builder.toString();
    }

    @PostMapping
    public ResponseEntity<?> createUrl(@RequestBody @Valid UrlRequestDto requestDto,
                                       Principal principal, HttpServletRequest request) {
        User user = null;
        if (principal != null) {
            user = userService.findByUsername(principal.getName()).orElse(null);
        }
        try {
            UrlResponseDto response = urlService.createShortUrl(requestDto, user, getBaseUrl(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUrls(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            Principal principal, HttpServletRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        
        Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<UrlResponseDto> urls = urlService.getUrls(user, search, pageRequest, getBaseUrl(request));
        return ResponseEntity.ok(urls);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUrl(@PathVariable Long id, @RequestBody UrlRequestDto requestDto,
                                       Principal principal, HttpServletRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        try {
            UrlResponseDto response = urlService.updateUrl(id, requestDto, user, getBaseUrl(request));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUrl(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        try {
            urlService.deleteUrl(id, user);
            return ResponseEntity.ok(Map.of("message", "URL deleted successfully"));
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<?> toggleUrlStatus(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        try {
            urlService.toggleUrlStatus(id, user);
            return ResponseEntity.ok(Map.of("message", "URL status toggled successfully"));
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> shortenBulk(@RequestBody @Valid BulkUrlRequestDto bulkRequest,
                                         Principal principal, HttpServletRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        String baseUrl = getBaseUrl(request);
        
        List<UrlResponseDto> responses = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (String originalUrl : bulkRequest.getOriginalUrls()) {
            if (originalUrl == null || originalUrl.isBlank()) continue;
            try {
                UrlRequestDto singleDto = new UrlRequestDto();
                singleDto.setOriginalUrl(originalUrl.trim());
                UrlResponseDto res = urlService.createShortUrl(singleDto, user, baseUrl);
                responses.add(res);
            } catch (Exception e) {
                errors.add("Failed to shorten: " + originalUrl + " - " + e.getMessage());
            }
        }
        
        return ResponseEntity.ok(Map.of(
                "shortened", responses,
                "errors", errors
        ));
    }

    @GetMapping("/{id}/health")
    public ResponseEntity<?> checkHealth(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        try {
            String health = urlService.getUrlHealth(id, user);
            return ResponseEntity.ok(Map.of("status", health));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
