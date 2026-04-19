package com.esteban.comunitymanager.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ReordenarAdjuntosRequest {

    private List<ItemOrden> items;

    @Data
    public static class ItemOrden {
        private UUID adjuntoId;
        private Integer orden;
    }
}
