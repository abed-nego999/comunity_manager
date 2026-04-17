package com.esteban.comunitymanager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "adjunto")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Adjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Evento al que pertenece — siempre presente. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    @Column(name = "nombre_fichero", nullable = false)
    private String nombreFichero;

    /** Ruta relativa al storage.base-path. */
    @Column(name = "ruta_fichero", nullable = false)
    private String rutaFichero;

    @Column(name = "tipo_mime", nullable = false)
    private String tipoMime;

    /** MANUAL → subido por el usuario. GENERADO → creado por Ideogram. */
    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false)
    private OrigenAdjunto origen;

    /** Prompt enviado a Ideogram. Solo si origen=GENERADO. */
    @Column(name = "prompt_usado", columnDefinition = "TEXT")
    private String promptUsado;

    /** Motor de generación (ej: ideogram-3). Solo si origen=GENERADO. */
    @Column(name = "motor")
    private String motor;

    /** Descripción textual generada por Claude al analizar el fichero. Evita reenviar base64. */
    @Column(name = "descripcion_ia", columnDefinition = "TEXT")
    private String descripcionIa;

    @CreationTimestamp
    @Column(name = "subido_en", updatable = false, nullable = false)
    private Instant subidoEn;
}
