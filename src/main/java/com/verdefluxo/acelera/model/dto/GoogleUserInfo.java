package com.verdefluxo.acelera.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleUserInfo(
    @JsonProperty("sub")            String sub,
    @JsonProperty("email")          String email,
    @JsonProperty("name")           String name,
    @JsonProperty("picture")        String picture,
    @JsonProperty("email_verified") Boolean emailVerified
) {}
