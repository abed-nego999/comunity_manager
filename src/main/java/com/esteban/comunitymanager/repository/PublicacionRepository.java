package com.esteban.comunitymanager.repository;

import com.esteban.comunitymanager.model.EstadoPublicacion;
import com.esteban.comunitymanager.model.Publicacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PublicacionRepository extends JpaRepository<Publicacion, UUID> {

    List<Publicacion> findByEventoId(UUID idEvento);

    List<Publicacion> findByEventoIdAndEstado(UUID idEvento, EstadoPublicacion estado);
}
