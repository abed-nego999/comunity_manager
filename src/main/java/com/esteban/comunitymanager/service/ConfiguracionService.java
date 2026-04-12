package com.esteban.comunitymanager.service;

import com.esteban.comunitymanager.dto.request.ConfiguracionClienteRequest;
import com.esteban.comunitymanager.dto.request.InstruccionPlataformaRequest;
import com.esteban.comunitymanager.dto.response.ConfiguracionClienteResponse;
import com.esteban.comunitymanager.dto.response.InstruccionPlataformaResponse;
import com.esteban.comunitymanager.exception.ResourceNotFoundException;
import com.esteban.comunitymanager.model.ConfiguracionCliente;
import com.esteban.comunitymanager.model.InstruccionPlataforma;
import com.esteban.comunitymanager.model.Plataforma;
import com.esteban.comunitymanager.repository.ConfiguracionClienteRepository;
import com.esteban.comunitymanager.repository.InstruccionPlataformaRepository;
import com.esteban.comunitymanager.repository.PlataformaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private final ConfiguracionClienteRepository configuracionRepository;
    private final InstruccionPlataformaRepository instruccionRepository;
    private final PlataformaRepository plataformaRepository;

    @Transactional(readOnly = true)
    public ConfiguracionClienteResponse obtenerConfiguracion(UUID idCliente) {
        return ConfiguracionClienteResponse.from(buscarConfiguracion(idCliente));
    }

    @Transactional
    public ConfiguracionClienteResponse actualizarConfiguracion(UUID idCliente, ConfiguracionClienteRequest request) {
        ConfiguracionCliente config = buscarConfiguracion(idCliente);
        config.setTono(request.getTono());
        config.setRestricciones(request.getRestricciones());
        config.setCtaPredeterminada(request.getLlamadaALaAccion());
        return ConfiguracionClienteResponse.from(configuracionRepository.save(config));
    }

    @Transactional(readOnly = true)
    public List<InstruccionPlataformaResponse> listarInstrucciones(UUID idCliente) {
        ConfiguracionCliente config = buscarConfiguracion(idCliente);
        return instruccionRepository.findByConfiguracionId(config.getId()).stream()
                .map(InstruccionPlataformaResponse::from)
                .toList();
    }

    /**
     * Crea o actualiza la instrucción para una plataforma concreta de un cliente.
     * La combinación (configuracion, plataforma) es única — restricción en BBDD.
     */
    @Transactional
    public InstruccionPlataformaResponse upsertInstruccion(UUID idCliente, UUID idPlataforma,
                                                            InstruccionPlataformaRequest request) {
        ConfiguracionCliente config = buscarConfiguracion(idCliente);
        Plataforma plataforma = plataformaRepository.findById(idPlataforma)
                .orElseThrow(() -> ResourceNotFoundException.of("Plataforma", idPlataforma));

        InstruccionPlataforma instruccion = instruccionRepository
                .findByConfiguracionIdAndPlataformaId(config.getId(), idPlataforma)
                .orElse(InstruccionPlataforma.builder()
                        .configuracion(config)
                        .plataforma(plataforma)
                        .build());

        instruccion.setInstrucciones(request.getInstrucciones());
        return InstruccionPlataformaResponse.from(instruccionRepository.save(instruccion));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ConfiguracionCliente buscarConfiguracion(UUID idCliente) {
        return configuracionRepository.findByClienteId(idCliente)
                .orElseThrow(() -> ResourceNotFoundException.of("ConfiguracionCliente para el cliente", idCliente));
    }
}
