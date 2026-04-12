package com.esteban.comunitymanager.dto.response;

import com.esteban.comunitymanager.model.Evento;
import com.esteban.comunitymanager.model.EstadoEvento;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoResponse {

    private UUID id;
    private UUID clienteId;
    private String nombre;
    private LocalDate fechaEvento;
    private String descripcion;
    private EstadoEvento estado;
    private Instant creadoEn;

    public static EventoResponse from(Evento e) {
        return EventoResponse.builder()
                .id(e.getId())
                .clienteId(e.getCliente().getId())
                .nombre(e.getNombre())
                .fechaEvento(e.getFechaEvento())
                .descripcion(e.getDescripcion())
                .estado(e.getEstado())
                .creadoEn(e.getCreadoEn())
                .build();
    }
}
