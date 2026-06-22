package com.shortlinkpro.controller;

import com.shortlinkpro.dto.AnalyticsResponseDto;
import com.shortlinkpro.entity.ClickAnalytics;
import com.shortlinkpro.entity.User;
import com.shortlinkpro.repository.ClickAnalyticsRepository;
import com.shortlinkpro.service.AnalyticsService;
import com.shortlinkpro.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserService userService;
    private final ClickAnalyticsRepository clickAnalyticsRepository;

    public AnalyticsController(AnalyticsService analyticsService, UserService userService, ClickAnalyticsRepository clickAnalyticsRepository) {
        this.analyticsService = analyticsService;
        this.userService = userService;
        this.clickAnalyticsRepository = clickAnalyticsRepository;
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

    @GetMapping
    public ResponseEntity<?> getAnalytics(Principal principal, HttpServletRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        AnalyticsResponseDto response = analyticsService.getAnalyticsForUser(user, getBaseUrl(request));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export")
    public void exportCsv(Principal principal, HttpServletResponse response) throws IOException {
        if (principal == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"shortlink_analytics.csv\"");

        List<ClickAnalytics> clicks = clickAnalyticsRepository.findAllByUser(user);
        
        PrintWriter writer = response.getWriter();
        // Write CSV Header
        writer.write("Short Code,Original URL,Click Time,IP Address,Country,Referrer,Device,Browser\n");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (ClickAnalytics click : clicks) {
            StringBuilder sb = new StringBuilder();
            sb.append(escapeCsvField(click.getUrl().getShortCode())).append(",");
            sb.append(escapeCsvField(click.getUrl().getOriginalUrl())).append(",");
            sb.append(escapeCsvField(click.getClickTime().format(formatter))).append(",");
            sb.append(escapeCsvField(click.getIpAddress())).append(",");
            sb.append(escapeCsvField(click.getCountry())).append(",");
            sb.append(escapeCsvField(click.getReferrer())).append(",");
            sb.append(escapeCsvField(click.getDevice())).append(",");
            sb.append(escapeCsvField(click.getBrowser())).append("\n");
            writer.write(sb.toString());
        }
        writer.flush();
    }

    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        String clean = field.replace("\"", "\"\"");
        if (clean.contains(",") || clean.contains("\n") || clean.contains("\"")) {
            return "\"" + clean + "\"";
        }
        return clean;
    }
}
