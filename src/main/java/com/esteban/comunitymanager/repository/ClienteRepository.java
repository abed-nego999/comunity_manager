package com.esteban.comunitymanager.repository;

import com.esteban.comunitymanager.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    List<Cliente> findByActivoTrue();
}
