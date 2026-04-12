package com.esteban.comunitymanager.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Respuesta al enviar un mensaje en la conversación.
 * Incluye la respuesta de Claude y los IDs de publicaciones
 * que haya creado como efecto secundario mediante Tool Use.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeRespuestaResponse {

    private MensajeConversacionResponse mensaje;

    /** IDs de publicaciones creadas por Claude mediante Tool Use durante este turno. */
    @Builder.Default
    private List<UUID> publicacionesCreadas = List.of();
}
