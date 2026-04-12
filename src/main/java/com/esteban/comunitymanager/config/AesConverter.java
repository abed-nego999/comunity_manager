package com.esteban.comunitymanager.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Cifra y descifra los tokens de credenciales con AES-256-GCM.
 *
 * Formato almacenado en BBDD: base64(iv):base64(ciphertext)
 * El IV (96 bits) se genera aleatoriamente en cada cifrado y se embebe en el valor,
 * por lo que cada campo gestiona su propio IV de forma independiente.
 *
 * Clave maestra: variable de entorno AES_SECRET_KEY (mínimo 32 caracteres).
 * En desarrollo, si no está definida, se usa una clave local de solo desarrollo.
 */
@Component
@Converter
public class AesConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(AesConverter.class);
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    @Value("${app.aes-secret-key:dev-only-key-change-in-productio}")
    private String rawKey;

    private SecretKey buildKey() {
        byte[] keyBytes = Arrays.copyOf(rawKey.getBytes(StandardCharsets.UTF_8), 32);
        return new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            String ivB64 = Base64.getEncoder().encodeToString(iv);
            String ciphertextB64 = Base64.getEncoder().encodeToString(ciphertext);
            return ivB64 + ":" + ciphertextB64;
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar credencial", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            String[] parts = dbData.split(":", 2);
            if (parts.length != 2) {
                log.warn("Formato de token cifrado inválido — ignorando descifrado");
                return null;
            }
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, buildKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar credencial", e);
        }
    }
}
