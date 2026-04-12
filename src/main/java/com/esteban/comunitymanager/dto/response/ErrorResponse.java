package com.esteban.comunitymanager.dto.response;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private String codigo;
    private String mensaje;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
