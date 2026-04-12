package com.esteban.comunitymanager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "imagen_generada")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagenGenerada {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Evento al que pertenece la imagen (siempre presente). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento", nullable = false)
    private Evento evento;

    /**
     * Publicación concreta a la que está vinculada la imagen.
     * Null si la imagen es un cartel genérico del evento.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_publicacion")
    private Publicacion publicacion;

    /** Prompt enviado a Ideogram para generar la imagen. */
    @Column(name = "prompt_usado", columnDefinition = "TEXT")
    private String promptUsado;

    /** Ruta relativa al storage.base-path del fichero generado. */
    @Column(name = "ruta_fichero")
    private String rutaFichero;

    /** Motor de generación usado (ej: ideogram-3). */
    @Column(name = "motor")
    private String motor;

    @CreationTimestamp
    @Column(name = "generado_en", updatable = false, nullable = false)
    private Instant generadoEn;
}
