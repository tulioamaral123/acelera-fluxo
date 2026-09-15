package com.verdefluxo.acelera.model.dto;

public record AuthResponse(
        String token,
        String name,
        String email,
        String role
) {}
