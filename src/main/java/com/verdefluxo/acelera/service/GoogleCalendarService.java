package com.verdefluxo.acelera.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.verdefluxo.acelera.model.dto.CalendarEventDTO;
import com.verdefluxo.acelera.model.entity.CalendarEvent;
import com.verdefluxo.acelera.model.entity.TicketPadraoHoras;
import com.verdefluxo.acelera.model.entity.User;
import com.verdefluxo.acelera.model.entity.UserApiSettings;
import com.verdefluxo.acelera.repository.CalendarEventRepository;
import com.verdefluxo.acelera.repository.UserApiSettingsRepository;
import com.verdefluxo.acelera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.verdefluxo.acelera.model.dto.GoogleTokenResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

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

    private static final int CACHE_TTL_MINUTES = 15;

    private final UserRepository userRepository;
    private final UserApiSettingsRepository settingsRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final GoogleOAuthService googleOAuthService;

    private final WebClient webClient = WebClient.create();

    private static final Map<String, String> COLOR_MAP = Map.ofEntries(
            Map.entry("1", "#7986CB"), Map.entry("2", "#33B679"), Map.entry("3", "#8E24AA"),
            Map.entry("4", "#E67C73"), Map.entry("5", "#F6BF26"), Map.entry("6", "#F4511E"),
            Map.entry("7", "#039BE5"), Map.entry("8", "#616161"), Map.entry("9", "#3F51B5"),
            Map.entry("10", "#0B8043"), Map.entry("11", "#D50000")
    );

    @Transactional
    public List<CalendarEventDTO> getEventsForWeek(LocalDate weekStart, LocalDate weekEnd) {
        User user = getCurrentUser();

        List<CalendarEvent> cached = calendarEventRepository
                .findByUserAndDateBetweenOrderByDateAscStartTimeAsc(user, weekStart, weekEnd);

        if (isCacheFresh(cached)) {
            return cached.stream().map(this::toDTO).toList();
        }

        return syncFromGoogle(user, weekStart, weekEnd);
    }

    @Transactional
    public List<CalendarEventDTO> forceSync(LocalDate weekStart, LocalDate weekEnd) {
        User user = getCurrentUser();
        return syncFromGoogle(user, weekStart, weekEnd);
    }

    @Transactional(readOnly = true)
    public Page<CalendarEventDTO> listAll(int page, int size) {
        int clampedSize = Math.min(Math.max(size, 1), 100);
        User user = getCurrentUser();
        return calendarEventRepository
                .findByUserOrderByDateDescStartTimeDesc(user, PageRequest.of(page, clampedSize))
                .map(this::toDTO);
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private boolean isCacheFresh(List<CalendarEvent> cached) {
        if (cached.isEmpty()) return false;
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(CACHE_TTL_MINUTES);
        return cached.stream().allMatch(e -> e.getSyncedAt().isAfter(threshold));
    }

    private List<CalendarEventDTO> syncFromGoogle(User user, LocalDate weekStart, LocalDate weekEnd) {
        UserApiSettings settings = settingsRepository.findByUser(user)
                .orElse(UserApiSettings.builder().user(user).build());

        String accessToken = resolveAccessToken(user, settings);

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

        List<GoogleEvent> items = (response != null && response.items() != null)
                ? response.items()
                : List.of();

        persistEvents(user, weekStart, weekEnd, items);

        return items.stream()
                .map(this::toCalendarEventDTO)
                .filter(Objects::nonNull)
                .toList();
    }

    private void persistEvents(User user, LocalDate from, LocalDate to, List<GoogleEvent> items) {
        calendarEventRepository.deleteByUserAndDateBetween(user, from, to);

        LocalDateTime now = LocalDateTime.now();

        List<CalendarEvent> entities = items.stream()
                .map(event -> toEntity(event, user, now))
                .filter(Objects::nonNull)
                .toList();

        calendarEventRepository.saveAll(entities);
    }

    private CalendarEvent toEntity(GoogleEvent event, User user, LocalDateTime syncedAt) {
        if (event.start() == null) return null;

        String title    = event.summary() != null ? event.summary() : "(sem título)";
        String colorHex = event.colorId() != null ? COLOR_MAP.getOrDefault(event.colorId(), "#164e3d") : "#164e3d";

        CalendarEvent.CalendarEventBuilder builder = CalendarEvent.builder()
                .googleEventId(event.id())
                .user(user)
                .title(title)
                .colorHex(colorHex)
                .syncedAt(syncedAt);

        if (event.start().date() != null) {
            return builder
                    .date(LocalDate.parse(event.start().date()))
                    .allDay(true)
                    .build();
        }

        OffsetDateTime start = OffsetDateTime.parse(event.start().dateTime());
        OffsetDateTime end   = event.end() != null && event.end().dateTime() != null
                ? OffsetDateTime.parse(event.end().dateTime()) : start;

        return builder
                .date(start.toLocalDate())
                .startTime(start.format(DateTimeFormatter.ofPattern("HH:mm")))
                .endTime(end.format(DateTimeFormatter.ofPattern("HH:mm")))
                .allDay(false)
                .build();
    }

    private CalendarEventDTO toDTO(CalendarEvent entity) {
        TicketPadraoHoras ticket = entity.getTicketPadrao();
        return new CalendarEventDTO(
                entity.getGoogleEventId(),
                entity.getTitle(),
                entity.getDate().toString(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.isAllDay(),
                entity.getColorHex(),
                ticket != null ? ticket.getId()        : null,
                ticket != null ? ticket.getNome()      : null,
                ticket != null ? ticket.getTicketKey() : null
        );
    }

    private CalendarEventDTO toCalendarEventDTO(GoogleEvent event) {
        if (event.start() == null) return null;

        String title    = event.summary() != null ? event.summary() : "(sem título)";
        String colorHex = event.colorId() != null ? COLOR_MAP.getOrDefault(event.colorId(), "#164e3d") : "#164e3d";

        if (event.start().date() != null) {
            return new CalendarEventDTO(event.id(), title, event.start().date(), null, null, true, colorHex, null, null, null);
        }

        OffsetDateTime start = OffsetDateTime.parse(event.start().dateTime());
        OffsetDateTime end   = event.end() != null && event.end().dateTime() != null
                ? OffsetDateTime.parse(event.end().dateTime()) : start;

        return new CalendarEventDTO(
                event.id(), title,
                start.toLocalDate().toString(),
                start.format(DateTimeFormatter.ofPattern("HH:mm")),
                end.format(DateTimeFormatter.ofPattern("HH:mm")),
                false, colorHex, null, null, null
        );
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

        if (settings.getGoogleRefreshToken() == null) {
            if (user.getGoogleAccessToken() == null) {
                throw new IllegalStateException(
                        "Faça login com o Google para acessar o Google Calendar.");
            }
            return user.getGoogleAccessToken();
        }

        String clientId     = settings.getGoogleClientId()     != null ? settings.getGoogleClientId()     : googleOAuthService.getClientId();
        String clientSecret = settings.getGoogleClientSecret() != null ? settings.getGoogleClientSecret() : googleOAuthService.getClientSecret();

        try {
            GoogleTokenResponse tokenResponse = googleOAuthService.refreshAccessTokenWith(
                    settings.getGoogleRefreshToken(), clientId, clientSecret);

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

    // -------------------------------------------------------------------------
    // Google API response types
    // -------------------------------------------------------------------------

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
