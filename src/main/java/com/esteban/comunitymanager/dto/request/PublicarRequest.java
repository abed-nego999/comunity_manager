package com.esteban.comunitymanager.dto.request;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicarRequest {

    /**
     * Fecha y hora de publicación programada en formato ISO 8601 sin zona
     * (ej: "2026-04-22T20:00:00"). Se interpreta en zona Europe/Madrid.
     * Si es null, se publica de forma inmediata.
     */
    private LocalDateTime fechaPublicacion;
}
