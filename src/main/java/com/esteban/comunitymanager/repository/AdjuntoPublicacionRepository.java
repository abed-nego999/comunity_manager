package com.esteban.comunitymanager.repository;

import com.esteban.comunitymanager.model.AdjuntoPublicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AdjuntoPublicacionRepository extends JpaRepository<AdjuntoPublicacion, UUID> {
    List<AdjuntoPublicacion> findByIdAdjunto(UUID idAdjunto);
    List<AdjuntoPublicacion> findByIdPublicacion(UUID idPublicacion);
    List<AdjuntoPublicacion> findByIdPublicacionOrderByOrdenAsc(UUID idPublicacion);
    void deleteByIdAdjuntoAndIdPublicacion(UUID idAdjunto, UUID idPublicacion);
    boolean existsByIdAdjuntoAndIdPublicacion(UUID idAdjunto, UUID idPublicacion);

    @Query("SELECT COALESCE(MAX(ap.orden), -1) FROM AdjuntoPublicacion ap WHERE ap.idPublicacion = :id")
    int findMaxOrdenByIdPublicacion(@Param("id") UUID id);
}
