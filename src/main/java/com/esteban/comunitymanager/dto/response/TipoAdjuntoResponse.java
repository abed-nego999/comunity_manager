package com.esteban.comunitymanager.dto.response;

import com.esteban.comunitymanager.model.TipoAdjunto;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoAdjuntoResponse {

    private UUID id;
    private String nombre;

    public static TipoAdjuntoResponse from(TipoAdjunto t) {
        return TipoAdjuntoResponse.builder()
                .id(t.getId())
                .nombre(t.getNombre())
                .build();
    }
}
