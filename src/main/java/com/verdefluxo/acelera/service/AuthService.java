package com.verdefluxo.acelera.service;

import com.verdefluxo.acelera.model.dto.AuthResponse;
import com.verdefluxo.acelera.model.dto.GoogleTokenResponse;
import com.verdefluxo.acelera.model.dto.GoogleUserInfo;
import com.verdefluxo.acelera.model.dto.LoginRequest;
import com.verdefluxo.acelera.model.dto.RegisterRequest;
import com.verdefluxo.acelera.model.entity.User;
import com.verdefluxo.acelera.model.entity.UserApiSettings;
import com.verdefluxo.acelera.repository.UserApiSettingsRepository;
import com.verdefluxo.acelera.repository.UserRepository;
import com.verdefluxo.acelera.security.JwtUtil;
import com.verdefluxo.acelera.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserApiSettingsRepository settingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final GoogleOAuthService googleOAuthService;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtUtil.generateToken(userDetails);
        User user = userRepository.findByEmail(request.email()).orElseThrow();
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse loginWithGoogle(String code) {
        GoogleTokenResponse tokens = googleOAuthService.exchangeCodeForTokens(code);
        GoogleUserInfo userInfo = googleOAuthService.getUserInfo(tokens.accessToken());

        // Upsert: find by googleId first, then by email, otherwise create new
        User user = userRepository.findByGoogleId(userInfo.sub())
                .orElseGet(() -> userRepository.findByEmail(userInfo.email()).orElse(null));

        if (user == null) {
            user = User.builder()
                    .name(userInfo.name())
                    .email(userInfo.email())
                    .build();
        }

        user.setGoogleId(userInfo.sub());
        user.setGoogleAccessToken(tokens.accessToken());
        user.setGoogleTokenExpiry(LocalDateTime.now().plusSeconds(
                tokens.expiresIn() != null ? tokens.expiresIn() : 3600L
        ));
        User savedUser = userRepository.save(user);

        // Persist refresh token in UserApiSettings if provided
        if (tokens.refreshToken() != null) {
            UserApiSettings settings = settingsRepository.findByUser(savedUser)
                    .orElse(UserApiSettings.builder().user(savedUser).build());
            settings.setGoogleRefreshToken(tokens.refreshToken());
            settingsRepository.save(settings);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String jwt = jwtUtil.generateToken(userDetails);
        return new AuthResponse(jwt, savedUser.getName(), savedUser.getEmail(),
                savedUser.getRole() != null ? savedUser.getRole().name() : "USER");
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email já cadastrado.");
        }
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();
        userRepository.save(user);
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtUtil.generateToken(userDetails);
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
    }
}
