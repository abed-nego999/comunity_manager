package com.esteban.comunitymanager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mensaje_conversacion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeConversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    /**
     * Rol del emisor: Usuario o Claude.
     * El historial completo se envía a la API de Anthropic en cada turno.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol", nullable = false)
    private RolConversacion rol;

    @Column(name = "contenido", columnDefinition = "TEXT", nullable = false)
    private String contenido;

    @CreationTimestamp
    @Column(name = "enviado_en", updatable = false, nullable = false)
    private Instant enviadoEn;
}
