package com.esteban.comunitymanager.controller;

import com.esteban.comunitymanager.dto.request.EventoRequest;
import com.esteban.comunitymanager.dto.request.MensajeRequest;
import com.esteban.comunitymanager.dto.response.EventoResponse;
import com.esteban.comunitymanager.dto.response.MensajeConversacionResponse;
import com.esteban.comunitymanager.dto.response.MensajeRespuestaResponse;
import com.esteban.comunitymanager.service.EventoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Gestión de eventos y conversación con Claude.
 *
 * Un evento es el contenedor principal de trabajo — agrupa la conversación
 * y todas las publicaciones generadas para un mismo evento o campaña.
 * Cada evento tiene su propio historial de conversación aislado.
 */
@RestController
@RequestMapping("/api/v1/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final EventoService eventoService;

    @GetMapping
    public ResponseEntity<List<EventoResponse>> listarEventos(
            @RequestParam UUID clienteId) {
        return ResponseEntity.ok(eventoService.listarEventos(clienteId));
    }

    @PostMapping
    public ResponseEntity<EventoResponse> crearEvento(@Valid @RequestBody EventoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.crearEvento(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoResponse> obtenerEvento(@PathVariable UUID id) {
        return ResponseEntity.ok(eventoService.obtenerEvento(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoResponse> actualizarEvento(
            @PathVariable UUID id,
            @Valid @RequestBody EventoRequest request) {
        return ResponseEntity.ok(eventoService.actualizarEvento(id, request));
    }

    // ── Conversación ─────────────────────────────────────────────────────────────

    /**
     * Devuelve el historial completo de la conversación del evento ordenado
     * cronológicamente. Se envía íntegramente a Claude en cada turno.
     */
    @GetMapping("/{id}/conversacion")
    public ResponseEntity<List<MensajeConversacionResponse>> obtenerConversacion(@PathVariable UUID id) {
        return ResponseEntity.ok(eventoService.obtenerConversacion(id));
    }

    /**
     * Procesa un mensaje del usuario y devuelve la respuesta de Claude.
     * Claude puede haber creado o actualizado publicaciones en BBDD como
     * efecto secundario mediante Tool Use — los IDs se incluyen en la respuesta.
     */
    @PostMapping("/{id}/conversacion")
    public ResponseEntity<MensajeRespuestaResponse> enviarMensaje(
            @PathVariable UUID id,
            @Valid @RequestBody MensajeRequest request) {
        return ResponseEntity.ok(eventoService.enviarMensaje(id, request));
    }
}
