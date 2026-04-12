package com.esteban.comunitymanager.dto.response;

import com.esteban.comunitymanager.model.Cliente;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponse {

    private UUID id;
    private String nombre;
    private String email;
    private String telefono;
    private String web;
    private boolean activo;
    private Instant creadoEn;

    public static ClienteResponse from(Cliente c) {
        return ClienteResponse.builder()
                .id(c.getId())
                .nombre(c.getNombre())
                .email(c.getEmail())
                .telefono(c.getTelefono())
                .web(c.getWeb())
                .activo(c.isActivo())
                .creadoEn(c.getCreadoEn())
                .build();
    }
}
