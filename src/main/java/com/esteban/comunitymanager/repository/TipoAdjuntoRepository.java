package com.esteban.comunitymanager.repository;

import com.esteban.comunitymanager.model.TipoAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TipoAdjuntoRepository extends JpaRepository<TipoAdjunto, UUID> {

    Optional<TipoAdjunto> findByNombre(String nombre);
}
