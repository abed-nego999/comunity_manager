package com.esteban.comunitymanager.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
    name = "instruccion_plataforma",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_instruccion_configuracion_plataforma",
        columnNames = {"id_configuracion", "id_plataforma"}
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstruccionPlataforma {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_configuracion", nullable = false)
    private ConfiguracionCliente configuracion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plataforma", nullable = false)
    private Plataforma plataforma;

    /** Instrucciones específicas para Claude en esta plataforma. */
    @Column(name = "instrucciones", columnDefinition = "TEXT")
    private String instrucciones;
}
