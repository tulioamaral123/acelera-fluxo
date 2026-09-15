package com.verdefluxo.acelera.model.entity;

import jakarta.persistence.*;
import lombok.*;

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
}
