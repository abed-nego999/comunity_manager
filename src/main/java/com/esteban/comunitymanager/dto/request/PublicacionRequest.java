package com.esteban.comunitymanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicacionRequest {

    @NotNull(message = "El eventoId es obligatorio")
    private UUID eventoId;

    @NotNull(message = "El idTipoPublicacion es obligatorio")
    private UUID idTipoPublicacion;

    @NotBlank(message = "El texto generado es obligatorio")
    private String textoGenerado;
}
