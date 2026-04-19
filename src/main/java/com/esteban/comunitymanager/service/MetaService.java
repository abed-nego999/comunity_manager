package com.esteban.comunitymanager.service;

import com.esteban.comunitymanager.dto.response.InsightFranjaResponse;
import com.esteban.comunitymanager.dto.response.ResultadoPublicacion;
import com.esteban.comunitymanager.exception.MetaPublicacionException;
import com.esteban.comunitymanager.model.*;
import com.esteban.comunitymanager.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.*;
import java.util.*;
import java.util.Collections;

/**
 * Servicio de Meta Graph API.
 * Obtiene y cachea insights de Facebook Page (page_fans_online_per_day).
 * En perfil dev, si no hay credenciales o la llamada falla, devuelve datos mock.
 */
@Service
public class MetaService {

    private static final Logger log = LoggerFactory.getLogger(MetaService.class);
    private static final String META_GRAPH_URL = "https://graph.facebook.com/v19.0";
    private static final Duration REFRESH_THRESHOLD = Duration.ofDays(7);
    private static final Duration MIN_SCHEDULED_AHEAD = Duration.ofMinutes(10);

    @Value("${meta.facebook.page-id:}")
    private String pageId;

    /** Token del sistema/usuario, leído de .env. Se usa para obtener el Page Access Token. */
    @Value("${meta.page-access-token:}")
    private String sistemaToken;

    @Value("${meta.mock-enabled:false}")
    private boolean mockEnabled;

    /** Page Access Token cacheado en memoria. Se obtiene de Meta la primera vez que se necesita. */
    private volatile String cachedPageToken;

    private final InsightPaginaRepository insightPaginaRepository;
    private final PlataformaRepository plataformaRepository;
    private final ClienteRepository clienteRepository;
    private final AdjuntoPublicacionRepository adjuntoPublicacionRepository;
    private final AdjuntoRepository adjuntoRepository;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    private RestClient metaClient;

