package com.esteban.comunitymanager.dto.response;

import com.esteban.comunitymanager.model.EstadoPublicacion;
import com.esteban.comunitymanager.model.Publicacion;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicacionResponse {

    private UUID id;
    private UUID eventoId;
    private TipoPublicacionResponse tipoPublicacion;
    private String textoGenerado;
    private EstadoPublicacion estado;
    /** ID devuelto por Meta o YouTube al publicar. Null hasta que se envíe. */
    private String idExterno;
    private Instant fechaEnvio;
    private Instant fechaPublicacion;
    private String feedbackUsuario;
    private Instant creadoEn;

    public static PublicacionResponse from(Publicacion p) {
        return PublicacionResponse.builder()
                .id(p.getId())
                .eventoId(p.getEvento().getId())
                .tipoPublicacion(TipoPublicacionResponse.from(p.getTipoPublicacion()))
                .textoGenerado(p.getTextoGenerado())
                .estado(p.getEstado())
                .idExterno(p.getIdExterno())
                .fechaEnvio(p.getFechaEnvio())
                .fechaPublicacion(p.getFechaPublicacion())
                .feedbackUsuario(p.getFeedbackUsuario())
                .creadoEn(p.getCreadoEn())
                .build();
    }
}
