package com.esteban.comunitymanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstruccionPlataformaRequest {

    @NotBlank(message = "Las instrucciones son obligatorias")
    private String instrucciones;
}
