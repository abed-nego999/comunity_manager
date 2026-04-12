package com.esteban.comunitymanager.repository;

import com.esteban.comunitymanager.model.InstruccionPlataforma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstruccionPlataformaRepository extends JpaRepository<InstruccionPlataforma, UUID> {

    List<InstruccionPlataforma> findByConfiguracionId(UUID idConfiguracion);

    Optional<InstruccionPlataforma> findByConfiguracionIdAndPlataformaId(UUID idConfiguracion, UUID idPlataforma);
}
