package com.esteban.comunitymanager.repository;

import com.esteban.comunitymanager.model.MensajeConversacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MensajeConversacionRepository extends JpaRepository<MensajeConversacion, UUID> {

    /** Devuelve el historial ordenado cronológicamente para enviarlo completo a Claude. */
    List<MensajeConversacion> findByEventoIdOrderByEnviadoEnAsc(UUID idEvento);
}
