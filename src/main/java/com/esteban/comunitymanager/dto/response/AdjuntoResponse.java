package com.esteban.comunitymanager.dto.response;

import com.esteban.comunitymanager.model.Adjunto;
import com.esteban.comunitymanager.model.OrigenAdjunto;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjuntoResponse {

    private UUID id;
    private TipoAdjuntoResponse tipoAdjunto;
    private String rutaFichero;
    private OrigenAdjunto origen;
    private Instant subidoEn;

    public static AdjuntoResponse from(Adjunto a) {
        return AdjuntoResponse.builder()
                .id(a.getId())
                .tipoAdjunto(TipoAdjuntoResponse.from(a.getTipoAdjunto()))
                .rutaFichero(a.getRutaFichero())
                .origen(a.getOrigen())
                .subidoEn(a.getSubidoEn())
                .build();
    }
}
