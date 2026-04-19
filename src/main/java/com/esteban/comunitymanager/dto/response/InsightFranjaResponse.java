package com.esteban.comunitymanager.dto.response;

/**
 * Franja horaria óptima para publicar, calculada a partir de los insights de Meta.
 * diaSemana: nombre del enum DayOfWeek en inglés (ej: "TUESDAY").
 * hora: hora del día en formato 0-23 (ej: 20 = 20:00).
 */
public record InsightFranjaResponse(String diaSemana, int hora) {}
