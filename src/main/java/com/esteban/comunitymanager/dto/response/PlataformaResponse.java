package com.esteban.comunitymanager.dto.response;

import com.esteban.comunitymanager.model.Plataforma;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlataformaResponse {

    private UUID id;
    private String nombre;

    public static PlataformaResponse from(Plataforma p) {
        return PlataformaResponse.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .build();
    }
}
