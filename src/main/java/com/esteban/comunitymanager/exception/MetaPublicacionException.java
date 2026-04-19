package com.esteban.comunitymanager.exception;

public class MetaPublicacionException extends RuntimeException {

    /** true → error por datos del usuario (400); false → error de integración o servidor (500). */
    private final boolean esErrorDatos;

    public MetaPublicacionException(String message, boolean esErrorDatos) {
        super(message);
        this.esErrorDatos = esErrorDatos;
    }

    public boolean isErrorDatos() {
        return esErrorDatos;
    }
}
