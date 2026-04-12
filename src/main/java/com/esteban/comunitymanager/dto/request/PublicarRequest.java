package com.esteban.comunitymanager.dto.request;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicarRequest {

    /**
     * Fecha futura de publicación. Solo se aplica si el tipo de publicación
     * tiene programacion_externa=true. Si es null, se publica de forma inmediata.
     */
    private Instant fechaPublicacion;
}
