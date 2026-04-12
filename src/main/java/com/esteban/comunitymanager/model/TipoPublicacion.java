package com.esteban.comunitymanager.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tipo_publicacion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoPublicacion {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plataforma", nullable = false)
    private Plataforma plataforma;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    /**
     * true → la app puede publicar vía API automáticamente.
     * false → requiere acción manual del usuario.
     */
    @Column(name = "publicacion_automatica", nullable = false)
    private boolean publicacionAutomatica;

    /**
     * true → la plataforma acepta fecha futura en la llamada API.
     * false → la publicación es siempre inmediata.
     */
    @Column(name = "programacion_externa", nullable = false)
    private boolean programacionExterna;
}
