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

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;

@Service
@RequiredArgsConstructor
public class JiraService {

    private final UserApiSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final AtlassianOAuthService atlassianOAuthService;

    // ──────────────────────────────────────────────────────
    // Boards (projetos)
    // ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getBoards() {
        User user = getCurrentUser();
        UserApiSettings settings = settingsRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Jira não configurado. Acesse as configurações."));

        WebClient agileClient = buildJiraClient(user, settings, "/rest/agile/1.0");

        Map<String, Object> response;
        try {
            response = agileClient.get()
                    .uri("/board?type=scrum&maxResults=50")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, r ->
                            r.bodyToMono(String.class).map(body ->
                                    new IllegalStateException("Erro ao buscar boards (%d): %s"
                                            .formatted(r.statusCode().value(), body))
                            )
                    )
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new IllegalStateException("Erro ao buscar boards Jira: " + e.getMessage());
        }

        List<Map<String, Object>> rawBoards = response != null
                ? (List<Map<String, Object>>) response.get("values")
                : List.of();

        List<Map<String, Object>> boards = new ArrayList<>();
        for (Map<String, Object> board : rawBoards) {
            String projectName = null;
            String projectKey  = null;
            Object locationObj = board.get("location");
            if (locationObj instanceof Map<?, ?> loc) {
                projectName = (String) loc.get("projectName");
                projectKey  = (String) loc.get("projectKey");
            }
            if (projectName == null || projectName.isBlank()) projectName = (String) board.get("name");

            Map<String, Object> entry = new java.util.HashMap<>();
            entry.put("id",          board.get("id"));
            entry.put("name",        board.get("name"));
            entry.put("projectName", projectName);
            if (projectKey != null) entry.put("projectKey", projectKey);
            boards.add(entry);
        }

        return Map.of("boards", boards);
    }

    // ──────────────────────────────────────────────────────
    // Sprints de um board específico
    // ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSprints(String boardId) {
        User user = getCurrentUser();
        UserApiSettings settings = settingsRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Jira não configurado. Acesse as configurações."));

        WebClient agileClient = buildJiraClient(user, settings, "/rest/agile/1.0");

        // Duas chamadas separadas:
        // 1. active — garante que a sprint atual sempre aparece, independente da paginação
        // 2. closed — pega as mais recentes (Jira retorna em ordem crescente de ID, por isso
        //    buscamos um volume maior e depois limitamos as mais novas no backend)
        List<Map<String, Object>> activeSprints  = fetchSprints(agileClient, boardId, "active",  10);
        List<Map<String, Object>> closedSprints  = fetchSprints(agileClient, boardId, "closed", 200);

        // Das fechadas, pega as 9 mais recentes (maior ID)
        closedSprints.sort((a, b) -> {
            Object idA = a.get("id"), idB = b.get("id");
            if (idA == null || idB == null) return 0;
            return Integer.compare(((Number) idB).intValue(), ((Number) idA).intValue());
        });
        List<Map<String, Object>> recentClosed = closedSprints.size() > 9
                ? closedSprints.subList(0, 9) : closedSprints;

        List<Map<String, Object>> combined = new ArrayList<>(activeSprints);
        combined.addAll(recentClosed);
        return Map.of("sprints", combined);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchSprints(WebClient agileClient, String boardId,
                                                    String state, int maxResults) {
        try {
            Map<String, Object> response = agileClient.get()
                    .uri("/board/" + boardId + "/sprint?state=" + state + "&maxResults=" + maxResults)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, r ->
                            r.bodyToMono(String.class).map(body ->
                                    new IllegalStateException("Erro ao buscar sprints (%d): %s"
                                            .formatted(r.statusCode().value(), body))
                            )
                    )
                    .bodyToMono(Map.class)
                    .block();
            List<Map<String, Object>> values = response != null
                    ? (List<Map<String, Object>>) response.get("values") : null;
            return values != null ? new ArrayList<>(values) : new ArrayList<>();
        } catch (WebClientResponseException e) {
            throw new IllegalStateException("Erro ao buscar sprints: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────
    // Tickets
    // ──────────────────────────────────────────────────────

    public Map<String, Object> getTickets(String sprintId) {
        User user = getCurrentUser();
        UserApiSettings settings = settingsRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Jira não configurado. Acesse as configurações."));

        WebClient client = buildJiraClient(user, settings, "/rest/api/3");

        String jql = (sprintId != null && !sprintId.isBlank())
                ? "assignee = currentUser() AND sprint = " + sprintId + " ORDER BY updated DESC"
                : "assignee = currentUser() AND sprint in openSprints() ORDER BY updated DESC";

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

    // ──────────────────────────────────────────────────────
    // Worklog
    // ──────────────────────────────────────────────────────

    public Map<String, Object> logWork(String issueKey, String timeSpent, String comment, String started) {
        User user = getCurrentUser();
        UserApiSettings settings = settingsRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Jira não configurado. Acesse as configurações."));

        WebClient client = buildJiraClient(user, settings, "/rest/api/3");

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("timeSpent", timeSpent);
        if (started != null && !started.isBlank()) body.put("started", started);
        if (comment != null && !comment.isBlank()) {
            body.put("comment", Map.of(
                    "type", "doc",
                    "version", 1,
                    "content", java.util.List.of(Map.of(
                            "type", "paragraph",
                            "content", java.util.List.of(Map.of("type", "text", "text", comment))
                    ))
            ));
        }

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

    // ──────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────

    private WebClient buildJiraClient(User user, UserApiSettings settings, String apiPath) {
        String accessToken = settings.getAtlassianAccessToken() != null
                ? atlassianOAuthService.resolveAccessToken(user, settings)
                : null;

        if (accessToken != null && settings.getAtlassianCloudId() != null) {
            return WebClient.builder()
                    .baseUrl("https://api.atlassian.com/ex/jira/" + settings.getAtlassianCloudId() + apiPath)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .build();
        }

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

        return WebClient.builder()
                .baseUrl("https://" + domain + apiPath)
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
