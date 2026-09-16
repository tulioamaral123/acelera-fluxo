package com.verdefluxo.acelera.controller;

import com.verdefluxo.acelera.model.dto.TicketPadraoHorasDTO;
import com.verdefluxo.acelera.service.TicketPadraoHorasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets-padrao")
@RequiredArgsConstructor
public class TicketPadraoHorasController {

    private final TicketPadraoHorasService service;

    @GetMapping
    public ResponseEntity<List<TicketPadraoHorasDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PostMapping
    public ResponseEntity<TicketPadraoHorasDTO> criar(@RequestBody TicketPadraoHorasDTO dto) {
        return ResponseEntity.ok(service.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketPadraoHorasDTO> atualizar(@PathVariable Long id,
                                                           @RequestBody TicketPadraoHorasDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    /** Vincula (ou remove) ticket de um evento. Body: { "ticketId": 1 } ou { "ticketId": null } */
    @PatchMapping("/vincular/{googleEventId}")
    public ResponseEntity<Void> vincular(@PathVariable String googleEventId,
                                          @RequestBody Map<String, Long> body) {
        service.vincularAoEvento(googleEventId, body.get("ticketId"));
        return ResponseEntity.noContent().build();
    }

    /** Vincula o mesmo ticket a múltiplos eventos. Body: { "eventIds": [...], "ticketId": 1 } */
    @PatchMapping("/vincular-lote")
    public ResponseEntity<Void> vincularLote(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> eventIds = (List<String>) body.get("eventIds");
        Long ticketId = body.get("ticketId") != null
                ? Long.valueOf(body.get("ticketId").toString()) : null;
        service.vincularLote(eventIds, ticketId);
        return ResponseEntity.noContent().build();
    }
}
