package com.esteban.comunitymanager.repository;

import com.esteban.comunitymanager.model.RolConversacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RolConversacionRepository extends JpaRepository<RolConversacion, UUID> {

    Optional<RolConversacion> findByNombre(String nombre);
}
