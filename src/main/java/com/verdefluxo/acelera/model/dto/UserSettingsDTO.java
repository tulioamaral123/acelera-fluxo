package com.verdefluxo.acelera.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSettingsDTO {
    private String jiraDomain;
    private String jiraEmail;
    private String jiraToken;
    private String bitbucketWorkspace;
    private String bitbucketToken;
    private String pontoApiKey;
    private String googleClientId;
    private String googleClientSecret;
    private String googleCalendarId;
    private String googleRefreshToken;
    // Atlassian OAuth
    private String atlassianClientId;
    private String atlassianClientSecret;
    private Boolean atlassianConnected;
    private String atlassianEmail;
    // Default Jira board/project
    private Long defaultBoardId;
    private String defaultBoardName;
}
