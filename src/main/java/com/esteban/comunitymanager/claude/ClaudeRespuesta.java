package com.esteban.comunitymanager.claude;

import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Resultado de una llamada a Claude.
 * textoRespuesta  — respuesta en lenguaje natural de Claude.
 * publicacionesCreadas — IDs de publicaciones que Claude creó via Tool Use durante este turno.
 */
@Value
@Builder
@AllArgsConstructor
public class ClaudeRespuesta {

    String textoRespuesta;

    @Builder.Default
    List<UUID> publicacionesCreadas = List.of();
}
