package com.esteban.comunitymanager.claude;

import com.esteban.comunitymanager.model.Adjunto;
import com.esteban.comunitymanager.model.*;
import com.esteban.comunitymanager.repository.EventoRepository;
import com.esteban.comunitymanager.repository.PublicacionRepository;
import com.esteban.comunitymanager.repository.TipoPublicacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Mock de ClaudeService. Desactivado — solo se carga con el perfil 'mock'.
 * Mantenido en el código como referencia y para pruebas manuales.
 *
 * Para activarlo: añadir 'mock' al perfil activo de Spring.
 */
@Service
@Profile("mock")
public class ClaudeServiceMock implements ClaudeService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeServiceMock.class);

    // UUID del tipo Instagram Post seeded en data.sql
    private static final UUID TIPO_INSTAGRAM_POST = UUID.fromString("00000000-0000-0000-0001-000000000004");
    // UUID del tipo Facebook Post seeded en data.sql
    private static final UUID TIPO_FACEBOOK_POST = UUID.fromString("00000000-0000-0000-0001-000000000001");

    private final PublicacionRepository publicacionRepository;
    private final TipoPublicacionRepository tipoPublicacionRepository;
    private final EventoRepository eventoRepository;

    public ClaudeServiceMock(PublicacionRepository publicacionRepository,
                             TipoPublicacionRepository tipoPublicacionRepository,
                             EventoRepository eventoRepository) {
        this.publicacionRepository = publicacionRepository;
        this.tipoPublicacionRepository = tipoPublicacionRepository;
        this.eventoRepository = eventoRepository;
    }

    @Override
    @Transactional
    public ClaudeRespuesta enviarConversacion(String systemPrompt, List<MensajeConversacion> historial, UUID eventoId) {
        log.debug("[MOCK] Claude recibe {} mensajes para el evento {}", historial.size(), eventoId);

        String ultimoMensaje = obtenerUltimoMensajeUsuario(historial);
        List<UUID> publicacionesCreadas = new ArrayList<>();

        // Simular creación de publicaciones de prueba
        Optional<Evento> eventoOpt = eventoRepository.findById(eventoId);
        if (eventoOpt.isPresent()) {
            Evento evento = eventoOpt.get();
            publicacionesCreadas.addAll(crearPublicacionesSimuladas(evento, ultimoMensaje));
        }

        String respuesta = construirRespuestaMock(ultimoMensaje, publicacionesCreadas);

        log.debug("[MOCK] Devuelve respuesta con {} publicaciones creadas", publicacionesCreadas.size());
        return ClaudeRespuesta.builder()
                .textoRespuesta(respuesta)
                .publicacionesCreadas(publicacionesCreadas)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<UUID> crearPublicacionesSimuladas(Evento evento, String mensajeUsuario) {
        List<UUID> ids = new ArrayList<>();

        // Crear publicación de Instagram
        tipoPublicacionRepository.findById(TIPO_INSTAGRAM_POST).ifPresent(tipo -> {
            Publicacion pub = publicacionRepository.save(Publicacion.builder()
                    .evento(evento)
                    .tipoPublicacion(tipo)
                    .textoGenerado(generarTextoSimulado("Instagram", mensajeUsuario))
                    .estado(EstadoPublicacion.PENDIENTE)
                    .build());
            ids.add(pub.getId());
            log.debug("[MOCK] Publicación simulada creada — ID: {} | Instagram Post", pub.getId());
        });

        // Crear publicación de Facebook si el mensaje contiene alguna pista
        if (mensajeUsuario.toLowerCase().contains("facebook") || mensajeUsuario.toLowerCase().contains("todas")) {
            tipoPublicacionRepository.findById(TIPO_FACEBOOK_POST).ifPresent(tipo -> {
                Publicacion pub = publicacionRepository.save(Publicacion.builder()
                        .evento(evento)
                        .tipoPublicacion(tipo)
                        .textoGenerado(generarTextoSimulado("Facebook", mensajeUsuario))
                        .estado(EstadoPublicacion.PENDIENTE)
                        .build());
                ids.add(pub.getId());
                log.debug("[MOCK] Publicación simulada creada — ID: {} | Facebook Post", pub.getId());
            });
        }

        return ids;
    }

    private String generarTextoSimulado(String plataforma, String mensajeUsuario) {
        return String.format(
                "[SIMULADO — perfil dev] Publicación de %s generada a partir de: \"%s\"%n%n"
                        + "En producción, Claude generaría aquí el contenido real adaptado a %s, "
                        + "siguiendo el tono y las instrucciones configuradas para este cliente.%n%n"
                        + "#CommunityManager #Dev #Simulado",
                plataforma,
                truncar(mensajeUsuario, 80),
                plataforma
        );
    }

    private String construirRespuestaMock(String mensajeUsuario, List<UUID> publicacionesCreadas) {
        StringBuilder sb = new StringBuilder();
        sb.append("[MOCK — perfil dev] ");

        if (mensajeUsuario.isBlank()) {
            sb.append("He recibido tu mensaje.");
        } else {
            sb.append("He procesado tu petición: \"").append(truncar(mensajeUsuario, 60)).append("\".");
        }

        if (publicacionesCreadas.isEmpty()) {
            sb.append("\n\nNo se han generado publicaciones en este turno. En producción, Claude decidiría cuándo crear contenido.");
        } else {
            sb.append("\n\nHe creado ").append(publicacionesCreadas.size())
                    .append(" publicación(es) de prueba en estado PENDIENTE. ")
                    .append("Puedes revisarlas y aprobarlas desde el panel de publicaciones.");
        }

        sb.append("\n\n_Esta respuesta es del mock de desarrollo. Activa el perfil prod para usar Claude real._");
        return sb.toString();
    }

    private String obtenerUltimoMensajeUsuario(List<MensajeConversacion> historial) {
        for (int i = historial.size() - 1; i >= 0; i--) {
            MensajeConversacion m = historial.get(i);
            if ("Usuario".equals(m.getRol().getNombre())) {
                return m.getContenido();
            }
        }
        return "";
    }

    @Override
    public String describirAdjunto(Adjunto adjunto) {
        log.debug("[MOCK] describirAdjunto para '{}' — devuelve descripción simulada", adjunto.getNombreFichero());
        return "[SIMULADO] Imagen: " + adjunto.getNombreFichero()
                + " | Tipo: " + adjunto.getTipoMime()
                + " | En producción Claude analizaría el contenido real.";
    }

    private String truncar(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}
