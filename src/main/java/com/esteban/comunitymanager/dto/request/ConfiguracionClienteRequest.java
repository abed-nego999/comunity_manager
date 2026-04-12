package com.esteban.comunitymanager.dto.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionClienteRequest {

    private String tono;
    private String restricciones;
    /** Llamada a la acción predeterminada (cta_predeterminada en BBDD). */
    private String llamadaALaAccion;
}
