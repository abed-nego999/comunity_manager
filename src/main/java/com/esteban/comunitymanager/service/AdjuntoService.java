package com.esteban.comunitymanager.service;

import com.esteban.comunitymanager.dto.response.AdjuntoResponse;
import com.esteban.comunitymanager.exception.ResourceNotFoundException;
import com.esteban.comunitymanager.model.Adjunto;
import com.esteban.comunitymanager.model.OrigenAdjunto;
import com.esteban.comunitymanager.model.Publicacion;
import com.esteban.comunitymanager.model.TipoAdjunto;
import com.esteban.comunitymanager.repository.AdjuntoRepository;
import com.esteban.comunitymanager.repository.PublicacionRepository;
import com.esteban.comunitymanager.repository.TipoAdjuntoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdjuntoService {

    private final AdjuntoRepository adjuntoRepository;
    private final PublicacionRepository publicacionRepository;
    private final TipoAdjuntoRepository tipoAdjuntoRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public List<AdjuntoResponse> listarAdjuntos(UUID idPublicacion) {
        return adjuntoRepository.findByPublicacionId(idPublicacion).stream()
                .map(AdjuntoResponse::from)
                .toList();
    }

    /**
     * Guarda el fichero en disco y registra el adjunto en BBDD.
     * La ruta en disco sigue la estructura:
     * storage/clientes/{id}_{nombre}/eventos/{id}_{nombre}/adjuntos/
     */
    @Transactional
    public AdjuntoResponse subirAdjunto(UUID idPublicacion, MultipartFile fichero, UUID idTipoAdjunto) {
        Publicacion publicacion = publicacionRepository.findById(idPublicacion)
                .orElseThrow(() -> ResourceNotFoundException.of("Publicacion", idPublicacion));
        TipoAdjunto tipoAdjunto = tipoAdjuntoRepository.findById(idTipoAdjunto)
                .orElseThrow(() -> ResourceNotFoundException.of("TipoAdjunto", idTipoAdjunto));

        var evento = publicacion.getEvento();
        var cliente = evento.getCliente();
        String nombreFichero = fichero.getOriginalFilename() != null
                ? fichero.getOriginalFilename()
                : UUID.randomUUID().toString();

        Path destino = storageService.resolverRutaAdjunto(
                cliente.getId(), cliente.getNombre(),
                evento.getId(), evento.getNombre(),
                nombreFichero);

        String rutaRelativa;
        try {
            rutaRelativa = storageService.guardarFichero(fichero, destino);
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el fichero adjunto: " + e.getMessage(), e);
        }

        Adjunto adjunto = adjuntoRepository.save(Adjunto.builder()
                .publicacion(publicacion)
                .tipoAdjunto(tipoAdjunto)
                .rutaFichero(rutaRelativa)
                .origen(OrigenAdjunto.MANUAL)
                .build());

        return AdjuntoResponse.from(adjunto);
    }

    @Transactional
    public void eliminarAdjunto(UUID idPublicacion, UUID idAdjunto) {
        Adjunto adjunto = adjuntoRepository.findById(idAdjunto)
                .orElseThrow(() -> ResourceNotFoundException.of("Adjunto", idAdjunto));

        if (!adjunto.getPublicacion().getId().equals(idPublicacion)) {
            throw new ResourceNotFoundException(
                    "Adjunto " + idAdjunto + " no pertenece a la publicacion " + idPublicacion);
        }

        storageService.eliminarFichero(adjunto.getRutaFichero());
        adjuntoRepository.delete(adjunto);
    }
}
