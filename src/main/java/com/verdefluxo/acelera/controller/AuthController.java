package com.verdefluxo.acelera.controller;

import com.verdefluxo.acelera.model.dto.AuthResponse;
import com.verdefluxo.acelera.model.dto.LoginRequest;
import com.verdefluxo.acelera.model.dto.RegisterRequest;
import com.verdefluxo.acelera.service.AuthService;
import com.verdefluxo.acelera.service.GoogleOAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GoogleOAuthService googleOAuthService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /** Returns the Google OAuth authorization URL for the frontend to redirect to. */
    @GetMapping("/google")
    public ResponseEntity<Map<String, String>> googleAuthUrl() {
        String url = googleOAuthService.buildAuthorizationUrl();
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * Angular calls this endpoint with the code received from Google.
     * Returns JSON with the app JWT — token never goes through URL params.
     */
    @PostMapping("/google/callback")
    public ResponseEntity<AuthResponse> googleCallback(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(authService.loginWithGoogle(code));
    }
}
