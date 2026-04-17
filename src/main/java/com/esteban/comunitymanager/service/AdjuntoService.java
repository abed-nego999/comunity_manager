package com.esteban.comunitymanager.service;

import com.esteban.comunitymanager.claude.ClaudeService;
import com.esteban.comunitymanager.dto.response.AdjuntoResponse;
import com.esteban.comunitymanager.exception.ResourceNotFoundException;
import com.esteban.comunitymanager.model.*;
import com.esteban.comunitymanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdjuntoService {

    private static final Logger log = LoggerFactory.getLogger(AdjuntoService.class);

    private static final Set<String> MIME_CHAT = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf"
    );

    private static final Set<String> MIME_PUBLICACION = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "video/mp4"
    );

    private static final Set<String> MIME_DESCRIBIBLES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf"
    );

    private final AdjuntoRepository adjuntoRepository;
    private final AdjuntoPublicacionRepository adjuntoPublicacionRepository;
    private final AdjuntoMensajeRepository adjuntoMensajeRepository;
    private final EventoRepository eventoRepository;
    private final MensajeConversacionRepository mensajeRepository;
    private final PublicacionRepository publicacionRepository;
    private final StorageService storageService;

    // Lazy para romper la dependencia circular AdjuntoService ↔ ClaudeServiceImpl
    @Lazy
    @Autowired
    private ClaudeService claudeService;

    // ── Procesamiento previo de imágenes ─────────────────────────────────────

    /**
     * Si el adjunto no tiene descripcionIa y su tipo es imagen o PDF soportado,
     * llama a Claude para obtener una descripción y la persiste.
     * Si el fichero no es describible o ya tiene descripción, no hace nada.
     * Errores de la API de Claude se capturan: el flujo no se interrumpe.
     */
    public void procesarDescripcionSiNecesario(Adjunto adjunto) {
        if (adjunto.getDescripcionIa() != null && !adjunto.getDescripcionIa().isBlank()) return;
        if (!MIME_DESCRIBIBLES.contains(adjunto.getTipoMime())) return;

        try {
            String descripcion = claudeService.describirAdjunto(adjunto);
            if (descripcion != null && !descripcion.isBlank()) {
                adjunto.setDescripcionIa(descripcion);
                adjuntoRepository.save(adjunto);
                log.info("[Adjunto] Descripción generada para {}", adjunto.getNombreFichero());
            }
        } catch (Exception e) {
            log.warn("[Adjunto] No se pudo generar descripción para '{}': {}",
                    adjunto.getNombreFichero(), e.getMessage());
        }
    }

    // ── Subida desde el chat ──────────────────────────────────────────────────

    /**
     * Sube un fichero desde el chat.
     * Si ya existe un adjunto con la misma ruta en este evento, lo reutiliza.
     * Llama a procesarDescripcionSiNecesario antes de devolver la respuesta.
     */
    @Transactional
    public AdjuntoResponse subirDesdeChat(MultipartFile fichero, UUID eventoId, UUID mensajeId) {
        Evento evento = buscarEvento(eventoId);

        if (mensajeId != null) {
            mensajeRepository.findById(mensajeId)
                    .orElseThrow(() -> ResourceNotFoundException.of("MensajeConversacion", mensajeId));
        }

        String tipoMime = fichero.getContentType();
        if (tipoMime == null || !MIME_CHAT.contains(tipoMime)) {
            throw new IllegalArgumentException(
                    "Tipo de fichero no soportado: " + tipoMime
                    + ". Formatos permitidos: image/jpeg, image/png, image/gif, image/webp, application/pdf");
        }

        String nombreFichero = resolverNombreFichero(fichero, tipoMime);
        Path destino = storageService.resolverRutaAdjunto(
                evento.getCliente().getId(), evento.getCliente().getNombre(),
                evento.getId(), evento.getNombre(), nombreFichero);

        // Guardar en disco (sobreescribe si ya existía)
        String rutaRelativa = guardar(fichero, destino);

        // Deduplicación: reutilizar registro si ya existe el mismo fichero en este evento
        Optional<Adjunto> existente = adjuntoRepository.findByEventoIdAndRutaFichero(eventoId, rutaRelativa);
        Adjunto adjunto;
        if (existente.isPresent()) {
            adjunto = existente.get();
            log.debug("[Adjunto] Reutilizando adjunto existente '{}' ({})", nombreFichero, adjunto.getId());
        } else {
            adjunto = adjuntoRepository.save(Adjunto.builder()
                    .evento(evento)
                    .nombreFichero(nombreFichero)
                    .rutaFichero(rutaRelativa)
                    .tipoMime(tipoMime)
                    .origen(OrigenAdjunto.MANUAL)
                    .build());
        }

        // Crear ADJUNTO_MENSAJE si se proporcionó mensajeId y no existe ya
        if (mensajeId != null) {
            final UUID adjuntoId = adjunto.getId();
            boolean yaAsociado = adjuntoMensajeRepository.findByIdAdjunto(adjuntoId)
                    .stream().anyMatch(am -> am.getIdMensaje().equals(mensajeId));
            if (!yaAsociado) {
                adjuntoMensajeRepository.save(AdjuntoMensaje.builder()
                        .idAdjunto(adjuntoId)
                        .idMensaje(mensajeId)
                        .build());
            }
        }

        // Generar descripción IA si no la tiene (llamada síncrona — el frontend espera)
        procesarDescripcionSiNecesario(adjunto);

        return AdjuntoResponse.from(adjunto);
    }

    // ── Subida adjunto de publicación ─────────────────────────────────────────

    @Transactional
    public AdjuntoResponse subirAdjuntoPublicacion(MultipartFile fichero, UUID eventoId, UUID publicacionId) {
        Evento evento = buscarEvento(eventoId);
        publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Publicacion", publicacionId));

        String tipoMime = fichero.getContentType();
        if (tipoMime == null || !MIME_PUBLICACION.contains(tipoMime)) {
            throw new IllegalArgumentException(
                    "Tipo de fichero no soportado: " + tipoMime
                    + ". Formatos permitidos: image/jpeg, image/png, image/gif, image/webp, video/mp4");
        }

        String nombreFichero = resolverNombreFichero(fichero, tipoMime);
        Path destino = storageService.resolverRutaAdjunto(
                evento.getCliente().getId(), evento.getCliente().getNombre(),
                evento.getId(), evento.getNombre(), nombreFichero);

        String rutaRelativa = guardar(fichero, destino);

        Adjunto adjunto = adjuntoRepository.save(Adjunto.builder()
                .evento(evento)
                .nombreFichero(nombreFichero)
                .rutaFichero(rutaRelativa)
                .tipoMime(tipoMime)
                .origen(OrigenAdjunto.MANUAL)
                .build());

        adjuntoPublicacionRepository.save(AdjuntoPublicacion.builder()
                .idAdjunto(adjunto.getId())
                .idPublicacion(publicacionId)
                .build());

        return AdjuntoResponse.from(adjunto);
    }

    // ── Asociar a publicación ─────────────────────────────────────────────────

    @Transactional
    public AdjuntoResponse asociarAPublicacion(UUID adjuntoId, UUID publicacionId) {
        Adjunto adjunto = adjuntoRepository.findById(adjuntoId)
                .orElseThrow(() -> ResourceNotFoundException.of("Adjunto", adjuntoId));
        publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Publicacion", publicacionId));

        if (!adjuntoPublicacionRepository.existsByIdAdjuntoAndIdPublicacion(adjuntoId, publicacionId)) {
            adjuntoPublicacionRepository.save(AdjuntoPublicacion.builder()
                    .idAdjunto(adjuntoId)
                    .idPublicacion(publicacionId)
                    .build());
        }

        return AdjuntoResponse.from(adjunto);
    }

    // ── Descripción generada por IA ───────────────────────────────────────────

    @Transactional
    public AdjuntoResponse actualizarDescripcionIa(UUID adjuntoId, String descripcion) {
        Adjunto adjunto = adjuntoRepository.findById(adjuntoId)
                .orElseThrow(() -> ResourceNotFoundException.of("Adjunto", adjuntoId));
        adjunto.setDescripcionIa(descripcion);
        return AdjuntoResponse.from(adjuntoRepository.save(adjunto));
    }

    // ── Registrar imagen generada ─────────────────────────────────────────────

    @Transactional
    public AdjuntoResponse registrarGenerado(String rutaFichero, UUID eventoId, UUID publicacionId,
                                              String promptUsado, String motor) {
        Evento evento = buscarEvento(eventoId);
        publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Publicacion", publicacionId));

        String nombreFichero = rutaFichero.contains("/")
                ? rutaFichero.substring(rutaFichero.lastIndexOf('/') + 1)
                : rutaFichero;

        Adjunto adjunto = adjuntoRepository.save(Adjunto.builder()
                .evento(evento)
                .nombreFichero(nombreFichero)
                .rutaFichero(rutaFichero)
                .tipoMime("image/png")
                .origen(OrigenAdjunto.GENERADO)
                .promptUsado(promptUsado)
                .motor(motor)
                .build());

        adjuntoPublicacionRepository.save(AdjuntoPublicacion.builder()
                .idAdjunto(adjunto.getId())
                .idPublicacion(publicacionId)
                .build());

        return AdjuntoResponse.from(adjunto);
    }

    // ── Eliminar ─────────────────────────────────────────────────────────────

    @Transactional
    public void eliminar(UUID adjuntoId, String contexto) {
        Adjunto adjunto = adjuntoRepository.findById(adjuntoId)
                .orElseThrow(() -> ResourceNotFoundException.of("Adjunto", adjuntoId));

        boolean borrarFisico;

        if ("publicacion".equals(contexto)) {
            adjuntoPublicacionRepository.findByIdAdjunto(adjuntoId)
                    .forEach(ap -> adjuntoPublicacionRepository.deleteByIdAdjuntoAndIdPublicacion(
                            ap.getIdAdjunto(), ap.getIdPublicacion()));
            borrarFisico = !adjuntoMensajeRepository.existsByIdAdjunto(adjuntoId);
        } else if ("mensaje".equals(contexto)) {
            adjuntoMensajeRepository.findByIdAdjunto(adjuntoId)
                    .forEach(am -> adjuntoMensajeRepository.deleteByIdAdjuntoAndIdMensaje(
                            am.getIdAdjunto(), am.getIdMensaje()));
            borrarFisico = adjuntoPublicacionRepository.findByIdAdjunto(adjuntoId).isEmpty();
        } else {
            adjuntoPublicacionRepository.findByIdAdjunto(adjuntoId)
                    .forEach(ap -> adjuntoPublicacionRepository.deleteByIdAdjuntoAndIdPublicacion(
                            ap.getIdAdjunto(), ap.getIdPublicacion()));
            adjuntoMensajeRepository.findByIdAdjunto(adjuntoId)
                    .forEach(am -> adjuntoMensajeRepository.deleteByIdAdjuntoAndIdMensaje(
                            am.getIdAdjunto(), am.getIdMensaje()));
            borrarFisico = true;
        }

        if (borrarFisico) {
            storageService.eliminarFichero(adjunto.getRutaFichero());
            adjuntoRepository.delete(adjunto);
        }
    }

    // ── Listados ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AdjuntoResponse> listarPorEvento(UUID eventoId) {
        buscarEvento(eventoId);
        return adjuntoRepository.findByEventoId(eventoId).stream()
                .map(AdjuntoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AdjuntoResponse> listarPorPublicacion(UUID publicacionId) {
        return adjuntoPublicacionRepository.findByIdPublicacion(publicacionId).stream()
                .map(ap -> adjuntoRepository.findById(ap.getIdAdjunto())
                        .map(AdjuntoResponse::from)
                        .orElseThrow(() -> ResourceNotFoundException.of("Adjunto", ap.getIdAdjunto())))
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Evento buscarEvento(UUID id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Evento", id));
    }

    private String resolverNombreFichero(MultipartFile fichero, String tipoMime) {
        if (fichero.getOriginalFilename() != null && !fichero.getOriginalFilename().isBlank()) {
            return fichero.getOriginalFilename();
        }
        return UUID.randomUUID() + extraerExtension(tipoMime);
    }

    private String extraerExtension(String tipoMime) {
        return switch (tipoMime) {
            case "image/jpeg"      -> ".jpg";
            case "image/png"       -> ".png";
            case "image/gif"       -> ".gif";
            case "image/webp"      -> ".webp";
            case "application/pdf" -> ".pdf";
            case "video/mp4"       -> ".mp4";
            default -> "";
        };
    }

    private String guardar(MultipartFile fichero, Path destino) {
        try {
            return storageService.guardarFichero(fichero, destino);
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el fichero: " + e.getMessage(), e);
        }
    }
}
