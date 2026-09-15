package com.verdefluxo.acelera.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.verdefluxo.acelera.model.dto.CalendarEventDTO;
import com.verdefluxo.acelera.model.entity.User;
import com.verdefluxo.acelera.model.entity.UserApiSettings;
import com.verdefluxo.acelera.repository.UserApiSettingsRepository;
import com.verdefluxo.acelera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.verdefluxo.acelera.model.dto.GoogleTokenResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private final UserRepository userRepository;
    private final UserApiSettingsRepository settingsRepository;
    private final GoogleOAuthService googleOAuthService;

    private final WebClient webClient = WebClient.create();

    private static final Map<String, String> COLOR_MAP = Map.ofEntries(
            Map.entry("1", "#7986CB"), Map.entry("2", "#33B679"), Map.entry("3", "#8E24AA"),
            Map.entry("4", "#E67C73"), Map.entry("5", "#F6BF26"), Map.entry("6", "#F4511E"),
            Map.entry("7", "#039BE5"), Map.entry("8", "#616161"), Map.entry("9", "#3F51B5"),
            Map.entry("10", "#0B8043"), Map.entry("11", "#D50000")
    );

    public List<CalendarEventDTO> getEventsForWeek(LocalDate weekStart, LocalDate weekEnd) {
        User user = getCurrentUser();
        UserApiSettings settings = settingsRepository.findByUser(user)
                .orElse(UserApiSettings.builder().user(user).build());

        String accessToken = resolveAccessToken(user, settings);

        // "primary" é a palavra reservada do Google que sempre aponta para o calendário principal do usuário autenticado
        String calendarId = (settings.getGoogleCalendarId() != null && !settings.getGoogleCalendarId().isBlank())
                ? settings.getGoogleCalendarId()
                : "primary";
        String timeMin = weekStart.atStartOfDay().atOffset(java.time.ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String timeMax = weekEnd.plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        CalendarEventsResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https").host("www.googleapis.com")
                        .path("/calendar/v3/calendars/{id}/events")
                        .queryParam("timeMin", timeMin)
                        .queryParam("timeMax", timeMax)
                        .queryParam("singleEvents", "true")
                        .queryParam("orderBy", "startTime")
                        .build(calendarId))
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(CalendarEventsResponse.class)
                .onErrorMap(WebClientResponseException.class, ex -> new IllegalStateException(
                        "Não foi possível acessar o Google Calendar. " +
                        "Verifique se a API Calendar está habilitada no Google Cloud Console " +
                        "e se o Calendar ID está correto. (HTTP " + ex.getStatusCode().value() + ")"))
                .block();

        if (response == null || response.items() == null) return List.of();

        return response.items().stream()
                .map(this::toDTO)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Retorna o access_token válido:
     * 1. Usa o token já salvo no User se ainda não expirou (com 1 min de margem).
     * 2. Caso expirado ou ausente, tenta refreshar usando o refreshToken das settings.
     * 3. Atualiza o User com o novo token.
     */
    private String resolveAccessToken(User user, UserApiSettings settings) {
        boolean tokenValid = user.getGoogleAccessToken() != null
                && user.getGoogleTokenExpiry() != null
                && user.getGoogleTokenExpiry().isAfter(LocalDateTime.now().plusMinutes(1));

        if (tokenValid) {
            return user.getGoogleAccessToken();
        }

        // Token expirado ou ausente — tenta refreshar
        if (settings.getGoogleRefreshToken() == null) {
            // Se o access_token do User existir mas estiver expirado e não temos refresh token,
            // o usuário precisa relogar com Google
            if (user.getGoogleAccessToken() == null) {
                throw new IllegalStateException(
                        "Faça login com o Google para acessar o Google Calendar.");
            }
            // Tenta usar mesmo assim (pode já ter expirado mas deixamos a API retornar o erro)
            return user.getGoogleAccessToken();
        }

        String clientId     = settings.getGoogleClientId()     != null ? settings.getGoogleClientId()     : googleOAuthService.getClientId();
        String clientSecret = settings.getGoogleClientSecret() != null ? settings.getGoogleClientSecret() : googleOAuthService.getClientSecret();

        try {
            GoogleTokenResponse tokenResponse = googleOAuthService.refreshAccessTokenWith(
                    settings.getGoogleRefreshToken(), clientId, clientSecret);

            // Persiste o novo access_token no User
            user.setGoogleAccessToken(tokenResponse.accessToken());
            user.setGoogleTokenExpiry(LocalDateTime.now().plusSeconds(
                    tokenResponse.expiresIn() != null ? tokenResponse.expiresIn() : 3600L));
            userRepository.save(user);

            return tokenResponse.accessToken();
        } catch (WebClientResponseException ex) {
            throw new IllegalStateException(
                    "Autorização com Google expirou. Faça logout e entre novamente com o Google para reautorizar o acesso ao Calendar.");
        }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));
    }

    private CalendarEventDTO toDTO(GoogleEvent event) {
        if (event.start() == null) return null;
        String title    = event.summary() != null ? event.summary() : "(sem título)";
        String colorHex = event.colorId() != null ? COLOR_MAP.getOrDefault(event.colorId(), "#164e3d") : "#164e3d";

        if (event.start().date() != null) {
            return new CalendarEventDTO(event.id(), title, event.start().date(), null, null, true, colorHex);
        }

        OffsetDateTime start = OffsetDateTime.parse(event.start().dateTime());
        OffsetDateTime end   = event.end() != null && event.end().dateTime() != null
                ? OffsetDateTime.parse(event.end().dateTime()) : start;

        return new CalendarEventDTO(
                event.id(), title,
                start.toLocalDate().toString(),
                start.format(DateTimeFormatter.ofPattern("HH:mm")),
                end.format(DateTimeFormatter.ofPattern("HH:mm")),
                false, colorHex
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CalendarEventsResponse(@JsonProperty("items") List<GoogleEvent> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GoogleEvent(
            @JsonProperty("id")      String id,
            @JsonProperty("summary") String summary,
            @JsonProperty("colorId") String colorId,
            @JsonProperty("start")   EventDateTime start,
            @JsonProperty("end")     EventDateTime end
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EventDateTime(
            @JsonProperty("dateTime") String dateTime,
            @JsonProperty("date")     String date
    ) {}
}
