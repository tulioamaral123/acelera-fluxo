package com.verdefluxo.acelera.model.dto;

import com.verdefluxo.acelera.model.entity.PontoRegistro;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record PontoRegistroDTO(
        Long id,
        @NotNull PontoRegistro.TipoPonto tipo,
        LocalDateTime dataHora,
        String observacao
) {}
