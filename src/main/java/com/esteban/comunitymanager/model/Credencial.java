package com.esteban.comunitymanager.model;

import com.esteban.comunitymanager.config.AesConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credencial")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Credencial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plataforma", nullable = false)
    private Plataforma plataforma;

    /**
     * Token de acceso cifrado con AES-GCM.
     * El IV se embede en el valor: base64(iv):base64(ciphertext).
     * Nunca se expone en claro desde la API REST.
     */
    @Convert(converter = AesConverter.class)
    @Column(name = "access_token_cifrado", columnDefinition = "TEXT")
    private String accessTokenCifrado;

    /**
     * Token de refresco cifrado con AES-GCM (mismo esquema que accessToken).
     * Nunca se expone en claro desde la API REST.
     */
    @Convert(converter = AesConverter.class)
    @Column(name = "refresh_token_cifrado", columnDefinition = "TEXT")
    private String refreshTokenCifrado;

    /**
     * IV de referencia de nivel de registro.
     * Los tokens individuales incluyen su propio IV embebido.
     * Este campo puede usarse para auditoría o esquema de IV compartido en el futuro.
     */
    @Column(name = "token_iv")
    private String tokenIv;

    @Column(name = "expira_en")
    private Instant expiraEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;
}
