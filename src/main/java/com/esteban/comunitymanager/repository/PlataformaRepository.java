package com.esteban.comunitymanager.repository;

import com.esteban.comunitymanager.model.Plataforma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlataformaRepository extends JpaRepository<Plataforma, UUID> {

    Optional<Plataforma> findByNombre(String nombre);
}
