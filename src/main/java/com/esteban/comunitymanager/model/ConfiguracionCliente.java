package com.esteban.comunitymanager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "configuracion_cliente")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false, unique = true)
    private Cliente cliente;

    /** Tono de comunicación para el system prompt de Claude. */
    @Column(name = "tono", columnDefinition = "TEXT")
    private String tono;

    /** Temas o palabras que Claude debe evitar. */
    @Column(name = "restricciones", columnDefinition = "TEXT")
    private String restricciones;

    /** Llamada a la acción por defecto. */
    @Column(name = "cta_predeterminada", columnDefinition = "TEXT")
    private String ctaPredeterminada;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;
}
