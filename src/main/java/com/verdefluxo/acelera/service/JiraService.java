package com.verdefluxo.acelera.service;

import com.verdefluxo.acelera.model.entity.User;
import com.verdefluxo.acelera.model.entity.UserApiSettings;
import com.verdefluxo.acelera.repository.UserApiSettingsRepository;
import com.verdefluxo.acelera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;

import org.springframework.http.MediaType;

@Service
@RequiredArgsConstructor
public class JiraService {

    private final UserApiSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    public Map<String, Object> getWeeklyTickets() {
        User user = getCurrentUser();
        UserApiSettings settings = settingsRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Jira não configurado. Acesse as configurações."));

        if (settings.getJiraDomain() == null || settings.getJiraDomain().isBlank()
                || settings.getJiraToken() == null || settings.getJiraToken().isBlank()) {
            throw new IllegalStateException("Jira não configurado. Acesse as configurações.");
        }

        String credentials = Base64.getEncoder().encodeToString(
                (settings.getJiraEmail() + ":" + settings.getJiraToken()).getBytes()
        );

        LocalDate since = LocalDate.now().minusDays(15);

        String jql = String.format(
                "assignee = currentUser() AND updated >= \"%s\" ORDER BY updated DESC",
                since
        );

        String domain = settings.getJiraDomain()
                .replaceAll("^https?://", "")  // remove protocolo se o usuário incluiu
                .replaceAll("/+$", "");         // remove barras no final

        WebClient client = WebClient.builder()
                .baseUrl("https://" + domain + "/rest/api/3")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();

        try {
            return client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/jql")
                            .queryParam("jql", jql)
                            .queryParam("fields", "summary,status,priority,assignee,updated,timetracking,worklog")
                            .queryParam("maxResults", "50")
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response ->
                            response.bodyToMono(String.class).map(body ->
                                    new IllegalStateException("Erro Jira (%d): %s".formatted(response.statusCode().value(), body))
                            )
                    )
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            response.bodyToMono(String.class).map(body ->
                                    new IllegalStateException("Erro no servidor Jira (%d)".formatted(response.statusCode().value()))
                            )
                    )
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new IllegalStateException("Falha na conexão com Jira: " + e.getMessage());
        }
    }

    public Map<String, Object> logWork(String issueKey, String timeSpent, String comment) {
        User user = getCurrentUser();
        UserApiSettings settings = settingsRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Jira não configurado. Acesse as configurações."));

        if (settings.getJiraDomain() == null || settings.getJiraDomain().isBlank()
                || settings.getJiraToken() == null || settings.getJiraToken().isBlank()) {
            throw new IllegalStateException("Jira não configurado. Acesse as configurações.");
        }

        String credentials = Base64.getEncoder().encodeToString(
                (settings.getJiraEmail() + ":" + settings.getJiraToken()).getBytes()
        );

        String domain = settings.getJiraDomain()
                .replaceAll("^https?://", "")
                .replaceAll("/+$", "");

        WebClient client = WebClient.builder()
                .baseUrl("https://" + domain + "/rest/api/3")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();

        Map<String, Object> body = comment != null && !comment.isBlank()
                ? Map.of("timeSpent", timeSpent, "comment", Map.of(
                        "type", "doc",
                        "version", 1,
                        "content", java.util.List.of(Map.of(
                                "type", "paragraph",
                                "content", java.util.List.of(Map.of("type", "text", "text", comment))
                        ))
                  ))
                : Map.of("timeSpent", timeSpent);

        try {
            return client.post()
                    .uri("/issue/" + issueKey + "/worklog")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response ->
                            response.bodyToMono(String.class).map(b ->
                                    new IllegalStateException("Erro Jira (%d): %s".formatted(response.statusCode().value(), b))
                            )
                    )
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new IllegalStateException("Falha ao registrar horas: " + e.getMessage());
        }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
