package com.esteban.comunitymanager.repository;

import com.esteban.comunitymanager.model.ImagenGenerada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImagenGeneradaRepository extends JpaRepository<ImagenGenerada, UUID> {

    List<ImagenGenerada> findByEventoId(UUID idEvento);

    Optional<ImagenGenerada> findByPublicacionId(UUID idPublicacion);
}
