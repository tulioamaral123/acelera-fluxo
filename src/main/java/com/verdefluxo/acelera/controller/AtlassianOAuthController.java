package com.verdefluxo.acelera.controller;

import com.verdefluxo.acelera.model.entity.User;
import com.verdefluxo.acelera.model.entity.UserApiSettings;
import com.verdefluxo.acelera.repository.UserApiSettingsRepository;
import com.verdefluxo.acelera.repository.UserRepository;
import com.verdefluxo.acelera.service.AtlassianOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/atlassian")
@RequiredArgsConstructor
public class AtlassianOAuthController {

    private final AtlassianOAuthService atlassianOAuthService;
    private final UserApiSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    /** Returns the Atlassian OAuth authorization URL built from user's saved clientId. */
    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> authUrl() {
        UserApiSettings settings = getSettings();
        if (settings.getAtlassianClientId() == null || settings.getAtlassianClientId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Atlassian Client ID não configurado. Salve as configurações antes de conectar."));
        }
        String url = atlassianOAuthService.buildAuthorizationUrl(settings.getAtlassianClientId());
        return ResponseEntity.ok(Map.of("url", url));
    }

    /** Receives the OAuth code, exchanges it for tokens, fetches cloudId, saves everything. */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> callback(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Código OAuth ausente."));
        }

        UserApiSettings settings = getSettings();

        if (settings.getAtlassianClientId() == null || settings.getAtlassianClientSecret() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Client ID e Secret não configurados."));
        }

        AtlassianOAuthService.AtlassianTokenResponse tokens = atlassianOAuthService.exchangeCode(
                code, settings.getAtlassianClientId(), settings.getAtlassianClientSecret()
        );

        AtlassianOAuthService.AtlassianCloudResource cloud = atlassianOAuthService.getCloudResource(tokens.accessToken());

        // Fetch email from Jira myself endpoint
        String email = fetchAtlassianEmail(tokens.accessToken(), cloud.id());

        settings.setAtlassianAccessToken(tokens.accessToken());
        settings.setAtlassianRefreshToken(tokens.refreshToken());
        settings.setAtlassianCloudId(cloud.id());
        settings.setAtlassianEmail(email);
        if (tokens.expiresIn() != null) {
            settings.setAtlassianTokenExpiry(LocalDateTime.now().plusSeconds(tokens.expiresIn()));
        }
        settingsRepository.save(settings);

        return ResponseEntity.ok(Map.of("connected", true, "email", email != null ? email : ""));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        UserApiSettings settings = getSettings();
        boolean connected = settings.getAtlassianAccessToken() != null;
        return ResponseEntity.ok(Map.of(
                "connected", connected,
                "email", connected && settings.getAtlassianEmail() != null ? settings.getAtlassianEmail() : ""
        ));
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<Void> disconnect() {
        UserApiSettings settings = getSettings();
        settings.setAtlassianAccessToken(null);
        settings.setAtlassianRefreshToken(null);
        settings.setAtlassianCloudId(null);
        settings.setAtlassianTokenExpiry(null);
        settings.setAtlassianEmail(null);
        settingsRepository.save(settings);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────

    private UserApiSettings getSettings() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return settingsRepository.findByUser(user)
                .orElseGet(() -> {
                    UserApiSettings s = UserApiSettings.builder().user(user).build();
                    return settingsRepository.save(s);
                });
    }

    @SuppressWarnings("unchecked")
    private String fetchAtlassianEmail(String accessToken, String cloudId) {
        try {
            org.springframework.web.reactive.function.client.WebClient client =
                    org.springframework.web.reactive.function.client.WebClient.create();
            Map<String, Object> myself = client.get()
                    .uri("https://api.atlassian.com/ex/jira/" + cloudId + "/rest/api/3/myself")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return myself != null ? (String) myself.get("emailAddress") : null;
        } catch (Exception e) {
            return null;
        }
    }
}
