package com.esteban.comunitymanager.service;

import com.esteban.comunitymanager.dto.request.CredencialRequest;
import com.esteban.comunitymanager.dto.response.CredencialResponse;
import com.esteban.comunitymanager.exception.ResourceNotFoundException;
import com.esteban.comunitymanager.model.Cliente;
import com.esteban.comunitymanager.model.Credencial;
import com.esteban.comunitymanager.model.Plataforma;
import com.esteban.comunitymanager.repository.ClienteRepository;
import com.esteban.comunitymanager.repository.CredencialRepository;
import com.esteban.comunitymanager.repository.PlataformaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Gestiona las credenciales de APIs externas por cliente y plataforma.
 *
 * Los tokens se pasan en claro desde CredencialRequest.
 * AesConverter (definido en la entidad Credencial) los cifra de forma
 * transparente al persistir y los descifra al leer — el servicio opera
 * siempre con valores en claro a nivel de objeto Java.
 *
 * IMPORTANTE: los tokens NUNCA se incluyen en CredencialResponse.
 * Solo se devuelven metadatos (plataforma, fecha de expiración).
 */
@Service
@RequiredArgsConstructor
public class CredencialService {

    private final CredencialRepository credencialRepository;
    private final ClienteRepository clienteRepository;
    private final PlataformaRepository plataformaRepository;

    @Transactional(readOnly = true)
    public List<CredencialResponse> listarCredenciales(UUID idCliente) {
        return credencialRepository.findByClienteId(idCliente).stream()
                .map(CredencialResponse::from)
                .toList();
    }

    /**
     * Crea o actualiza la credencial para la combinación (cliente, plataforma).
     * Los tokens en claro del request se cifran automáticamente por AesConverter.
     */
    @Transactional
    public CredencialResponse upsertCredencial(UUID idCliente, UUID idPlataforma, CredencialRequest request) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", idCliente));
        Plataforma plataforma = plataformaRepository.findById(idPlataforma)
                .orElseThrow(() -> ResourceNotFoundException.of("Plataforma", idPlataforma));

        Credencial credencial = credencialRepository
                .findByClienteIdAndPlataformaId(idCliente, idPlataforma)
                .orElse(Credencial.builder()
                        .cliente(cliente)
                        .plataforma(plataforma)
                        .build());

        // Los tokens se asignan en claro; AesConverter los cifra al persistir
        credencial.setAccessTokenCifrado(request.getAccessToken());
        credencial.setRefreshTokenCifrado(request.getRefreshToken());
        credencial.setExpiraEn(request.getExpiraEn());

        // Los tokens NO se devuelven en el response — solo metadatos
        return CredencialResponse.from(credencialRepository.save(credencial));
    }

    @Transactional
    public void eliminarCredencial(UUID idCliente, UUID idPlataforma) {
        Credencial credencial = credencialRepository
                .findByClienteIdAndPlataformaId(idCliente, idPlataforma)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Credencial no encontrada para cliente " + idCliente + " y plataforma " + idPlataforma));
        credencialRepository.delete(credencial);
    }
}
