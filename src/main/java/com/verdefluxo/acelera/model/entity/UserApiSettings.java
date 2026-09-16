package com.verdefluxo.acelera.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_api_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserApiSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String jiraDomain;
    private String jiraEmail;
    private String jiraToken;

    private String bitbucketWorkspace;
    private String bitbucketToken;

    private String pontoApiKey;

    // Google Calendar
    private String googleClientId;
    private String googleClientSecret;
    private String googleCalendarId;
    private String googleRefreshToken;

    // Atlassian OAuth 2.0 (3LO)
    private String atlassianClientId;
    private String atlassianClientSecret;
    @Column(length = 2048) private String atlassianAccessToken;
    @Column(length = 1024) private String atlassianRefreshToken;
    private String atlassianCloudId;
    private LocalDateTime atlassianTokenExpiry;
    private String atlassianEmail;

    // Default Jira board/project
    private Long defaultBoardId;
    private String defaultBoardName;
}
