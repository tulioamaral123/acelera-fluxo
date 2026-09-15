package com.verdefluxo.acelera.service;

import com.verdefluxo.acelera.model.dto.GoogleTokenResponse;
import com.verdefluxo.acelera.model.dto.GoogleUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GoogleOAuthService {

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${google.oauth.scopes}")
    private String scopes;

    private final WebClient webClient = WebClient.create();

    private static final String AUTH_ENDPOINT  = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String TOKEN_REVOKE_ENDPOINT = "https://oauth2.googleapis.com/revoke";

    public String buildAuthorizationUrl() {
        return UriComponentsBuilder.fromUriString(AUTH_ENDPOINT)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", scopes.replace(",", " "))
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build()
                .toUriString();
    }

    public GoogleTokenResponse exchangeCodeForTokens(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        return webClient.post()
                .uri(TOKEN_ENDPOINT)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(params))
                .retrieve()
                .bodyToMono(GoogleTokenResponse.class)
                .block();
    }

    public GoogleTokenResponse refreshAccessToken(String refreshToken) {
        return refreshAccessTokenWith(refreshToken, clientId, clientSecret);
    }

    public GoogleTokenResponse refreshAccessTokenWith(String refreshToken, String overrideClientId, String overrideClientSecret) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("refresh_token", refreshToken);
        params.add("client_id", overrideClientId != null ? overrideClientId : clientId);
        params.add("client_secret", overrideClientSecret != null ? overrideClientSecret : clientSecret);
        params.add("grant_type", "refresh_token");

        return webClient.post()
                .uri(TOKEN_ENDPOINT)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(params))
                .retrieve()
                .bodyToMono(GoogleTokenResponse.class)
                .block();
    }

    public String getClientId() { return clientId; }
    public String getClientSecret() { return clientSecret; }

    public GoogleUserInfo getUserInfo(String accessToken) {
        return webClient.get()
                .uri(USERINFO_ENDPOINT)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(GoogleUserInfo.class)
                .block();
    }
}
