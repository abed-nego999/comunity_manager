package com.esteban.comunitymanager.service;

import com.esteban.comunitymanager.dto.request.FeedbackRequest;
import com.esteban.comunitymanager.dto.request.PublicacionRequest;
import com.esteban.comunitymanager.dto.request.PublicarRequest;
import com.esteban.comunitymanager.dto.response.PublicacionResponse;
import com.esteban.comunitymanager.exception.ResourceNotFoundException;
import com.esteban.comunitymanager.model.*;
import com.esteban.comunitymanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final EventoRepository eventoRepository;
    private final TipoPublicacionRepository tipoPublicacionRepository;

    @Transactional(readOnly = true)
    public List<PublicacionResponse> listarPublicaciones(UUID eventoId, EstadoPublicacion estado) {
        List<Publicacion> publicaciones = (estado != null)
                ? publicacionRepository.findByEventoIdAndEstado(eventoId, estado)
                : publicacionRepository.findByEventoId(eventoId);
        return publicaciones.stream()
                .map(PublicacionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicacionResponse obtenerPublicacion(UUID id) {
        return PublicacionResponse.from(buscarPublicacion(id));
    }

    /** Usado por Claude via Tool Use para crear publicaciones en estado PENDIENTE. */
    @Transactional
    public PublicacionResponse crearPublicacion(PublicacionRequest request) {
        Evento evento = eventoRepository.findById(request.getEventoId())
                .orElseThrow(() -> ResourceNotFoundException.of("Evento", request.getEventoId()));
        TipoPublicacion tipo = tipoPublicacionRepository.findById(request.getIdTipoPublicacion())
                .orElseThrow(() -> ResourceNotFoundException.of("TipoPublicacion", request.getIdTipoPublicacion()));

        Publicacion publicacion = publicacionRepository.save(Publicacion.builder()
                .evento(evento)
                .tipoPublicacion(tipo)
                .textoGenerado(request.getTextoGenerado())
                .estado(EstadoPublicacion.PENDIENTE)
                .build());

        return PublicacionResponse.from(publicacion);
    }

    /** Usado por Claude via Tool Use para actualizar contenido tras feedback del usuario. */
    @Transactional
    public PublicacionResponse actualizarPublicacion(UUID id, PublicacionRequest request) {
        Publicacion publicacion = buscarPublicacion(id);
        publicacion.setTextoGenerado(request.getTextoGenerado());
        return PublicacionResponse.from(publicacionRepository.save(publicacion));
    }

    // ── Transiciones de estado (exclusivas del usuario) ───────────────────────

    @Transactional
    public PublicacionResponse aprobarPublicacion(UUID id) {
        Publicacion publicacion = buscarPublicacion(id);
        verificarEstado(publicacion, EstadoPublicacion.PENDIENTE, "aprobar");
        publicacion.setEstado(EstadoPublicacion.APROBADA);
        return PublicacionResponse.from(publicacionRepository.save(publicacion));
    }

    @Transactional
    public PublicacionResponse rechazarPublicacion(UUID id) {
        Publicacion publicacion = buscarPublicacion(id);
        verificarEstado(publicacion, EstadoPublicacion.PENDIENTE, "rechazar");
        publicacion.setEstado(EstadoPublicacion.RECHAZADA);
        return PublicacionResponse.from(publicacionRepository.save(publicacion));
    }

    @Transactional
    public PublicacionResponse solicitarCambios(UUID id, FeedbackRequest request) {
        Publicacion publicacion = buscarPublicacion(id);
        publicacion.setFeedbackUsuario(request.getFeedback());
        publicacion.setEstado(EstadoPublicacion.PENDIENTE);
        return PublicacionResponse.from(publicacionRepository.save(publicacion));
    }

    /**
     * Publica la publicación en la plataforma externa.
     *
     * Fase 1: simula el envío (establece fechas y estado ENVIADA sin llamada real a la API).
     * Fase 2/3: se implementará la integración real con Meta Graph API y YouTube Data API.
     *
     * Si publicacionAutomatica=false (Blog Web), lanza excepción — el usuario publica manualmente.
     */
    @Transactional
    public PublicacionResponse publicar(UUID id, PublicarRequest request) {
        Publicacion publicacion = buscarPublicacion(id);
        verificarEstado(publicacion, EstadoPublicacion.APROBADA, "publicar");

        TipoPublicacion tipo = publicacion.getTipoPublicacion();
        if (!tipo.isPublicacionAutomatica()) {
            throw new IllegalStateException(
                    "El tipo '" + tipo.getNombre() + "' en " + tipo.getPlataforma().getNombre()
                    + " no admite publicación automática — publícalo manualmente.");
        }

        Instant ahora = Instant.now();
        publicacion.setFechaEnvio(ahora);

        // Si el tipo soporta programación externa y se pasó una fecha futura, se usa esa fecha
        if (tipo.isProgramacionExterna() && request != null && request.getFechaPublicacion() != null) {
            publicacion.setFechaPublicacion(request.getFechaPublicacion());
        } else {
            publicacion.setFechaPublicacion(ahora);
        }

        // TODO Fase 2/3: llamar a Meta Graph API o YouTube Data API aquí
        // publicacion.setIdExterno(resultadoApiExterna.getId());

        publicacion.setEstado(EstadoPublicacion.ENVIADA);
        return PublicacionResponse.from(publicacionRepository.save(publicacion));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    Publicacion buscarPublicacion(UUID id) {
        return publicacionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Publicacion", id));
    }

    private void verificarEstado(Publicacion publicacion, EstadoPublicacion estadoRequerido, String operacion) {
        if (publicacion.getEstado() != estadoRequerido) {
            throw new IllegalStateException(
                    "No se puede " + operacion + " una publicación en estado " + publicacion.getEstado()
                    + ". Estado requerido: " + estadoRequerido);
        }
    }
}
