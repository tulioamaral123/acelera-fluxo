package com.verdefluxo.acelera.repository;

import com.verdefluxo.acelera.model.entity.TicketPadraoHoras;
import com.verdefluxo.acelera.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketPadraoHorasRepository extends JpaRepository<TicketPadraoHoras, Long> {

    List<TicketPadraoHoras> findByUserOrderByNomeAsc(User user);

    Optional<TicketPadraoHoras> findByIdAndUser(Long id, User user);
}
