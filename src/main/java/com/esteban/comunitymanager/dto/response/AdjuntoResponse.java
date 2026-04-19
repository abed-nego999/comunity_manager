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
    private UUID eventoId;
    private String nombreFichero;
    private String rutaFichero;
    private String tipoMime;
    private OrigenAdjunto origen;
    private String descripcionIa;
    private Instant subidoEn;
    private Integer orden;

    public static AdjuntoResponse from(Adjunto a) {
        return AdjuntoResponse.builder()
                .id(a.getId())
                .eventoId(a.getEvento().getId())
                .nombreFichero(a.getNombreFichero())
                .rutaFichero(a.getRutaFichero())
                .tipoMime(a.getTipoMime())
                .origen(a.getOrigen())
                .descripcionIa(a.getDescripcionIa())
                .subidoEn(a.getSubidoEn())
                .build();
    }

    public static AdjuntoResponse from(Adjunto a, int orden) {
        AdjuntoResponse r = from(a);
        r.setOrden(orden);
        return r;
    }
}
