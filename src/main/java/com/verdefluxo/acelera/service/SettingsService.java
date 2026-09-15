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
                .orElse(new UserSettingsDTO(null, null, null, null, null, null, null, null, null, null));
    }

    public UserSettingsDTO saveSettings(UserSettingsDTO dto) {
        User user = getCurrentUser();
        UserApiSettings settings = settingsRepository.findByUser(user)
                .orElse(UserApiSettings.builder().user(user).build());

        settings.setJiraDomain(dto.jiraDomain());
        settings.setJiraEmail(dto.jiraEmail());
        settings.setJiraToken(dto.jiraToken());
        settings.setBitbucketWorkspace(dto.bitbucketWorkspace());
        settings.setBitbucketToken(dto.bitbucketToken());
        settings.setPontoApiKey(dto.pontoApiKey());
        settings.setGoogleClientId(dto.googleClientId());
        settings.setGoogleClientSecret(dto.googleClientSecret());
        settings.setGoogleCalendarId(dto.googleCalendarId());
        settings.setGoogleRefreshToken(dto.googleRefreshToken());

        return toDTO(settingsRepository.save(settings));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private UserSettingsDTO toDTO(UserApiSettings s) {
        return new UserSettingsDTO(
                s.getJiraDomain(), s.getJiraEmail(), s.getJiraToken(),
                s.getBitbucketWorkspace(), s.getBitbucketToken(), s.getPontoApiKey(),
                s.getGoogleClientId(), s.getGoogleClientSecret(),
                s.getGoogleCalendarId(), s.getGoogleRefreshToken()
        );
    }
}
