package com.esteban.comunitymanager.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "rol_conversacion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolConversacion {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;
}
