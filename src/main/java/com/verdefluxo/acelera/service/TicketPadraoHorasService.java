package com.verdefluxo.acelera.service;

import com.verdefluxo.acelera.model.dto.TicketPadraoHorasDTO;
import com.verdefluxo.acelera.model.entity.CalendarEvent;
import com.verdefluxo.acelera.model.entity.TicketPadraoHoras;
import com.verdefluxo.acelera.model.entity.User;
import com.verdefluxo.acelera.repository.CalendarEventRepository;
import com.verdefluxo.acelera.repository.TicketPadraoHorasRepository;
import com.verdefluxo.acelera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketPadraoHorasService {

    private final TicketPadraoHorasRepository ticketRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final UserRepository userRepository;

    public List<TicketPadraoHorasDTO> listar() {
        return ticketRepository.findByUserOrderByNomeAsc(getCurrentUser())
                .stream().map(this::toDTO).toList();
    }

    @Transactional
    public TicketPadraoHorasDTO criar(TicketPadraoHorasDTO dto) {
        TicketPadraoHoras ticket = TicketPadraoHoras.builder()
                .user(getCurrentUser())
                .nome(dto.nome())
                .ticketKey(dto.ticketKey())
                .descricao(dto.descricao())
                .ativo(true)
                .build();
        return toDTO(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketPadraoHorasDTO atualizar(Long id, TicketPadraoHorasDTO dto) {
        TicketPadraoHoras ticket = ticketRepository.findByIdAndUser(id, getCurrentUser())
                .orElseThrow(() -> new IllegalArgumentException("Ticket não encontrado."));
        ticket.setNome(dto.nome());
        ticket.setTicketKey(dto.ticketKey());
        ticket.setDescricao(dto.descricao());
        ticket.setAtivo(dto.ativo());
        return toDTO(ticketRepository.save(ticket));
    }

    @Transactional
    public void excluir(Long id) {
        TicketPadraoHoras ticket = ticketRepository.findByIdAndUser(id, getCurrentUser())
                .orElseThrow(() -> new IllegalArgumentException("Ticket não encontrado."));
        ticketRepository.delete(ticket);
    }

    /**
     * Vincula ou desvincula um ticket de um evento do calendário.
     * ticketId == null remove o vínculo.
     */
    @Transactional
    public void vincularAoEvento(String googleEventId, Long ticketId) {
        User user = getCurrentUser();

        CalendarEvent event = calendarEventRepository.findByGoogleEventIdAndUser(googleEventId, user)
                .orElseThrow(() -> new IllegalArgumentException("Evento não encontrado."));

        if (ticketId == null) {
            event.setTicketPadrao(null);
        } else {
            TicketPadraoHoras ticket = ticketRepository.findByIdAndUser(ticketId, user)
                    .orElseThrow(() -> new IllegalArgumentException("Ticket não encontrado."));
            event.setTicketPadrao(ticket);
        }
        calendarEventRepository.save(event);
    }

    private TicketPadraoHorasDTO toDTO(TicketPadraoHoras t) {
        return new TicketPadraoHorasDTO(t.getId(), t.getNome(), t.getTicketKey(), t.getDescricao(), t.isAtivo());
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado."));
    }
}
