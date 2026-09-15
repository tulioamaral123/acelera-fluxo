package com.verdefluxo.acelera.service;

import com.verdefluxo.acelera.model.dto.PontoRegistroDTO;
import com.verdefluxo.acelera.model.entity.PontoRegistro;
import com.verdefluxo.acelera.model.entity.User;
import com.verdefluxo.acelera.repository.PontoRegistroRepository;
import com.verdefluxo.acelera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PontoService {

    private final PontoRegistroRepository pontoRepository;
    private final UserRepository userRepository;

    public PontoRegistroDTO registrar(PontoRegistroDTO dto) {
        User user = getCurrentUser();
        PontoRegistro registro = PontoRegistro.builder()
                .user(user)
                .tipo(dto.tipo())
                .dataHora(dto.dataHora() != null ? dto.dataHora() : LocalDateTime.now())
                .observacao(dto.observacao())
                .build();
        PontoRegistro saved = pontoRepository.save(registro);
        return toDTO(saved);
    }

    public List<PontoRegistroDTO> getRegistrosHoje() {
        User user = getCurrentUser();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        return pontoRepository
                .findByUserAndDataHoraBetweenOrderByDataHoraAsc(user, startOfDay, endOfDay)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private PontoRegistroDTO toDTO(PontoRegistro r) {
        return new PontoRegistroDTO(r.getId(), r.getTipo(), r.getDataHora(), r.getObservacao());
    }
}
