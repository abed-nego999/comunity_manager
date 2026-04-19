package com.esteban.comunitymanager.dto.response;

import java.time.Instant;

public record ResultadoPublicacion(String idExterno, Instant fechaEnvio, Instant fechaPublicacion) {}
