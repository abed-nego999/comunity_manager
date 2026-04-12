package com.esteban.comunitymanager.dto.response;

import com.esteban.comunitymanager.model.MensajeConversacion;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeConversacionResponse {

    private UUID id;
    private RolConversacionResponse rol;
    private String contenido;
    private Instant enviadoEn;

    public static MensajeConversacionResponse from(MensajeConversacion m) {
        return MensajeConversacionResponse.builder()
                .id(m.getId())
                .rol(RolConversacionResponse.from(m.getRol()))
                .contenido(m.getContenido())
                .enviadoEn(m.getEnviadoEn())
                .build();
    }
}
