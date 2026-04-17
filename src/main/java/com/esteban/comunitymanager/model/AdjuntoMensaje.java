package com.esteban.comunitymanager.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "adjunto_mensaje")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjuntoMensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "id_adjunto", nullable = false)
    private UUID idAdjunto;

    @Column(name = "id_mensaje", nullable = false)
    private UUID idMensaje;
}
