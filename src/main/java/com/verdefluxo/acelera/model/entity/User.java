package com.verdefluxo.acelera.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // Google OAuth 2.0
    @Column(unique = true)
    private String googleId;

    @Column(length = 2048)
    private String googleAccessToken;

    @Column
    private LocalDateTime googleTokenExpiry;

    public enum Role {
        USER, ADMIN
    }
}
