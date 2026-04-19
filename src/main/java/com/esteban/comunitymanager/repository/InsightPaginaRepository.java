package com.esteban.comunitymanager.repository;

import com.esteban.comunitymanager.model.InsightPagina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InsightPaginaRepository extends JpaRepository<InsightPagina, UUID> {

    Optional<InsightPagina> findByClienteIdAndPlataformaId(UUID clienteId, UUID plataformaId);
}
