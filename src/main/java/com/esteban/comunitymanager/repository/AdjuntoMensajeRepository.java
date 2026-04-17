package com.esteban.comunitymanager.repository;

import com.esteban.comunitymanager.model.AdjuntoMensaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdjuntoMensajeRepository extends JpaRepository<AdjuntoMensaje, UUID> {
    List<AdjuntoMensaje> findByIdAdjunto(UUID idAdjunto);
    List<AdjuntoMensaje> findByIdMensaje(UUID idMensaje);
    void deleteByIdAdjuntoAndIdMensaje(UUID idAdjunto, UUID idMensaje);
    boolean existsByIdAdjunto(UUID idAdjunto);
}
