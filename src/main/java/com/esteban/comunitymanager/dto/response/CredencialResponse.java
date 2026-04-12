package com.esteban.comunitymanager.dto.response;

import com.esteban.comunitymanager.model.Credencial;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Los tokens de acceso y refresco nunca se incluyen en la respuesta.
 * Solo se devuelven metadatos: plataforma y fechas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredencialResponse {

    private UUID id;
    private PlataformaResponse plataforma;
    private Instant expiraEn;
    private Instant actualizadoEn;

    public static CredencialResponse from(Credencial c) {
        return CredencialResponse.builder()
                .id(c.getId())
                .plataforma(PlataformaResponse.from(c.getPlataforma()))
                .expiraEn(c.getExpiraEn())
                .actualizadoEn(c.getActualizadoEn())
                .build();
    }
}
