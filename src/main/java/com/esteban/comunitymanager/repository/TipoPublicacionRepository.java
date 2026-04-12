package com.esteban.comunitymanager.repository;

import com.esteban.comunitymanager.model.Plataforma;
import com.esteban.comunitymanager.model.TipoPublicacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TipoPublicacionRepository extends JpaRepository<TipoPublicacion, UUID> {

    List<TipoPublicacion> findByPlataforma(Plataforma plataforma);

    List<TipoPublicacion> findByPlataformaId(UUID idPlataforma);
}
