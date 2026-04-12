package com.esteban.comunitymanager.repository;

import com.esteban.comunitymanager.model.ConfiguracionCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConfiguracionClienteRepository extends JpaRepository<ConfiguracionCliente, UUID> {

    Optional<ConfiguracionCliente> findByClienteId(UUID idCliente);
}
