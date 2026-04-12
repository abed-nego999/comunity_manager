package com.esteban.comunitymanager.repository;

import com.esteban.comunitymanager.model.Adjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdjuntoRepository extends JpaRepository<Adjunto, UUID> {

    List<Adjunto> findByPublicacionId(UUID idPublicacion);
}
