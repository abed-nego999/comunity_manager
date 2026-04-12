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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_publicacion", nullable = false)
    private Publicacion publicacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_adjunto", nullable = false)
    private TipoAdjunto tipoAdjunto;

    /** Ruta relativa al storage.base-path del fichero multimedia. */
    @Column(name = "ruta_fichero", nullable = false)
    private String rutaFichero;

    /** MANUAL → subido por el usuario. GENERADO → creado por Ideogram. */
    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false)
    private OrigenAdjunto origen;

    @CreationTimestamp
    @Column(name = "subido_en", updatable = false, nullable = false)
    private Instant subidoEn;
}
