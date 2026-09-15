package com.verdefluxo.acelera.model.dto;

public record TicketPadraoHorasDTO(
        Long   id,
        String nome,
        String ticketKey,
        String descricao,
        boolean ativo
) {}
