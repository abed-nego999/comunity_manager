package com.esteban.comunitymanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String email;
    private String telefono;
    private String web;

    @Builder.Default
    private boolean activo = true;
}
