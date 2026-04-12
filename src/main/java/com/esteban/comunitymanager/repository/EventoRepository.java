package com.esteban.comunitymanager.repository;

import com.esteban.comunitymanager.model.Evento;
import com.esteban.comunitymanager.model.EstadoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventoRepository extends JpaRepository<Evento, UUID> {

    List<Evento> findByClienteId(UUID idCliente);

    List<Evento> findByClienteIdAndEstado(UUID idCliente, EstadoEvento estado);
}
