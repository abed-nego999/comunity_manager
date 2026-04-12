package com.esteban.comunitymanager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "publicacion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Publicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    /**
     * Determina la plataforma destino y el comportamiento de publicación
     * a través de los flags publicacion_automatica y programacion_externa.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_publicacion", nullable = false)
    private TipoPublicacion tipoPublicacion;

    @Column(name = "texto_generado", columnDefinition = "TEXT")
    private String textoGenerado;

    /**
     * Ciclo de vida: PENDIENTE → APROBADA → ENVIADA o RECHAZADA.
     * Las transiciones de estado son exclusivas del usuario (no de Claude).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private EstadoPublicacion estado = EstadoPublicacion.PENDIENTE;

    /** ID devuelto por Meta o YouTube al publicar, para trazabilidad. */
    @Column(name = "id_externo")
    private String idExterno;

    /** Momento en que la app llama a la API de la plataforma. */
    @Column(name = "fecha_envio")
    private Instant fechaEnvio;

    /** Momento en que la plataforma publica al público (puede ser futura si programacion_externa=true). */
    @Column(name = "fecha_publicacion")
    private Instant fechaPublicacion;

    /** Texto libre del usuario al solicitar cambios a Claude. */
    @Column(name = "feedback_usuario", columnDefinition = "TEXT")
    private String feedbackUsuario;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false, nullable = false)
    private Instant creadoEn;
}
