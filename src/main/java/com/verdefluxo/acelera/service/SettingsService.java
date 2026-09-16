package com.verdefluxo.acelera.service;

import com.verdefluxo.acelera.model.dto.UserSettingsDTO;
import com.verdefluxo.acelera.model.entity.User;
import com.verdefluxo.acelera.model.entity.UserApiSettings;
import com.verdefluxo.acelera.repository.UserApiSettingsRepository;
import com.verdefluxo.acelera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final UserApiSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    public UserSettingsDTO getSettings() {
        User user = getCurrentUser();
        return settingsRepository.findByUser(user)
                .map(this::toDTO)
                .orElse(new UserSettingsDTO());
    }

    public UserSettingsDTO saveSettings(UserSettingsDTO dto) {
        User user = getCurrentUser();
        UserApiSettings settings = settingsRepository.findByUser(user)
                .orElse(UserApiSettings.builder().user(user).build());

        settings.setJiraDomain(dto.getJiraDomain());
        settings.setJiraEmail(dto.getJiraEmail());
        settings.setJiraToken(dto.getJiraToken());
        settings.setBitbucketWorkspace(dto.getBitbucketWorkspace());
        settings.setBitbucketToken(dto.getBitbucketToken());
        settings.setPontoApiKey(dto.getPontoApiKey());
        settings.setGoogleClientId(dto.getGoogleClientId());
        settings.setGoogleClientSecret(dto.getGoogleClientSecret());
        settings.setGoogleCalendarId(dto.getGoogleCalendarId());
        settings.setGoogleRefreshToken(dto.getGoogleRefreshToken());
        // Only persist clientId/secret — never overwrite OAuth tokens from this endpoint
        settings.setAtlassianClientId(dto.getAtlassianClientId());
        settings.setAtlassianClientSecret(dto.getAtlassianClientSecret());
        settings.setDefaultBoardId(dto.getDefaultBoardId());
        settings.setDefaultBoardName(dto.getDefaultBoardName());

        return toDTO(settingsRepository.save(settings));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private UserSettingsDTO toDTO(UserApiSettings s) {
        return UserSettingsDTO.builder()
                .jiraDomain(s.getJiraDomain())
                .jiraEmail(s.getJiraEmail())
                .jiraToken(s.getJiraToken())
                .bitbucketWorkspace(s.getBitbucketWorkspace())
                .bitbucketToken(s.getBitbucketToken())
                .pontoApiKey(s.getPontoApiKey())
                .googleClientId(s.getGoogleClientId())
                .googleClientSecret(s.getGoogleClientSecret())
                .googleCalendarId(s.getGoogleCalendarId())
                .googleRefreshToken(s.getGoogleRefreshToken())
                .atlassianClientId(s.getAtlassianClientId())
                .atlassianClientSecret(s.getAtlassianClientSecret())
                .atlassianConnected(s.getAtlassianAccessToken() != null)
                .atlassianEmail(s.getAtlassianEmail())
                .defaultBoardId(s.getDefaultBoardId())
                .defaultBoardName(s.getDefaultBoardName())
                .build();
    }
}
