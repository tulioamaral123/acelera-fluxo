package com.verdefluxo.acelera.model.dto;

public record UserSettingsDTO(
        String jiraDomain,
        String jiraEmail,
        String jiraToken,
        String bitbucketWorkspace,
        String bitbucketToken,
        String pontoApiKey,
        String googleClientId,
        String googleClientSecret,
        String googleCalendarId,
        String googleRefreshToken
) {}
