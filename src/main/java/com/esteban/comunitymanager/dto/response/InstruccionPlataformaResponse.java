package com.esteban.comunitymanager.dto.response;

import com.esteban.comunitymanager.model.InstruccionPlataforma;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstruccionPlataformaResponse {

    private UUID id;
    private PlataformaResponse plataforma;
    private String instrucciones;

    public static InstruccionPlataformaResponse from(InstruccionPlataforma i) {
        return InstruccionPlataformaResponse.builder()
                .id(i.getId())
                .plataforma(PlataformaResponse.from(i.getPlataforma()))
                .instrucciones(i.getInstrucciones())
                .build();
    }
}
