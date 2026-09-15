package com.verdefluxo.acelera.model.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MetricsDTO(
        long totalPontosHoje,
        long totalPontosSemana,
        LocalDateTime ultimaEntrada,
        LocalDateTime ultimaSaida,
        boolean pontoAberto,
        List<PontoRegistroDTO> registrosHoje
) {}
