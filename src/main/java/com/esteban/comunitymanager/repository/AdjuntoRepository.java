package com.esteban.comunitymanager.repository;

import com.esteban.comunitymanager.model.Adjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdjuntoRepository extends JpaRepository<Adjunto, UUID> {

    List<Adjunto> findByEventoId(UUID idEvento);

    Optional<Adjunto> findByEventoIdAndRutaFichero(UUID eventoId, String rutaFichero);

    List<Adjunto> findByEventoIdAndDescripcionIaIsNull(UUID eventoId);

}
