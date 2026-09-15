package com.verdefluxo.acelera.service;

import com.verdefluxo.acelera.model.entity.User;
import com.verdefluxo.acelera.model.entity.UserApiSettings;
import com.verdefluxo.acelera.repository.UserApiSettingsRepository;
import com.verdefluxo.acelera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BitbucketService {

    private final UserApiSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    public Map<String, Object> getRepositories() {
        UserApiSettings settings = getSettings();
        WebClient client = buildClient(settings);

        return client.get()
                .uri("/repositories/" + settings.getBitbucketWorkspace())
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())))
                .block();
    }

    public Map<String, Object> getPullRequests(String repoSlug) {
        UserApiSettings settings = getSettings();
        WebClient client = buildClient(settings);

        return client.get()
                .uri("/repositories/" + settings.getBitbucketWorkspace() + "/" + repoSlug + "/pullrequests")
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())))
                .block();
    }

    private UserApiSettings getSettings() {
        User user = getCurrentUser();
        UserApiSettings settings = settingsRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Bitbucket não configurado. Acesse as configurações."));

        if (settings.getBitbucketWorkspace() == null || settings.getBitbucketToken() == null) {
            throw new IllegalStateException("Bitbucket não configurado. Acesse as configurações.");
        }
        return settings;
    }

    private WebClient buildClient(UserApiSettings settings) {
        String credentials = Base64.getEncoder().encodeToString(
                ("x-token-auth:" + settings.getBitbucketToken()).getBytes()
        );
        return WebClient.builder()
                .baseUrl("https://api.bitbucket.org/2.0")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
