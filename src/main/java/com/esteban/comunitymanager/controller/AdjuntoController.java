package com.esteban.comunitymanager.controller;

import com.esteban.comunitymanager.dto.response.AdjuntoResponse;
import com.esteban.comunitymanager.service.AdjuntoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Gestión de archivos multimedia adjuntos a publicaciones.
 * Los ficheros se guardan en disco bajo:
 * storage/clientes/{id}_{nombre}/eventos/{id}_{nombre}/adjuntos/
 */
@RestController
@RequestMapping("/api/v1/publicaciones/{id}/adjuntos")
@RequiredArgsConstructor
public class AdjuntoController {

    private final AdjuntoService adjuntoService;

    @GetMapping
    public ResponseEntity<List<AdjuntoResponse>> listarAdjuntos(@PathVariable UUID id) {
        return ResponseEntity.ok(adjuntoService.listarAdjuntos(id));
    }

    /**
     * Sube un fichero y lo registra como adjunto de la publicación.
     * El origen se establece automáticamente como MANUAL.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdjuntoResponse> subirAdjunto(
            @PathVariable UUID id,
            @RequestPart("fichero") MultipartFile fichero,
            @RequestPart("tipoAdjuntoId") String tipoAdjuntoId) {
        UUID idTipo = UUID.fromString(tipoAdjuntoId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adjuntoService.subirAdjunto(id, fichero, idTipo));
    }

    @DeleteMapping("/{adjuntoId}")
    public ResponseEntity<Void> eliminarAdjunto(
            @PathVariable UUID id,
            @PathVariable UUID adjuntoId) {
        adjuntoService.eliminarAdjunto(id, adjuntoId);
        return ResponseEntity.noContent().build();
    }
}