    public MetaService(InsightPaginaRepository insightPaginaRepository,
                       PlataformaRepository plataformaRepository,
                       ClienteRepository clienteRepository,
                       AdjuntoPublicacionRepository adjuntoPublicacionRepository,
                       AdjuntoRepository adjuntoRepository,
                       StorageService storageService,
                       ObjectMapper objectMapper) {
        this.insightPaginaRepository = insightPaginaRepository;
        this.plataformaRepository = plataformaRepository;
        this.clienteRepository = clienteRepository;
        this.adjuntoPublicacionRepository = adjuntoPublicacionRepository;
        this.adjuntoRepository = adjuntoRepository;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void init() {
        metaClient = RestClient.builder()
                .baseUrl(META_GRAPH_URL)
                .build();
        log.info("[Meta] MetaService inicializado. Mock activo: {}", mockEnabled);
    }

    // ── Publicación en Facebook ───────────────────────────────────────────────
    //
    // TODO (Fase 2 — OAuth): La publicación real en Meta está deshabilitada hasta
    // implementar Facebook Login OAuth con Page Access Token.
    // Cuando se retome:
    //   1. Restaurar obtenerPageAccessToken() que llama a:
    //      GET /{pageId}?fields=access_token&access_token={sistemaToken}
    //   2. Restaurar publicarFeedSoloTexto() que llama a:
    //      POST /{pageId}/feed  (application/x-www-form-urlencoded)
    //      params: message, access_token, published, [scheduled_publish_time]
    //   3. Restaurar publicarFotoConTexto() que llama a:
    //      POST /{pageId}/photos  (multipart/form-data)
    //      params: source (binary), caption, access_token, published, [scheduled_publish_time]
    //   4. En modo dev usar published=false (borrador visible solo para admins).
    //   5. En modo prod: published=true (inmediato) o published=false + scheduled_publish_time.
    //   6. Extraer idExterno de: nodo.has("post_id") ? post_id : id
    //
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Simula la publicación en Facebook.
     * Pendiente implementar OAuth para llamada real a Meta Graph API.
     */
    public ResultadoPublicacion publicarEnFacebook(Publicacion publicacion) {
        Instant ahora = Instant.now();
        String idSimulado = "SIMULADO_" + UUID.randomUUID();
        Instant fechaPublicacionReal = (publicacion.getFechaPublicacion() != null)
                ? publicacion.getFechaPublicacion()
                : ahora;

        log.info("[Meta] Publicación SIMULADA — pendiente implementar OAuth (id={})", idSimulado);

        return new ResultadoPublicacion(idSimulado, ahora, fechaPublicacionReal);
    }

    // ── Insights ─────────────────────────────────────────────────────────────

    /**
     * Devuelve el JSON de insights para el cliente dado.
     * Usa caché en BBDD y refresca si tiene más de 7 días.
     * Devuelve null si no hay datos disponibles.
     */
    public String obtenerInsights(UUID clienteId) {
        Optional<Plataforma> facebookOpt = plataformaRepository.findByNombre("Facebook");
        if (facebookOpt.isEmpty()) {
            log.warn("[Meta] Plataforma 'Facebook' no encontrada en BBDD");
            return null;
        }
        Plataforma facebook = facebookOpt.get();

        Optional<InsightPagina> cached =
                insightPaginaRepository.findByClienteIdAndPlataformaId(clienteId, facebook.getId());

        boolean caducado = cached.isEmpty()
                || cached.get().getActualizadoEn().isBefore(Instant.now().minus(REFRESH_THRESHOLD));

        if (!caducado) {
            log.debug("[Meta] Devolviendo insights desde caché para cliente {}", clienteId);
            return cached.get().getDatosJson();
        }

        String json = fetchInsightsJson();
        if (json == null) {
            // Si falla, devolver caché antigua si existe
            return cached.map(InsightPagina::getDatosJson).orElse(null);
        }

        Optional<Cliente> clienteOpt = clienteRepository.findById(clienteId);
        if (clienteOpt.isEmpty()) return null;

        InsightPagina insight = cached.orElse(InsightPagina.builder()
                .cliente(clienteOpt.get())
                .plataforma(facebook)
                .build());
        insight.setDatosJson(json);
        insightPaginaRepository.save(insight);

        log.info("[Meta] Insights actualizados para cliente {}", clienteId);
        return json;
    }

    /**
     * Parsea el JSON de insights y devuelve un resumen legible con los mejores momentos.
     * Devuelve null si no se pueden parsear los datos.
     */
    public String resumirInsights(String datosJson) {
        try {
            JsonNode root = objectMapper.readTree(datosJson);
            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) return null;

            JsonNode values = data.get(0).path("values");
            if (!values.isArray() || values.isEmpty()) return null;

            // Acumular conteos por (dayOfWeek 1-7, hora 0-23)
            long[][] suma = new long[8][24];
            int[] diasPorDow = new int[8];

            for (JsonNode entry : values) {
                String endTime = entry.path("end_time").asText();
                LocalDate fecha = parseFechaEndTime(endTime);
                if (fecha == null) continue;
                int dow = fecha.getDayOfWeek().getValue(); // 1=Lun...7=Dom
                diasPorDow[dow]++;

                entry.path("value").fields().forEachRemaining(field -> {
                    int h = Integer.parseInt(field.getKey());
                    suma[dow][h] += field.getValue().asLong();
                });
            }

            // Para cada día, encontrar la ventana de 2 horas con más fans online (promedio)
            record MejorMomento(DayOfWeek dia, int horaInicio, double promedio) {}
            List<MejorMomento> momentos = new ArrayList<>();

            for (int dow = 1; dow <= 7; dow++) {
                if (diasPorDow[dow] == 0) continue;
                int dias = diasPorDow[dow];
                int mejorHora = 0;
                double mejorPromedio = 0;
                for (int h = 0; h <= 22; h++) {
                    double avg = (suma[dow][h] + suma[dow][h + 1]) / (2.0 * dias);
                    if (avg > mejorPromedio) {
                        mejorPromedio = avg;
                        mejorHora = h;
                    }
                }
                momentos.add(new MejorMomento(DayOfWeek.of(dow), mejorHora, mejorPromedio));
            }

            momentos.sort((a, b) -> Double.compare(b.promedio(), a.promedio()));
            List<MejorMomento> top = momentos.stream().limit(3).toList();

            if (top.isEmpty()) return null;

            String[] diasEs = {"", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
            StringBuilder sb = new StringBuilder("Mejores momentos para publicar:");
            top.forEach(m -> sb.append("\n- ")
                    .append(diasEs[m.dia().getValue()])
                    .append(" ").append(m.horaInicio()).append("h-").append(m.horaInicio() + 2).append("h"));

            return sb.toString();
        } catch (Exception e) {
            log.warn("[Meta] Error procesando insights JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Devuelve las N franjas horarias óptimas para publicar, en formato estructurado.
     * Usado por el frontend para calcular las próximas fechas recomendadas.
     */
    public List<InsightFranjaResponse> obtenerFranjasOptimas(UUID clienteId) {
        String datosJson = obtenerInsights(clienteId);
        if (datosJson == null || datosJson.isBlank()) return Collections.emptyList();
        try {
            JsonNode root = objectMapper.readTree(datosJson);
            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) return Collections.emptyList();

            JsonNode values = data.get(0).path("values");
            if (!values.isArray() || values.isEmpty()) return Collections.emptyList();

            long[][] suma = new long[8][24];
            int[] diasPorDow = new int[8];

            for (JsonNode entry : values) {
                LocalDate fecha = parseFechaEndTime(entry.path("end_time").asText());
                if (fecha == null) continue;
                int dow = fecha.getDayOfWeek().getValue();
                diasPorDow[dow]++;
                entry.path("value").fields().forEachRemaining(field -> {
                    int h = Integer.parseInt(field.getKey());
                    suma[dow][h] += field.getValue().asLong();
                });
            }

            record Slot(DayOfWeek dia, int hora, double promedio) {}
            List<Slot> slots = new ArrayList<>();
            for (int dow = 1; dow <= 7; dow++) {
                if (diasPorDow[dow] == 0) continue;
                int dias = diasPorDow[dow];
                int mejorHora = 0;
                double mejorPromedio = 0;
                for (int h = 0; h < 24; h++) {
                    double avg = (double) suma[dow][h] / dias;
                    if (avg >= mejorPromedio) { mejorPromedio = avg; mejorHora = h; }
                }
                slots.add(new Slot(DayOfWeek.of(dow), mejorHora, mejorPromedio));
            }

            slots.sort((a, b) -> Double.compare(b.promedio(), a.promedio()));
            return slots.stream().limit(3)
                    .map(s -> new InsightFranjaResponse(s.dia().name(), s.hora()))
                    .toList();
        } catch (Exception e) {
            log.warn("[Meta] Error al parsear franjas óptimas: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Llamada a Meta Graph API ──────────────────────────────────────────────

    private String fetchInsightsJson() {
        if (!pageId.isBlank() && !sistemaToken.isBlank()) {
            try {
                long since = LocalDate.now().minusDays(30).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
                long until = LocalDate.now().atStartOfDay().toEpochSecond(ZoneOffset.UTC);

                return metaClient.get()
                        .uri("/{pageId}/insights?metric=page_fans_online_per_day&period=day"
                                        + "&since={since}&until={until}&access_token={token}",
                                pageId, since, until, sistemaToken)
                        .retrieve()
                        .body(String.class);
            } catch (Exception e) {
                log.warn("[Meta] Error llamando a Meta Insights API: {}", e.getMessage());
                if (mockEnabled) {
                    log.info("[Meta] Usando datos mock como fallback");
                    return generarMockJson();
                }
                return null;
            }
        }
        if (mockEnabled) {
            log.info("[Meta] Sin credenciales de Meta — usando datos mock");
            return generarMockJson();
        }
        log.debug("[Meta] Sin credenciales de Meta y mock desactivado — omitiendo insights");
        return null;
    }

    /**
     * Genera JSON mock simulando `page_fans_online_per_day` para los últimos 14 días.
     * Picos: martes y jueves 19h-21h.
     */
    private String generarMockJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"data\":[{\"name\":\"page_fans_online_per_day\",\"period\":\"day\",\"values\":[");

        LocalDate hoy = LocalDate.now();
        boolean primero = true;
        for (int i = 13; i >= 0; i--) {
            LocalDate dia = hoy.minusDays(i);
            DayOfWeek dow = dia.getDayOfWeek();
            // end_time = inicio del día siguiente (Meta convention)
            LocalDate endDate = dia.plusDays(1);

            if (!primero) sb.append(",");
            primero = false;

            sb.append("{\"value\":{");
            for (int h = 0; h < 24; h++) {
                int val = 5;
                if ((dow == DayOfWeek.TUESDAY || dow == DayOfWeek.THURSDAY) && (h == 19 || h == 20)) val = 120;
                else if ((dow == DayOfWeek.TUESDAY || dow == DayOfWeek.THURSDAY) && (h == 18 || h == 21)) val = 70;
                else if ((dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) && (h == 12 || h == 13)) val = 55;
                if (h > 0) sb.append(",");
                sb.append("\"").append(h).append("\":").append(val);
            }
            sb.append("},\"end_time\":\"").append(endDate).append("T00:00:00+0000\"}");
        }

        sb.append("]}]}");
        return sb.toString();
    }

    private LocalDate parseFechaEndTime(String endTime) {
        try {
            // end_time es el inicio del día siguiente → restamos 1 día
            LocalDate nextDay = LocalDate.parse(endTime.substring(0, 10));
            return nextDay.minusDays(1);
        } catch (Exception e) {
            return null;
        }
    }
}
