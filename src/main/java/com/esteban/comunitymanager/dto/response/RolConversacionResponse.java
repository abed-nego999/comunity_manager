package com.esteban.comunitymanager.dto.response;

import com.esteban.comunitymanager.model.RolConversacion;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolConversacionResponse {

    private UUID id;
    private String nombre;

    public static RolConversacionResponse from(RolConversacion r) {
        return RolConversacionResponse.builder()
                .id(r.getId())
                .nombre(r.getNombre())
                .build();
    }
}
