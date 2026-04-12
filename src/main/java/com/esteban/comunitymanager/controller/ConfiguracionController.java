package com.esteban.comunitymanager.controller;

import com.esteban.comunitymanager.dto.request.ConfiguracionClienteRequest;
import com.esteban.comunitymanager.dto.request.InstruccionPlataformaRequest;
import com.esteban.comunitymanager.dto.response.ConfiguracionClienteResponse;
import com.esteban.comunitymanager.dto.response.InstruccionPlataformaResponse;
import com.esteban.comunitymanager.service.ConfiguracionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Gestión del system prompt de Claude y las instrucciones específicas por plataforma.
 * El system prompt se construye dinámicamente a partir de CONFIGURACION_CLIENTE
 * e INSTRUCCION_PLATAFORMA — editable desde el panel sin reiniciar la aplicación.
 */
@RestController
@RequestMapping("/api/v1/clientes/{id}/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    @GetMapping
    public ResponseEntity<ConfiguracionClienteResponse> obtenerConfiguracion(@PathVariable UUID id) {
        return ResponseEntity.ok(configuracionService.obtenerConfiguracion(id));
    }

    @PutMapping
    public ResponseEntity<ConfiguracionClienteResponse> actualizarConfiguracion(
            @PathVariable UUID id,
            @RequestBody ConfiguracionClienteRequest request) {
        return ResponseEntity.ok(configuracionService.actualizarConfiguracion(id, request));
    }

    @GetMapping("/instrucciones")
    public ResponseEntity<List<InstruccionPlataformaResponse>> listarInstrucciones(@PathVariable UUID id) {
        return ResponseEntity.ok(configuracionService.listarInstrucciones(id));
    }

    /**
     * Crea o actualiza la instrucción de Claude para una plataforma concreta.
     * La combinación (configuracion, plataforma) es única — upsert garantiza idempotencia.
     */
    @PutMapping("/instrucciones/{plataformaId}")
    public ResponseEntity<InstruccionPlataformaResponse> upsertInstruccion(
            @PathVariable UUID id,
            @PathVariable UUID plataformaId,
            @Valid @RequestBody InstruccionPlataformaRequest request) {
        return ResponseEntity.ok(configuracionService.upsertInstruccion(id, plataformaId, request));
    }
}
