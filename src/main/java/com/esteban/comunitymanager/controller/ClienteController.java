package com.esteban.comunitymanager.controller;

import com.esteban.comunitymanager.dto.request.ClienteRequest;
import com.esteban.comunitymanager.dto.response.ClienteResponse;
import com.esteban.comunitymanager.dto.response.InsightFranjaResponse;
import com.esteban.comunitymanager.service.ClienteService;
import com.esteban.comunitymanager.service.MetaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;
    private final MetaService metaService;

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listarClientes() {
        return ResponseEntity.ok(clienteService.listarClientes());
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> crearCliente(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crearCliente(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> obtenerCliente(@PathVariable UUID id) {
        return ResponseEntity.ok(clienteService.obtenerCliente(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> actualizarCliente(
            @PathVariable UUID id,
            @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(@PathVariable UUID id) {
        clienteService.eliminarCliente(id);
        return ResponseEntity.noContent().build();
    }

    /** Devuelve las franjas horarias óptimas para publicar, calculadas desde los insights de Meta. */
    @GetMapping("/{id}/insights")
    public ResponseEntity<List<InsightFranjaResponse>> obtenerInsights(@PathVariable UUID id) {
        return ResponseEntity.ok(metaService.obtenerFranjasOptimas(id));
    }
}
