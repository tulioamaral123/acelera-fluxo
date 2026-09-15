package com.verdefluxo.acelera.service;

import com.verdefluxo.acelera.model.dto.MetricsDTO;
import com.verdefluxo.acelera.model.dto.PontoRegistroDTO;
import com.verdefluxo.acelera.model.entity.PontoRegistro;
import com.verdefluxo.acelera.model.entity.User;
import com.verdefluxo.acelera.repository.PontoRegistroRepository;
import com.verdefluxo.acelera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PontoRegistroRepository pontoRepository;
    private final UserRepository userRepository;

    public MetricsDTO getMetrics() {
        User user = getCurrentUser();
        LocalDate today = LocalDate.now();

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        LocalDateTime startOfWeek = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime endOfWeek = today.with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX);

        List<PontoRegistro> registrosHoje = pontoRepository
                .findByUserAndDataHoraBetweenOrderByDataHoraAsc(user, startOfDay, endOfDay);

        long totalSemana = pontoRepository.countByUserAndDataHoraBetween(user, startOfWeek, endOfWeek);

        LocalDateTime ultimaEntrada = registrosHoje.stream()
                .filter(r -> r.getTipo() == PontoRegistro.TipoPonto.ENTRADA)
                .map(PontoRegistro::getDataHora)
                .reduce((a, b) -> b)
                .orElse(null);

        LocalDateTime ultimaSaida = registrosHoje.stream()
                .filter(r -> r.getTipo() == PontoRegistro.TipoPonto.SAIDA)
                .map(PontoRegistro::getDataHora)
                .reduce((a, b) -> b)
                .orElse(null);

        boolean pontoAberto = !registrosHoje.isEmpty() &&
                registrosHoje.getLast().getTipo() == PontoRegistro.TipoPonto.ENTRADA;

        List<PontoRegistroDTO> dtos = registrosHoje.stream()
                .map(r -> new PontoRegistroDTO(r.getId(), r.getTipo(), r.getDataHora(), r.getObservacao()))
                .toList();

        return new MetricsDTO(
                registrosHoje.size(),
                totalSemana,
                ultimaEntrada,
                ultimaSaida,
                pontoAberto,
                dtos
        );
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
