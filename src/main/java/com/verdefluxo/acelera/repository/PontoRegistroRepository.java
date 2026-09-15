package com.verdefluxo.acelera.repository;

import com.verdefluxo.acelera.model.entity.PontoRegistro;
import com.verdefluxo.acelera.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PontoRegistroRepository extends JpaRepository<PontoRegistro, Long> {

    List<PontoRegistro> findByUserAndDataHoraBetweenOrderByDataHoraAsc(
            User user, LocalDateTime start, LocalDateTime end);

    Optional<PontoRegistro> findTopByUserOrderByDataHoraDesc(User user);

    long countByUserAndDataHoraBetween(User user, LocalDateTime start, LocalDateTime end);
}
