package com.esteban.comunitymanager.dto.response;

import com.esteban.comunitymanager.model.MensajeConversacion;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

    @Builder.Default
    private List<AdjuntoResponse> adjuntos = new ArrayList<>();

    public static MensajeConversacionResponse from(MensajeConversacion m) {
        return MensajeConversacionResponse.builder()
                .id(m.getId())
                .rol(RolConversacionResponse.from(m.getRol()))
                .contenido(m.getContenido())
                .enviadoEn(m.getEnviadoEn())
                .build();
    }

    public static MensajeConversacionResponse from(MensajeConversacion m, List<AdjuntoResponse> adjuntos) {
        return MensajeConversacionResponse.builder()
                .id(m.getId())
                .rol(RolConversacionResponse.from(m.getRol()))
                .contenido(m.getContenido())
                .enviadoEn(m.getEnviadoEn())
                .adjuntos(adjuntos != null ? adjuntos : new ArrayList<>())
                .build();
    }
}
