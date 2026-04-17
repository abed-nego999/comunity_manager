package com.esteban.comunitymanager.controller;

import com.esteban.comunitymanager.dto.response.AdjuntoResponse;
import com.esteban.comunitymanager.service.AdjuntoService;
import com.esteban.comunitymanager.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestión unificada de adjuntos.
 *
 * POST   /api/v1/eventos/{id}/adjuntos           → sube desde el chat
 * GET    /api/v1/eventos/{id}/adjuntos            → lista adjuntos del evento
 * POST   /api/v1/publicaciones/{id}/adjuntos      → sube adjunto de publicación
 * GET    /api/v1/publicaciones/{id}/adjuntos      → lista adjuntos de publicación
 * DELETE /api/v1/adjuntos/{id}                   → elimina adjunto
 * PUT    /api/v1/adjuntos/{id}/publicacion        → asocia adjunto a publicación
 * GET    /api/v1/ficheros?ruta={ruta_relativa}   → sirve el fichero del storage
 */
@RestController
@RequiredArgsConstructor
public class AdjuntoController {

    private final AdjuntoService adjuntoService;
    private final StorageService storageService;

    // ── Adjuntos del evento ───────────────────────────────────────────────────

    @PostMapping(value = "/api/v1/eventos/{id}/adjuntos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdjuntoResponse> subirDesdeChat(
            @PathVariable UUID id,
            @RequestPart("fichero") MultipartFile fichero,
            @RequestPart(value = "mensajeId", required = false) String mensajeId) {
        UUID mensajeUuid = mensajeId != null && !mensajeId.isBlank() ? UUID.fromString(mensajeId) : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adjuntoService.subirDesdeChat(fichero, id, mensajeUuid));
    }

    @GetMapping("/api/v1/eventos/{id}/adjuntos")
    public ResponseEntity<List<AdjuntoResponse>> listarAdjuntosEvento(@PathVariable UUID id) {
        return ResponseEntity.ok(adjuntoService.listarPorEvento(id));
    }

    // ── Adjuntos de publicación ───────────────────────────────────────────────

    @PostMapping(value = "/api/v1/publicaciones/{id}/adjuntos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdjuntoResponse> subirAdjuntoPublicacion(
            @PathVariable UUID id,
            @RequestPart("fichero") MultipartFile fichero,
            @RequestPart("eventoId") String eventoId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adjuntoService.subirAdjuntoPublicacion(fichero, UUID.fromString(eventoId), id));
    }

    @GetMapping("/api/v1/publicaciones/{id}/adjuntos")
    public ResponseEntity<List<AdjuntoResponse>> listarAdjuntosPublicacion(@PathVariable UUID id) {
        return ResponseEntity.ok(adjuntoService.listarPorPublicacion(id));
    }

    // ── Operaciones sobre un adjunto concreto ─────────────────────────────────

    /**
     * Elimina o desasocia un adjunto.
     *
     * @param contexto "publicacion" → desasocia de la publicación (y borra si sin más referencias);
     *                 "mensaje"     → desasocia del mensaje (y borra si sin más referencias);
     *                 null          → borrado total incondicional.
     */
    @DeleteMapping("/api/v1/adjuntos/{id}")
    public ResponseEntity<Void> eliminarAdjunto(
            @PathVariable UUID id,
            @RequestParam(required = false) String contexto) {
        adjuntoService.eliminar(id, contexto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/v1/adjuntos/{id}/descripcion-ia")
    public ResponseEntity<AdjuntoResponse> actualizarDescripcionIa(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adjuntoService.actualizarDescripcionIa(id, body.get("descripcion")));
    }

    @PutMapping("/api/v1/adjuntos/{id}/publicacion")
    public ResponseEntity<AdjuntoResponse> asociarAPublicacion(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        UUID publicacionId = UUID.fromString(body.get("publicacionId"));
        return ResponseEntity.ok(adjuntoService.asociarAPublicacion(id, publicacionId));
    }

    // ── Servir ficheros del storage ───────────────────────────────────────────

    /**
     * Sirve un fichero del storage dado su ruta relativa al base-path.
     * Infiere el Content-Type desde la extensión del fichero.
     */
    @GetMapping("/api/v1/ficheros")
    public ResponseEntity<byte[]> servirFichero(@RequestParam String ruta) {
        byte[] contenido;
        try {
            contenido = storageService.leerFichero(ruta);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = inferirMediaType(ruta);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(contenido);
    }

    private MediaType inferirMediaType(String ruta) {
        String lower = ruta.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".png"))  return MediaType.IMAGE_PNG;
        if (lower.endsWith(".gif"))  return MediaType.IMAGE_GIF;
        if (lower.endsWith(".webp")) return MediaType.valueOf("image/webp");
        if (lower.endsWith(".pdf"))  return MediaType.APPLICATION_PDF;
        if (lower.endsWith(".mp4"))  return MediaType.valueOf("video/mp4");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
