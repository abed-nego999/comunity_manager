package com.esteban.comunitymanager.controller;

import com.esteban.comunitymanager.dto.request.CredencialRequest;
import com.esteban.comunitymanager.dto.response.CredencialResponse;
import com.esteban.comunitymanager.service.CredencialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Gestión de tokens de acceso a APIs externas por cliente y plataforma.
 * Los tokens se cifran con AES antes de persistir y NUNCA se devuelven en claro.
 * Los endpoints solo exponen metadatos (plataforma, fecha de expiración).
 */
@RestController
@RequestMapping("/api/v1/clientes/{id}/credenciales")
@RequiredArgsConstructor
public class CredencialController {

    private final CredencialService credencialService;

    @GetMapping
    public ResponseEntity<List<CredencialResponse>> listarCredenciales(@PathVariable UUID id) {
        return ResponseEntity.ok(credencialService.listarCredenciales(id));
    }

    @PutMapping("/{plataformaId}")
    public ResponseEntity<CredencialResponse> upsertCredencial(
            @PathVariable UUID id,
            @PathVariable UUID plataformaId,
            @Valid @RequestBody CredencialRequest request) {
        return ResponseEntity.ok(credencialService.upsertCredencial(id, plataformaId, request));
    }

    @DeleteMapping("/{plataformaId}")
    public ResponseEntity<Void> eliminarCredencial(
            @PathVariable UUID id,
            @PathVariable UUID plataformaId) {
        credencialService.eliminarCredencial(id, plataformaId);
        return ResponseEntity.noContent().build();
    }
}
