package com.esteban.comunitymanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeRequest {

    @NotBlank(message = "El contenido del mensaje es obligatorio")
    private String contenido;
}
