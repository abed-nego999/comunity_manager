package com.esteban.comunitymanager.dto.response;

import com.esteban.comunitymanager.model.ConfiguracionCliente;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionClienteResponse {

    private UUID id;
    private String tono;
    private String restricciones;
    /** Nombre en la API: llamadaALaAccion. Campo en BBDD: cta_predeterminada. */
    private String llamadaALaAccion;
    private Instant actualizadoEn;

    public static ConfiguracionClienteResponse from(ConfiguracionCliente c) {
        return ConfiguracionClienteResponse.builder()
                .id(c.getId())
                .tono(c.getTono())
                .restricciones(c.getRestricciones())
                .llamadaALaAccion(c.getCtaPredeterminada())
                .actualizadoEn(c.getActualizadoEn())
                .build();
    }
}
