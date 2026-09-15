package com.verdefluxo.acelera.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("[API] Erro não tratado: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body(Map.of("message", "Erro interno do servidor. Tente novamente."));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    /**
     * Captura erros de chamadas a APIs externas (Google, Jira, Bitbucket) via WebClient.
     * Converte qualquer status HTTP externo em 400 para evitar que o interceptor Angular
     * interprete um 401 do Google como sessão expirada do app.
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, String>> handleWebClientError(WebClientResponseException ex) {
        String message = switch (ex.getStatusCode().value()) {
            case 400 -> "Credenciais inválidas. Verifique as configurações de integração.";
            case 401 -> "Acesso não autorizado pela API externa. Verifique as credenciais nas configurações.";
            case 403 -> "Acesso negado pela API externa. Verifique as permissões configuradas.";
            case 404 -> "Recurso não encontrado na API externa.";
            default  -> "Erro ao comunicar com serviço externo (" + ex.getStatusCode().value() + ").";
        };
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
