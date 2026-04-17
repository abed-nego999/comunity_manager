package com.esteban.comunitymanager.service;

import com.esteban.comunitymanager.dto.request.ConfiguracionClienteRequest;
import com.esteban.comunitymanager.dto.request.InstruccionPlataformaRequest;
import com.esteban.comunitymanager.dto.response.ConfiguracionClienteResponse;
import com.esteban.comunitymanager.dto.response.InstruccionPlataformaResponse;
import com.esteban.comunitymanager.exception.ResourceNotFoundException;
import com.esteban.comunitymanager.model.Cliente;
import com.esteban.comunitymanager.model.ConfiguracionCliente;
import com.esteban.comunitymanager.model.InstruccionPlataforma;
import com.esteban.comunitymanager.model.Plataforma;
import com.esteban.comunitymanager.repository.ClienteRepository;
import com.esteban.comunitymanager.repository.ConfiguracionClienteRepository;
import com.esteban.comunitymanager.repository.InstruccionPlataformaRepository;
import com.esteban.comunitymanager.repository.PlataformaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionService.class);

    private final ConfiguracionClienteRepository configuracionRepository;
    private final InstruccionPlataformaRepository instruccionRepository;
    private final PlataformaRepository plataformaRepository;
    private final ClienteRepository clienteRepository;
    private final ObjectMapper objectMapper;
    private final StorageService storageService;

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
        ConfiguracionClienteResponse response = ConfiguracionClienteResponse.from(configuracionRepository.save(config));
        generarBackup(idCliente);
        return response;
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
        InstruccionPlataformaResponse response = InstruccionPlataformaResponse.from(instruccionRepository.save(instruccion));
        generarBackup(idCliente);
        return response;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ConfiguracionCliente buscarConfiguracion(UUID idCliente) {
        return configuracionRepository.findByClienteId(idCliente)
                .orElseThrow(() -> ResourceNotFoundException.of("ConfiguracionCliente para el cliente", idCliente));
    }

    /**
     * Exporta la configuración completa del cliente a un fichero JSON en:
     * storage/clientes/{id}_{nombre}/config-backup.json
     *
     * Se ejecuta automáticamente tras cada PUT de configuración o upsert de instrucción.
     * No interrumpe la operación principal si falla — solo registra una advertencia.
     */
    private void generarBackup(UUID idCliente) {
        try {
            Cliente cliente = clienteRepository.findById(idCliente).orElse(null);
            if (cliente == null) return;

            ConfiguracionCliente config = configuracionRepository.findByClienteId(idCliente).orElse(null);
            List<InstruccionPlataforma> instrucciones = config != null
                    ? instruccionRepository.findByConfiguracionId(config.getId())
                    : List.of();

            Map<String, Object> backup = new LinkedHashMap<>();
            backup.put("generadoEn", Instant.now().toString());

            Map<String, Object> clienteData = new LinkedHashMap<>();
            clienteData.put("id", cliente.getId().toString());
            clienteData.put("nombre", cliente.getNombre());
            clienteData.put("email", cliente.getEmail());
            clienteData.put("web", cliente.getWeb());
            backup.put("cliente", clienteData);

            if (config != null) {
                Map<String, Object> configData = new LinkedHashMap<>();
                configData.put("tono", config.getTono());
                configData.put("restricciones", config.getRestricciones());
                configData.put("llamadaALaAccion", config.getCtaPredeterminada());
                backup.put("configuracion", configData);
            }

            List<Map<String, String>> instrList = instrucciones.stream()
                    .map(i -> {
                        Map<String, String> m = new LinkedHashMap<>();
                        m.put("plataforma", i.getPlataforma().getNombre());
                        m.put("instrucciones", i.getInstrucciones());
                        return m;
                    })
                    .toList();
            backup.put("instrucciones", instrList);

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(backup);
            storageService.guardarJson(json, storageService.resolverRutaConfigBackup(cliente.getId(), cliente.getNombre()));
            log.info("[Config] Backup generado para cliente '{}'", cliente.getNombre());
        } catch (Exception e) {
            log.warn("[Config] No se pudo generar el backup de configuración del cliente {}: {}", idCliente, e.getMessage());
        }
    }
}
