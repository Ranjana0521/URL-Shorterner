package com.shortlinkpro.controller;

import com.shortlinkpro.dto.UserLoginDto;
import com.shortlinkpro.dto.UserRegisterDto;
import com.shortlinkpro.entity.User;
import com.shortlinkpro.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid UserRegisterDto registerDto) {
        try {
            User user = userService.registerUser(registerDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "User registered successfully",
                    "username", user.getUsername(),
                    "email", user.getEmail()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid UserLoginDto loginDto, HttpServletRequest request) {
        try {
            UserDetails userDetails = userService.loadUserByUsername(loginDto.getUsername());
            if (passwordEncoder.matches(loginDto.getPassword(), userDetails.getPassword())) {
                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(token);
                
                HttpSession session = request.getSession(true);
                session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
                
                User user = userService.findByUsername(userDetails.getUsername())
                        .or(() -> userService.findByEmail(userDetails.getUsername()))
                        .orElseThrow();
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Login successful");
                response.put("username", user.getUsername());
                response.put("email", user.getEmail());
                response.put("apiKey", user.getApiKey());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
            }
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getAuthStatus(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("authenticated", false));
        }
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("authenticated", false));
        }
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("apiKey", user.getApiKey());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> payload, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized."));
        }
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        try {
            User updated = userService.updateProfile(user, payload.get("email"), payload.get("password"));
            return ResponseEntity.ok(Map.of(
                    "message", "Profile updated successfully",
                    "email", updated.getEmail()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/apikey/regenerate")
    public ResponseEntity<?> regenerateApiKey(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized."));
        }
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String newKey = userService.regenerateApiKey(user);
        return ResponseEntity.ok(Map.of(
                "message", "API Key regenerated successfully",
                "apiKey", newKey
        ));
    }
}
