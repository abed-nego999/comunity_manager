package com.esteban.comunitymanager.repository;

import com.esteban.comunitymanager.model.Credencial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CredencialRepository extends JpaRepository<Credencial, UUID> {

    List<Credencial> findByClienteId(UUID idCliente);

    Optional<Credencial> findByClienteIdAndPlataformaId(UUID idCliente, UUID idPlataforma);
}
