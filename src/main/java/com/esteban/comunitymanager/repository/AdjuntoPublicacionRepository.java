package com.esteban.comunitymanager.repository;

import com.esteban.comunitymanager.model.AdjuntoPublicacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdjuntoPublicacionRepository extends JpaRepository<AdjuntoPublicacion, UUID> {
    List<AdjuntoPublicacion> findByIdAdjunto(UUID idAdjunto);
    List<AdjuntoPublicacion> findByIdPublicacion(UUID idPublicacion);
    void deleteByIdAdjuntoAndIdPublicacion(UUID idAdjunto, UUID idPublicacion);
    boolean existsByIdAdjuntoAndIdPublicacion(UUID idAdjunto, UUID idPublicacion);
}
