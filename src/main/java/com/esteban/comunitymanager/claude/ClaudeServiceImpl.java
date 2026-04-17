package com.esteban.comunitymanager.claude;

import com.esteban.comunitymanager.dto.request.PublicacionRequest;
import com.esteban.comunitymanager.dto.response.AdjuntoResponse;
import com.esteban.comunitymanager.dto.response.PublicacionResponse;
import com.esteban.comunitymanager.model.*;
import com.esteban.comunitymanager.repository.*;
import com.esteban.comunitymanager.service.AdjuntoService;
import com.esteban.comunitymanager.service.PublicacionService;
import com.esteban.comunitymanager.service.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación real de ClaudeService.
 * Llama a la API de Anthropic (claude-sonnet-4-6) con Tool Use.
 *
 * Los adjuntos del evento se incluyen en el system prompt como descripciones textuales.
 * El historial de mensajes se envía siempre como texto plano — sin base64.
 *
 * Loop agéntico:
 *  1. Envía historial + tools → API
 *  2. Si stop_reason=tool_use: ejecuta las herramientas, añade resultados y repite
 *  3. Si stop_reason=end_turn: devuelve el texto final
 */
@Service
public class ClaudeServiceImpl implements ClaudeService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeServiceImpl.class);

    private static final String ANTHROPIC_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-6";
    private static final int MAX_TOKENS = 8096;
    private static final int MAX_TOKENS_DESCRIPCION = 200;
    private static final int MAX_ITERACIONES = 10;

    @Value("${ANTHROPIC_API_KEY:}")
    private String apiKey;

    @Value("classpath:prompts/system-prompt-base.txt")
    private Resource systemPromptBaseResource;

    private final ObjectMapper objectMapper;
    private final PublicacionService publicacionService;
    private final AdjuntoService adjuntoService;
    private final EventoRepository eventoRepository;
    private final ClienteRepository clienteRepository;
    private final AdjuntoRepository adjuntoRepository;
    private final PublicacionRepository publicacionRepository;
    private final TipoPublicacionRepository tipoPublicacionRepository;
    private final StorageService storageService;

    private String systemPromptBase;
    private RestClient anthropicClient;

    public ClaudeServiceImpl(ObjectMapper objectMapper,
                             PublicacionService publicacionService,
                             AdjuntoService adjuntoService,
                             EventoRepository eventoRepository,
                             ClienteRepository clienteRepository,
                             AdjuntoRepository adjuntoRepository,
                             PublicacionRepository publicacionRepository,
                             TipoPublicacionRepository tipoPublicacionRepository,
                             StorageService storageService) {
        this.objectMapper = objectMapper;
        this.publicacionService = publicacionService;
        this.adjuntoService = adjuntoService;
        this.eventoRepository = eventoRepository;
        this.clienteRepository = clienteRepository;
        this.adjuntoRepository = adjuntoRepository;
        this.publicacionRepository = publicacionRepository;
        this.tipoPublicacionRepository = tipoPublicacionRepository;
        this.storageService = storageService;
    }

    @PostConstruct
    void init() throws IOException {
        anthropicClient = RestClient.builder()
                .baseUrl(ANTHROPIC_URL)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();

        systemPromptBase = systemPromptBaseResource.getContentAsString(StandardCharsets.UTF_8);
        log.info("[Claude] ClaudeServiceImpl inicializado con modelo {}", MODEL);
    }

    // ── Conversación principal ────────────────────────────────────────────────

    @Override
    public ClaudeRespuesta enviarConversacion(String systemPrompt, List<MensajeConversacion> historial, UUID eventoId) {
        Evento evento = eventoRepository.findById(eventoId).orElse(null);
        List<TipoPublicacion> tipos = tipoPublicacionRepository.findAll();
        List<Publicacion> publicacionesEvento = publicacionRepository.findByEventoId(eventoId);
        List<Adjunto> todosAdjuntos = adjuntoRepository.findByEventoId(eventoId);

        // Los adjuntos van en el system prompt como texto (descripcionIa) — nunca en base64
        String seccionEvento = buildSeccionEvento(evento, tipos, publicacionesEvento, todosAdjuntos);
        String fullSystemPrompt = systemPromptBase.trim()
                + "\n\n" + systemPrompt.trim()
                + "\n\n" + seccionEvento;

        log.debug("[Claude] Adjuntos del evento {}: {} en system prompt", eventoId, todosAdjuntos.size());

        // Mensajes solo como texto plano — sin ningún bloque base64
        List<Object> messages = buildMessages(historial);

        // Log del último mensaje del usuario antes de enviarlo a Anthropic
        if (!messages.isEmpty()) {
            Object ultimo = messages.get(messages.size() - 1);
            String preview = ultimo.toString();
            log.info("[Claude] Último msg → Anthropic: {}",
                    preview.length() > 200 ? preview.substring(0, 200) + "…" : preview);
        }

        List<UUID> publicacionesCreadas = new ArrayList<>();
        String textoFinal = "";

        for (int iteracion = 0; iteracion < MAX_ITERACIONES; iteracion++) {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL);
            requestBody.put("max_tokens", MAX_TOKENS);
            requestBody.put("system", fullSystemPrompt);
            requestBody.put("tools", buildTools());
            requestBody.put("messages", messages);

            JsonNode response = llamarAnthropicApi(requestBody);
            String stopReason = response.path("stop_reason").asText();
            JsonNode content = response.path("content");

            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    textoFinal = block.path("text").asText();
                }
            }

            messages.add(Map.of(
                    "role", "assistant",
                    "content", objectMapper.convertValue(content, Object.class)
            ));

            if (!"tool_use".equals(stopReason)) {
                log.debug("[Claude] Loop finalizado en iteración {} con stop_reason={}", iteracion, stopReason);
                break;
            }

            List<Map<String, Object>> toolResults = new ArrayList<>();
            for (JsonNode block : content) {
                if ("tool_use".equals(block.path("type").asText())) {
                    String toolName = block.path("name").asText();
                    String toolUseId = block.path("id").asText();
                    JsonNode input = block.path("input");

                    log.info("[Claude] Tool call: {} | input: {}", toolName, input);
                    String resultado = ejecutarTool(toolName, input, eventoId, publicacionesCreadas);
                    log.info("[Claude] Tool result: {}", resultado);

                    toolResults.add(Map.of(
                            "type", "tool_result",
                            "tool_use_id", toolUseId,
                            "content", resultado
                    ));
                }
            }
            messages.add(Map.of("role", "user", "content", toolResults));
        }

        return ClaudeRespuesta.builder()
                .textoRespuesta(textoFinal)
                .publicacionesCreadas(publicacionesCreadas)
                .build();
    }

    // ── Descripción minimalista de adjunto ────────────────────────────────────

    @Override
    public String describirAdjunto(Adjunto adjunto) {
        try {
            byte[] bytes = storageService.leerFichero(adjunto.getRutaFichero());
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String tipoMime = adjunto.getTipoMime();
            String blockType = tipoMime.startsWith("image/") ? "image" : "document";

            Map<String, Object> source = Map.of(
                    "type", "base64",
                    "media_type", tipoMime,
                    "data", base64
            );
            Map<String, Object> fileBlock = Map.of("type", blockType, "source", source);
            Map<String, Object> textBlock = Map.of("type", "text", "text",
                    "Describe esta imagen en 3-5 líneas máximo. Incluye: "
                    + "personas (número, vestimenta, actitud), lugar/ambiente, "
                    + "colores principales y cualquier texto visible. "
                    + "Sin formato, sin listas, solo texto plano.");

            Map<String, Object> message = Map.of("role", "user",
                    "content", List.of(fileBlock, textBlock));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL);
            requestBody.put("max_tokens", MAX_TOKENS_DESCRIPCION);
            requestBody.put("messages", List.of(message));

            JsonNode response = llamarAnthropicApi(requestBody);
            for (JsonNode block : response.path("content")) {
                if ("text".equals(block.path("type").asText())) {
                    log.debug("[Claude] Descripción generada para '{}'", adjunto.getNombreFichero());
                    return block.path("text").asText();
                }
            }
            return "";
        } catch (IOException e) {
            log.warn("[Claude] No se pudo leer '{}' para describir: {}", adjunto.getRutaFichero(), e.getMessage());
            return "";
        }
    }

    // ── System prompt: sección del evento ─────────────────────────────────────

    private String buildSeccionEvento(Evento evento, List<TipoPublicacion> tipos,
                                       List<Publicacion> publicaciones, List<Adjunto> adjuntos) {
        if (evento == null) return "";
        StringBuilder sb = new StringBuilder();

        if (evento.getCliente() != null) {
            sb.append("CLIENTE ACTIVO:\n");
            sb.append("ID: ").append(evento.getCliente().getId()).append("\n");
            sb.append("Nombre: ").append(evento.getCliente().getNombre()).append("\n\n");
        }

        sb.append("EVENTO ACTUAL:\n");
        sb.append("ID: ").append(evento.getId()).append("\n");
        sb.append("Nombre: ").append(evento.getNombre()).append("\n");
        if (evento.getFechaEvento() != null) {
            sb.append("Fecha: ").append(evento.getFechaEvento()).append("\n");
        }
        if (evento.getDescripcion() != null && !evento.getDescripcion().isBlank()) {
            sb.append("Descripción: ").append(evento.getDescripcion()).append("\n");
        }

        if (adjuntos != null && !adjuntos.isEmpty()) {
            sb.append("\nADJUNTOS DISPONIBLES EN ESTE EVENTO:\n");
            adjuntos.forEach(a -> {
                sb.append("- ID: ").append(a.getId())
                        .append(" | ").append(a.getNombreFichero())
                        .append(" | ").append(a.getTipoMime());
                if (a.getDescripcionIa() != null && !a.getDescripcionIa().isBlank()) {
                    String desc = a.getDescripcionIa();
                    if (desc.length() > 300) desc = desc.substring(0, 297) + "...";
                    sb.append(" | Descripción: ").append(desc);
                } else {
                    sb.append(" | (sin descripción)");
                }
                sb.append("\n");
            });
        }

        if (tipos != null && !tipos.isEmpty()) {
            sb.append("\nTIPOS DE PUBLICACIÓN DISPONIBLES:\n");
            tipos.forEach(t -> sb.append("- ID: ").append(t.getId())
                    .append(" | ").append(t.getPlataforma().getNombre())
                    .append(" - ").append(t.getNombre())
                    .append(" | automatica: ").append(t.isPublicacionAutomatica())
                    .append("\n"));
        }

        if (publicaciones != null && !publicaciones.isEmpty()) {
            sb.append("\nPUBLICACIONES DEL EVENTO:\n");
            publicaciones.forEach(p -> {
                String plataforma = p.getTipoPublicacion().getPlataforma().getNombre();
                String tipo       = p.getTipoPublicacion().getNombre();
                sb.append("- ID: ").append(p.getId())
                        .append(" | ").append(plataforma).append(" - ").append(tipo)
                        .append(" | Estado: ").append(p.getEstado());
                if (p.getFechaGeneracion() != null) {
                    sb.append(" | Generado: ").append(p.getFechaGeneracion());
                }
                if (p.getFeedbackUsuario() != null && !p.getFeedbackUsuario().isBlank()) {
                    sb.append(" | Feedback: ").append(p.getFeedbackUsuario());
                }
                sb.append("\n");
            });
        }

        return sb.toString().trim();
    }

    // ── Construcción de mensajes — solo texto plano ───────────────────────────

    /**
     * Convierte el historial en el array de mensajes para Anthropic.
     * Los adjuntos NO viajan en los mensajes — van en el system prompt como texto.
     * Deduplica mensajes consecutivos del mismo rol (ej: reintento sin respuesta previa).
     */
    private List<Object> buildMessages(List<MensajeConversacion> historial) {
        List<MensajeConversacion> deduplicado = new ArrayList<>();
        for (MensajeConversacion m : historial) {
            if (!deduplicado.isEmpty()) {
                String rolPrev = mapRol(deduplicado.get(deduplicado.size() - 1).getRol().getNombre());
                String rolCurr = mapRol(m.getRol().getNombre());
                if (rolPrev.equals(rolCurr)) {
                    deduplicado.remove(deduplicado.size() - 1);
                }
            }
            deduplicado.add(m);
        }

        return deduplicado.stream()
                .map(m -> (Object) Map.of(
                        "role", mapRol(m.getRol().getNombre()),
                        "content", m.getContenido()))
                .collect(Collectors.toList());
    }

    // ── Llamada HTTP ──────────────────────────────────────────────────────────

    private JsonNode llamarAnthropicApi(Map<String, Object> body) {
        for (int intento = 0; intento <= 1; intento++) {
            try {
                return anthropicClient.post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 429 && intento == 0) {
                    log.info("[Claude] Rate limit alcanzado, reintentando en 60s...");
                    try { Thread.sleep(60_000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }
                if (e.getStatusCode().value() == 429) {
                    throw new RuntimeException("Claude está ocupado, espera un momento y reintenta.", e);
                }
                log.error("[Claude] Error HTTP en la llamada a Anthropic: {} {}", e.getStatusCode(), e.getMessage());
                throw new RuntimeException("Error comunicándose con Claude: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("[Claude] Error en la llamada a la API de Anthropic: {}", e.getMessage(), e);
                throw new RuntimeException("Error comunicándose con Claude: " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("Claude está ocupado, espera un momento y reintenta.");
    }

    // ── Ejecución de herramientas ─────────────────────────────────────────────

    private String ejecutarTool(String toolName, JsonNode input, UUID eventoId, List<UUID> publicacionesCreadas) {
        try {
            return switch (toolName) {
                case "crear_publicacion" -> {
                    PublicacionRequest req = new PublicacionRequest();
                    req.setEventoId(UUID.fromString(input.path("eventoId").asText()));
                    req.setIdTipoPublicacion(UUID.fromString(input.path("idTipoPublicacion").asText()));
                    req.setTextoGenerado(input.path("textoGenerado").asText());
                    PublicacionResponse pub = publicacionService.crearPublicacion(req);
                    publicacionesCreadas.add(pub.getId());
                    yield "OK. Publicación creada."
                            + " | ID: " + pub.getId()
                            + " | Plataforma: " + pub.getTipoPublicacion().getPlataforma().getNombre()
                            + " | Tipo: " + pub.getTipoPublicacion().getNombre()
                            + " | Estado: " + pub.getEstado();
                }
                case "actualizar_publicacion" -> {
                    UUID pubId = UUID.fromString(input.path("id").asText());
                    PublicacionRequest req = new PublicacionRequest();
                    req.setEventoId(UUID.fromString(input.path("eventoId").asText()));
                    req.setIdTipoPublicacion(UUID.fromString(input.path("idTipoPublicacion").asText()));
                    req.setTextoGenerado(input.path("textoGenerado").asText());
                    PublicacionResponse pub = publicacionService.actualizarPublicacion(pubId, req);
                    yield "OK. Publicación " + pub.getId() + " actualizada correctamente.";
                }
                case "crear_evento" -> {
                    UUID clienteId = UUID.fromString(input.path("clienteId").asText());
                    Cliente cliente = clienteRepository.findById(clienteId)
                            .orElseThrow(() -> new RuntimeException("Cliente " + clienteId + " no encontrado"));
                    Evento.EventoBuilder builder = Evento.builder()
                            .cliente(cliente)
                            .nombre(input.path("nombre").asText())
                            .estado(EstadoEvento.BORRADOR);
                    if (input.has("descripcion") && !input.path("descripcion").isNull()) {
                        builder.descripcion(input.path("descripcion").asText());
                    }
                    if (input.has("fechaEvento") && !input.path("fechaEvento").isNull()
                            && !input.path("fechaEvento").asText().isBlank()) {
                        builder.fechaEvento(LocalDate.parse(input.path("fechaEvento").asText()));
                    }
                    Evento saved = eventoRepository.save(builder.build());
                    yield "OK. Evento creado. | ID: " + saved.getId() + " | Nombre: " + saved.getNombre();
                }
                case "asociar_adjunto_publicacion" -> {
                    UUID adjuntoId = UUID.fromString(input.path("adjuntoId").asText());
                    UUID pubId = UUID.fromString(input.path("publicacionId").asText());
                    AdjuntoResponse adj = adjuntoService.asociarAPublicacion(adjuntoId, pubId);
                    yield "OK. Adjunto " + adjuntoId + " asociado a publicación " + pubId
                            + " | Fichero: " + adj.getNombreFichero();
                }
                case "listar_adjuntos_evento" -> {
                    UUID evId = UUID.fromString(input.path("eventoId").asText());
                    List<Adjunto> adjuntos = adjuntoRepository.findByEventoId(evId);
                    if (adjuntos.isEmpty()) yield "No hay adjuntos en el evento.";
                    yield adjuntos.stream()
                            .map(a -> "- ID: " + a.getId()
                                    + " | " + a.getNombreFichero()
                                    + " | " + a.getTipoMime()
                                    + (a.getDescripcionIa() != null ? " | Descrito: sí" : " | Descrito: no"))
                            .collect(Collectors.joining("\n"));
                }
                case "listar_adjuntos_publicacion" -> {
                    UUID pubId = UUID.fromString(input.path("publicacionId").asText());
                    List<AdjuntoResponse> adjResp = adjuntoService.listarPorPublicacion(pubId);
                    if (adjResp.isEmpty()) yield "No hay adjuntos asociados a esta publicación.";
                    yield adjResp.stream()
                            .map(a -> "- ID: " + a.getId()
                                    + " | " + a.getNombreFichero()
                                    + " | " + a.getTipoMime())
                            .collect(Collectors.joining("\n"));
                }
                case "guardar_descripcion_adjunto" -> {
                    UUID adjId = UUID.fromString(input.path("adjuntoId").asText());
                    String desc = input.path("descripcion").asText();
                    AdjuntoResponse adj = adjuntoService.actualizarDescripcionIa(adjId, desc);
                    log.debug("[Claude] Descripción guardada para adjunto '{}'", adj.getNombreFichero());
                    yield "OK. Descripción guardada para el adjunto '" + adj.getNombreFichero() + "'.";
                }
                default -> "ERROR: Herramienta desconocida: " + toolName;
            };
        } catch (Exception e) {
            log.error("[Claude] Error ejecutando herramienta {}: {}", toolName, e.getMessage(), e);
            return "ERROR al ejecutar " + toolName + ": " + e.getMessage();
        }
    }

    // ── Definición de herramientas ────────────────────────────────────────────

    private List<Map<String, Object>> buildTools() {
        return List.of(
                buildTool(
                        "crear_publicacion",
                        "Crea una nueva publicación en estado PENDIENTE para un evento. "
                                + "Úsala para generar contenido para una red social concreta.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "eventoId", Map.of("type", "string",
                                                "description", "UUID del evento al que pertenece la publicación"),
                                        "idTipoPublicacion", Map.of("type", "string",
                                                "description", "UUID del tipo de publicación (determina plataforma y tipo de contenido)"),
                                        "textoGenerado", Map.of("type", "string",
                                                "description", "Contenido textual completo de la publicación, listo para publicar")
                                ),
                                "required", List.of("eventoId", "idTipoPublicacion", "textoGenerado")
                        )
                ),
                buildTool(
                        "actualizar_publicacion",
                        "Actualiza el contenido de una publicación existente. "
                                + "Úsala cuando el usuario pida cambios sobre una publicación ya generada.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "id", Map.of("type", "string",
                                                "description", "UUID de la publicación a actualizar"),
                                        "eventoId", Map.of("type", "string",
                                                "description", "UUID del evento al que pertenece"),
                                        "idTipoPublicacion", Map.of("type", "string",
                                                "description", "UUID del tipo de publicación"),
                                        "textoGenerado", Map.of("type", "string",
                                                "description", "Nuevo contenido textual de la publicación")
                                ),
                                "required", List.of("id", "eventoId", "idTipoPublicacion", "textoGenerado")
                        )
                ),
                buildTool(
                        "crear_evento",
                        "Crea un nuevo evento en la base de datos como contenedor de trabajo.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "clienteId", Map.of("type", "string",
                                                "description", "UUID del cliente al que pertenece el evento"),
                                        "nombre", Map.of("type", "string",
                                                "description", "Nombre descriptivo del evento"),
                                        "descripcion", Map.of("type", "string",
                                                "description", "Descripción detallada del evento (opcional)"),
                                        "fechaEvento", Map.of("type", "string",
                                                "description", "Fecha del evento en formato YYYY-MM-DD (opcional)")
                                ),
                                "required", List.of("clienteId", "nombre")
                        )
                ),
                buildTool(
                        "asociar_adjunto_publicacion",
                        "Asocia un adjunto ya subido al evento a una publicación concreta. "
                                + "Usa el ID del adjunto obtenido con listar_adjuntos_evento.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "adjuntoId", Map.of("type", "string",
                                                "description", "UUID del adjunto a asociar"),
                                        "publicacionId", Map.of("type", "string",
                                                "description", "UUID de la publicación a la que asociar el adjunto")
                                ),
                                "required", List.of("adjuntoId", "publicacionId")
                        )
                ),
                buildTool(
                        "listar_adjuntos_evento",
                        "Lista todos los adjuntos del evento (imágenes, PDFs, vídeos) con sus IDs. "
                                + "Usa sus IDs para asociarlos a publicaciones con asociar_adjunto_publicacion.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "eventoId", Map.of("type", "string",
                                                "description", "UUID del evento cuyos adjuntos se listan")
                                ),
                                "required", List.of("eventoId")
                        )
                ),
                buildTool(
                        "listar_adjuntos_publicacion",
                        "Lista los adjuntos ya asociados a una publicación concreta.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "publicacionId", Map.of("type", "string",
                                                "description", "UUID de la publicación cuyos adjuntos se listan")
                                ),
                                "required", List.of("publicacionId")
                        )
                ),
                buildTool(
                        "guardar_descripcion_adjunto",
                        "Guarda una descripción textual de un fichero adjunto que has analizado. "
                                + "Úsala si recibes base64 de un adjunto sin descripción previa.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "adjuntoId", Map.of("type", "string",
                                                "description", "UUID del adjunto descrito"),
                                        "descripcion", Map.of("type", "string",
                                                "description", "Descripción detallada del contenido del fichero")
                                ),
                                "required", List.of("adjuntoId", "descripcion")
                        )
                )
        );
    }

    private Map<String, Object> buildTool(String name, String description, Map<String, Object> inputSchema) {
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", name);
        tool.put("description", description);
        tool.put("input_schema", inputSchema);
        return tool;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String mapRol(String nombre) {
        return switch (nombre) {
            case "Claude" -> "assistant";
            default -> "user";
        };
    }
}
