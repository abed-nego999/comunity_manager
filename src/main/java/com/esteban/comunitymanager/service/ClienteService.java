package com.esteban.comunitymanager.service;

import com.esteban.comunitymanager.dto.request.ClienteRequest;
import com.esteban.comunitymanager.dto.response.ClienteResponse;
import com.esteban.comunitymanager.exception.ResourceNotFoundException;
import com.esteban.comunitymanager.model.Cliente;
import com.esteban.comunitymanager.model.ConfiguracionCliente;
import com.esteban.comunitymanager.repository.ClienteRepository;
import com.esteban.comunitymanager.repository.ConfiguracionClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ConfiguracionClienteRepository configuracionClienteRepository;

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarClientes() {
        return clienteRepository.findAll().stream()
                .map(ClienteResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerCliente(UUID id) {
        return ClienteResponse.from(buscarCliente(id));
    }

    /**
     * Crea el cliente y su ConfiguracionCliente asociada (1:1 obligatoria).
     * La configuración se inicializa vacía y se edita desde el panel.
     */
    @Transactional
    public ClienteResponse crearCliente(ClienteRequest request) {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .web(request.getWeb())
                .activo(request.isActivo())
                .build());

        configuracionClienteRepository.save(ConfiguracionCliente.builder()
                .cliente(cliente)
                .build());

        return ClienteResponse.from(cliente);
    }

    @Transactional
    public ClienteResponse actualizarCliente(UUID id, ClienteRequest request) {
        Cliente cliente = buscarCliente(id);
        cliente.setNombre(request.getNombre());
        cliente.setEmail(request.getEmail());
        cliente.setTelefono(request.getTelefono());
        cliente.setWeb(request.getWeb());
        cliente.setActivo(request.isActivo());
        return ClienteResponse.from(clienteRepository.save(cliente));
    }

    @Transactional
    public void eliminarCliente(UUID id) {
        if (!clienteRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Cliente", id);
        }
        clienteRepository.deleteById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    Cliente buscarCliente(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", id));
    }
}
