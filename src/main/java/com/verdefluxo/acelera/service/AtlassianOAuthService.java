package com.verdefluxo.acelera.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.verdefluxo.acelera.model.entity.User;
import com.verdefluxo.acelera.model.entity.UserApiSettings;
import com.verdefluxo.acelera.repository.UserApiSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AtlassianOAuthService {

    @Value("${atlassian.oauth.redirect-uri}")
    private String redirectUri;

    private final UserApiSettingsRepository settingsRepository;

    private final WebClient webClient = WebClient.create();

    private static final String AUTH_ENDPOINT       = "https://auth.atlassian.com/authorize";
    private static final String TOKEN_ENDPOINT      = "https://auth.atlassian.com/oauth/token";
    private static final String RESOURCES_ENDPOINT  = "https://api.atlassian.com/oauth/token/accessible-resources";
    private static final String SCOPES              = "read:jira-work write:jira-work read:jira-user offline_access";

    // ──────────────────────────────────────────────────────
    // Internal records
    // ──────────────────────────────────────────────────────

    public record AtlassianTokenResponse(
            @JsonProperty("access_token")  String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in")    Long expiresIn,
            @JsonProperty("scope")         String scope
    ) {}

    public record AtlassianCloudResource(
            @JsonProperty("id")   String id,
            @JsonProperty("url")  String url,
            @JsonProperty("name") String name
    ) {}

    // ──────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────

    public String buildAuthorizationUrl(String clientId) {
        return UriComponentsBuilder.fromUriString(AUTH_ENDPOINT)
                .queryParam("audience", "api.atlassian.com")
                .queryParam("client_id", clientId)
                .queryParam("scope", SCOPES)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", "atlassian-oauth")
                .queryParam("response_type", "code")
                .queryParam("prompt", "consent")
                .build()
                .toUriString();
    }

    public AtlassianTokenResponse exchangeCode(String code, String clientId, String clientSecret) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("redirect_uri", redirectUri);

        return webClient.post()
                .uri(TOKEN_ENDPOINT)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(params))
                .retrieve()
                .bodyToMono(AtlassianTokenResponse.class)
                .block();
    }

    public AtlassianTokenResponse refreshToken(String refreshToken, String clientId, String clientSecret) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("refresh_token", refreshToken);

        return webClient.post()
                .uri(TOKEN_ENDPOINT)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(params))
                .retrieve()
                .bodyToMono(AtlassianTokenResponse.class)
                .block();
    }

    /**
     * Returns a valid access token, refreshing if necessary.
     * Persists updated settings back to DB when a refresh occurs.
     */
    public String resolveAccessToken(User user, UserApiSettings settings) {
        if (settings.getAtlassianAccessToken() == null) return null;

        boolean expired = settings.getAtlassianTokenExpiry() != null
                && LocalDateTime.now().isAfter(settings.getAtlassianTokenExpiry().minusMinutes(5));

        if (!expired) return settings.getAtlassianAccessToken();

        AtlassianTokenResponse refreshed = refreshToken(
                settings.getAtlassianRefreshToken(),
                settings.getAtlassianClientId(),
                settings.getAtlassianClientSecret()
        );

        settings.setAtlassianAccessToken(refreshed.accessToken());
        if (refreshed.refreshToken() != null) {
            settings.setAtlassianRefreshToken(refreshed.refreshToken());
        }
        if (refreshed.expiresIn() != null) {
            settings.setAtlassianTokenExpiry(LocalDateTime.now().plusSeconds(refreshed.expiresIn()));
        }
        settingsRepository.save(settings);

        return refreshed.accessToken();
    }

    public AtlassianCloudResource getCloudResource(String accessToken) {
        List<AtlassianCloudResource> resources = webClient.get()
                .uri(RESOURCES_ENDPOINT)
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToFlux(AtlassianCloudResource.class)
                .collectList()
                .block();

        if (resources == null || resources.isEmpty()) {
            throw new IllegalStateException("Nenhuma instância Atlassian acessível encontrada.");
        }
        return resources.get(0);
    }
}
