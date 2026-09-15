package com.verdefluxo.acelera.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ponto_registros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PontoRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoPonto tipo;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime dataHora = LocalDateTime.now();

    private String observacao;

    public enum TipoPonto {
        ENTRADA, SAIDA
    }
}
