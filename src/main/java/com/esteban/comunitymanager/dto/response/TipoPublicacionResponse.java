package com.esteban.comunitymanager.dto.response;

import com.esteban.comunitymanager.model.TipoPublicacion;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoPublicacionResponse {

    private UUID id;
    private PlataformaResponse plataforma;
    private String nombre;
    private boolean publicacionAutomatica;
    private boolean programacionExterna;

    public static TipoPublicacionResponse from(TipoPublicacion t) {
        return TipoPublicacionResponse.builder()
                .id(t.getId())
                .plataforma(PlataformaResponse.from(t.getPlataforma()))
                .nombre(t.getNombre())
                .publicacionAutomatica(t.isPublicacionAutomatica())
                .programacionExterna(t.isProgramacionExterna())
                .build();
    }
}
