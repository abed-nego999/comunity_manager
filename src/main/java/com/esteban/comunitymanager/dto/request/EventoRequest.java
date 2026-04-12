package com.esteban.comunitymanager.dto.request;

import com.esteban.comunitymanager.model.EstadoEvento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoRequest {

    @NotNull(message = "El clienteId es obligatorio")
    private UUID clienteId;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private LocalDate fechaEvento;
    private String descripcion;

    @Builder.Default
    private EstadoEvento estado = EstadoEvento.BORRADOR;
}
