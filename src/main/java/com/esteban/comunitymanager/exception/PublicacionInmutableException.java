package com.esteban.comunitymanager.exception;

import java.util.UUID;

/**
 * Se lanza cuando se intenta modificar una publicación en estado ENVIADA.
 * El GlobalExceptionHandler la mapea a HTTP 409 Conflict.
 */
public class PublicacionInmutableException extends RuntimeException {

    public PublicacionInmutableException(UUID publicacionId) {
        super("La publicación " + publicacionId + " está en estado ENVIADA y no puede modificarse.");
    }
}
