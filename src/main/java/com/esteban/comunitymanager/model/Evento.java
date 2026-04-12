package com.esteban.comunitymanager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "evento")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "fecha_evento")
    private LocalDate fechaEvento;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Ciclo de vida: BORRADOR → ACTIVO → CERRADO
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private EstadoEvento estado = EstadoEvento.BORRADOR;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false, nullable = false)
    private Instant creadoEn;
}
