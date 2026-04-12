package com.esteban.comunitymanager.controller;

import com.esteban.comunitymanager.dto.response.*;
import com.esteban.comunitymanager.service.AuxiliarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints de solo lectura para los catálogos auxiliares:
 * plataformas, tipos de publicación, tipos de adjunto y roles de conversación.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuxiliarController {

    private final AuxiliarService auxiliarService;

    @GetMapping("/plataformas")
    public ResponseEntity<List<PlataformaResponse>> listarPlataformas() {
        return ResponseEntity.ok(auxiliarService.listarPlataformas());
    }

    @GetMapping("/tipos-publicacion")
    public ResponseEntity<List<TipoPublicacionResponse>> listarTiposPublicacion(
            @RequestParam(required = false) UUID plataformaId) {
        return ResponseEntity.ok(auxiliarService.listarTiposPublicacion(plataformaId));
    }

    @GetMapping("/tipos-adjunto")
    public ResponseEntity<List<TipoAdjuntoResponse>> listarTiposAdjunto() {
        return ResponseEntity.ok(auxiliarService.listarTiposAdjunto());
    }

    @GetMapping("/roles-conversacion")
    public ResponseEntity<List<RolConversacionResponse>> listarRolesConversacion() {
        return ResponseEntity.ok(auxiliarService.listarRolesConversacion());
    }
}
