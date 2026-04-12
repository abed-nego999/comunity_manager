package com.esteban.comunitymanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredencialRequest {

    /**
     * Token de acceso en claro. El servicio lo pasa a la entidad,
     * y AesConverter lo cifra de forma transparente al persistir.
     */
    @NotBlank(message = "El access token es obligatorio")
    private String accessToken;

    /** Token de refresco en claro. Puede ser nulo según la plataforma. */
    private String refreshToken;

    private Instant expiraEn;
}
