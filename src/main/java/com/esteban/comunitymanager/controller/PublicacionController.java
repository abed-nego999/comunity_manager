package com.esteban.comunitymanager.controller;

import com.esteban.comunitymanager.dto.request.FeedbackRequest;
import com.esteban.comunitymanager.dto.request.PublicacionRequest;
import com.esteban.comunitymanager.dto.request.PublicarRequest;
import com.esteban.comunitymanager.dto.response.PublicacionResponse;
import com.esteban.comunitymanager.model.EstadoPublicacion;
import com.esteban.comunitymanager.service.PublicacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Ciclo de vida completo de las publicaciones generadas por Claude.
 *
 * Transiciones de estado (exclusivas del usuario):
 *   PENDIENTE → APROBADA  (PATCH /aprobar)
 *   PENDIENTE → RECHAZADA (PATCH /rechazar)
 *   APROBADA  → ENVIADA   (POST /publicar)
 *
 * Operaciones de Claude via Tool Use:
 *   POST /publicaciones       — crea borrador en estado PENDIENTE
 *   PUT  /publicaciones/{id}  — actualiza contenido tras feedback del usuario
 */
@RestController
@RequestMapping("/api/v1/publicaciones")
@RequiredArgsConstructor
public class PublicacionController {

    private final PublicacionService publicacionService;

    @GetMapping
    public ResponseEntity<List<PublicacionResponse>> listarPublicaciones(
            @RequestParam UUID eventoId,
            @RequestParam(required = false) EstadoPublicacion estado) {
        return ResponseEntity.ok(publicacionService.listarPublicaciones(eventoId, estado));
    }

    /** Usado principalmente por Claude via Tool Use para crear borradores. */
    @PostMapping
    public ResponseEntity<PublicacionResponse> crearPublicacion(
            @Valid @RequestBody PublicacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(publicacionService.crearPublicacion(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicacionResponse> obtenerPublicacion(@PathVariable UUID id) {
        return ResponseEntity.ok(publicacionService.obtenerPublicacion(id));
    }

    /** Usado por Claude via Tool Use para regenerar contenido tras feedback del usuario. */
    @PutMapping("/{id}")
    public ResponseEntity<PublicacionResponse> actualizarPublicacion(
            @PathVariable UUID id,
            @Valid @RequestBody PublicacionRequest request) {
        return ResponseEntity.ok(publicacionService.actualizarPublicacion(id, request));
    }

    // ── Transiciones de estado ────────────────────────────────────────────────────

    /** Aprueba la publicación (PENDIENTE → APROBADA). Operación local. */
    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<PublicacionResponse> aprobarPublicacion(@PathVariable UUID id) {
        return ResponseEntity.ok(publicacionService.aprobarPublicacion(id));
    }

    /** Rechaza la publicación (PENDIENTE → RECHAZADA). Queda en el historial. */
    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<PublicacionResponse> rechazarPublicacion(@PathVariable UUID id) {
        return ResponseEntity.ok(publicacionService.rechazarPublicacion(id));
    }

    /**
     * Guarda el feedback del usuario en la publicación.
     * El usuario continuará la conversación del evento para que Claude
     * lea el feedback y regenere el contenido.
     */
    @PatchMapping("/{id}/solicitar-cambios")
    public ResponseEntity<PublicacionResponse> solicitarCambios(
            @PathVariable UUID id,
            @Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.ok(publicacionService.solicitarCambios(id, request));
    }

    /**
     * Publica en la plataforma externa (Meta Graph API o YouTube Data API).
     * Operación separada del cambio de estado — no mezcla lógica local con llamadas externas.
     * Fase 1: simula el envío. Fase 2/3: integración real.
     */
    @PostMapping("/{id}/publicar")
    public ResponseEntity<PublicacionResponse> publicar(
            @PathVariable UUID id,
            @RequestBody(required = false) PublicarRequest request) {
        return ResponseEntity.ok(publicacionService.publicar(id, request));
    }
}
