package com.esteban.comunitymanager.claude;

import com.esteban.comunitymanager.model.Adjunto;
import com.esteban.comunitymanager.model.MensajeConversacion;

import java.util.List;
import java.util.UUID;

/**
 * Abstracción para la integración con la API de Anthropic.
 *
 * Fase 1 — perfil dev: implementado por ClaudeServiceMock (sin coste de créditos).
 * Fase 1 — perfil prod: pendiente de implementar en ClaudeServiceImpl.
 *
 * Claude recibe el historial completo de mensajes del evento en cada turno
 * y puede crear/actualizar publicaciones en BBDD mediante Tool Use.
 */
public interface ClaudeService {

    /**
     * Envía el historial completo de la conversación a Claude y devuelve su respuesta.
     *
     * @param systemPrompt   Prompt de sistema construido a partir de ConfiguracionCliente e InstruccionPlataforma
     * @param historial      Todos los mensajes del evento ordenados cronológicamente
     * @param eventoId       ID del evento activo (para contexto en las tools de Claude)
     * @return               Texto de respuesta + IDs de publicaciones creadas via Tool Use
     */
    ClaudeRespuesta enviarConversacion(String systemPrompt, List<MensajeConversacion> historial, UUID eventoId);

    /**
     * Hace una llamada minimalista a Anthropic para obtener una descripción textual
     * del adjunto (imagen o PDF). Sin historial, sin system prompt complejo.
     * Devuelve la descripción o cadena vacía si falla.
     */
    String describirAdjunto(Adjunto adjunto);
}
