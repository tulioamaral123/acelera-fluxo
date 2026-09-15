package com.verdefluxo.acelera.controller;

import com.verdefluxo.acelera.model.dto.PontoRegistroDTO;
import com.verdefluxo.acelera.service.PontoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ponto")
@RequiredArgsConstructor
public class PontoController {

    private final PontoService pontoService;

    @PostMapping("/registrar")
    public ResponseEntity<PontoRegistroDTO> registrar(@Valid @RequestBody PontoRegistroDTO dto) {
        return ResponseEntity.ok(pontoService.registrar(dto));
    }

    @GetMapping("/hoje")
    public ResponseEntity<List<PontoRegistroDTO>> getRegistrosHoje() {
        return ResponseEntity.ok(pontoService.getRegistrosHoje());
    }
}
