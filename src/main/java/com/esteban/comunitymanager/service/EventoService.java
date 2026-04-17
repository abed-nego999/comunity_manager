package com.esteban.comunitymanager.service;

import com.esteban.comunitymanager.claude.ClaudeRespuesta;
import com.esteban.comunitymanager.claude.ClaudeService;
import com.esteban.comunitymanager.dto.request.EventoRequest;
import com.esteban.comunitymanager.dto.request.MensajeRequest;
import com.esteban.comunitymanager.dto.response.EventoResponse;
import com.esteban.comunitymanager.dto.response.MensajeConversacionResponse;
import com.esteban.comunitymanager.dto.response.AdjuntoResponse;
import com.esteban.comunitymanager.dto.response.MensajeRespuestaResponse;
import com.esteban.comunitymanager.exception.ResourceNotFoundException;
import com.esteban.comunitymanager.model.*;
import com.esteban.comunitymanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EventoService {

    private static final Logger log = LoggerFactory.getLogger(EventoService.class);

    // Extrae UUIDs de adjuntos de las líneas "[Adjunto subido: nombre | ID: uuid]"
    private static final Pattern ADJUNTO_REF_PATTERN =
            Pattern.compile("\\[Adjunto subido: .+? \\| ID: ([0-9a-f\\-]+)\\]");

    private final EventoRepository eventoRepository;
    private final ClienteRepository clienteRepository;
    private final MensajeConversacionRepository mensajeRepository;
    private final RolConversacionRepository rolRepository;
    private final ConfiguracionClienteRepository configuracionRepository;
    private final InstruccionPlataformaRepository instruccionRepository;
    private final AdjuntoRepository adjuntoRepository;
    private final AdjuntoMensajeRepository adjuntoMensajeRepository;
    private final ClaudeService claudeService;

    @Transactional(readOnly = true)
    public List<EventoResponse> listarEventos(UUID clienteId) {
        return eventoRepository.findByClienteId(clienteId).stream()
                .map(EventoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventoResponse obtenerEvento(UUID id) {
        return EventoResponse.from(buscarEvento(id));
    }

    @Transactional
    public EventoResponse crearEvento(EventoRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", request.getClienteId()));

        Evento evento = eventoRepository.save(Evento.builder()
                .cliente(cliente)
                .nombre(request.getNombre())
                .fechaEvento(request.getFechaEvento())
                .descripcion(request.getDescripcion())
                .estado(request.getEstado() != null ? request.getEstado() : EstadoEvento.ACTIVO)
                .build());

        return EventoResponse.from(evento);
    }

    @Transactional
    public EventoResponse actualizarEvento(UUID id, EventoRequest request) {
        Evento evento = buscarEvento(id);
        evento.setNombre(request.getNombre());
        evento.setFechaEvento(request.getFechaEvento());
        evento.setDescripcion(request.getDescripcion());
        if (request.getEstado() != null) {
            evento.setEstado(request.getEstado());
        }
        return EventoResponse.from(eventoRepository.save(evento));
    }

    // ── Conversación ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MensajeConversacionResponse> obtenerConversacion(UUID idEvento) {
        buscarEvento(idEvento); // verifica que el evento exista
        return mensajeRepository.findByEventoIdOrderByEnviadoEnAsc(idEvento).stream()
                .map(m -> {
                    List<AdjuntoResponse> adjuntos = adjuntoMensajeRepository.findByIdMensaje(m.getId())
                            .stream()
                            .flatMap(am -> adjuntoRepository.findById(am.getIdAdjunto()).stream())
                            .map(AdjuntoResponse::from)
                            .toList();
                    log.debug("[Historial] Mensaje {}: {} adjuntos", m.getId(), adjuntos.size());
                    return MensajeConversacionResponse.from(m, adjuntos);
                })
                .toList();
    }

    /**
     * Procesa un mensaje del usuario:
     * 1. Guarda el mensaje del usuario en el historial.
     * 2. Construye el system prompt a partir de ConfiguracionCliente e InstruccionPlataforma.
     * 3. Envía el historial completo a Claude (o al mock en dev).
     * 4. Guarda la respuesta de Claude en el historial.
     * 5. Devuelve la respuesta con los IDs de publicaciones creadas por Tool Use.
     */
    @Transactional
    public MensajeRespuestaResponse enviarMensaje(UUID idEvento, MensajeRequest request) {
        Evento evento = buscarEvento(idEvento);

        RolConversacion rolUsuario = rolRepository.findByNombre("Usuario")
                .orElseThrow(() -> new ResourceNotFoundException("Rol 'Usuario' no encontrado en catálogo"));
        RolConversacion rolClaude = rolRepository.findByNombre("Claude")
                .orElseThrow(() -> new ResourceNotFoundException("Rol 'Claude' no encontrado en catálogo"));

        // Guardar mensaje del usuario
        MensajeConversacion mensajeUsuario = mensajeRepository.save(MensajeConversacion.builder()
                .evento(evento)
                .rol(rolUsuario)
                .contenido(request.getContenido())
                .build());

        // Enlazar adjuntos referenciados en el mensaje via ADJUNTO_MENSAJE
        vincularAdjuntosDelMensaje(mensajeUsuario.getId(), request.getContenido());

        // Obtener historial completo (incluyendo el mensaje recién guardado)
        List<MensajeConversacion> historial =
                mensajeRepository.findByEventoIdOrderByEnviadoEnAsc(idEvento);

        // Construir system prompt
        String systemPrompt = construirSystemPrompt(evento.getCliente().getId());

        // Llamar a Claude (mock en dev, real en prod)
        ClaudeRespuesta claudeRespuesta = claudeService.enviarConversacion(systemPrompt, historial, idEvento);

        // Guardar respuesta de Claude en el historial
        MensajeConversacion mensajeClaude = mensajeRepository.save(MensajeConversacion.builder()
                .evento(evento)
                .rol(rolClaude)
                .contenido(claudeRespuesta.getTextoRespuesta())
                .build());

        return MensajeRespuestaResponse.builder()
                .mensajeUsuarioId(mensajeUsuario.getId())
                .mensaje(MensajeConversacionResponse.from(mensajeClaude, List.of()))
                .publicacionesCreadas(claudeRespuesta.getPublicacionesCreadas())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Parsea las líneas "[Adjunto subido: nombre | ID: uuid]" del contenido del mensaje
     * y crea registros ADJUNTO_MENSAJE para cada UUID encontrado que exista en BBDD.
     */
    private void vincularAdjuntosDelMensaje(UUID mensajeId, String contenido) {
        if (contenido == null || contenido.isBlank()) return;
        Matcher matcher = ADJUNTO_REF_PATTERN.matcher(contenido);
        List<UUID> encontrados = new ArrayList<>();
        while (matcher.find()) {
            try { encontrados.add(UUID.fromString(matcher.group(1))); }
            catch (IllegalArgumentException ignored) { /* UUID malformado — ignorar */ }
        }
        for (UUID adjuntoId : encontrados) {
            if (adjuntoRepository.existsById(adjuntoId)
                    && !adjuntoMensajeRepository.existsByIdAdjuntoAndIdMensaje(adjuntoId, mensajeId)) {
                adjuntoMensajeRepository.save(AdjuntoMensaje.builder()
                        .idAdjunto(adjuntoId)
                        .idMensaje(mensajeId)
                        .build());
                log.debug("[Historial] Adjunto {} vinculado al mensaje {}", adjuntoId, mensajeId);
            }
        }
    }

    Evento buscarEvento(UUID id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Evento", id));
    }

    /**
     * Construye el system prompt de Claude concatenando:
     * 1. Configuración general del cliente (tono, restricciones, CTA).
     * 2. Instrucciones específicas por plataforma.
     */
    private String construirSystemPrompt(UUID idCliente) {
        StringBuilder sb = new StringBuilder();

        configuracionRepository.findByClienteId(idCliente).ifPresent(config -> {
            if (config.getTono() != null) {
                sb.append("TONO: ").append(config.getTono()).append("\n\n");
            }
            if (config.getRestricciones() != null) {
                sb.append("RESTRICCIONES: ").append(config.getRestricciones()).append("\n\n");
            }
            if (config.getCtaPredeterminada() != null) {
                sb.append("LLAMADA A LA ACCIÓN: ").append(config.getCtaPredeterminada()).append("\n\n");
            }

            List<com.esteban.comunitymanager.model.InstruccionPlataforma> instrucciones =
                    instruccionRepository.findByConfiguracionId(config.getId());
            if (!instrucciones.isEmpty()) {
                sb.append("INSTRUCCIONES POR PLATAFORMA:\n");
                instrucciones.forEach(i -> sb.append("- ")
                        .append(i.getPlataforma().getNombre())
                        .append(": ").append(i.getInstrucciones()).append("\n"));
            }
        });

        return sb.toString();
    }
}
