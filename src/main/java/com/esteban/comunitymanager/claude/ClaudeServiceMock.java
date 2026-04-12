package com.esteban.comunitymanager.claude;

import com.esteban.comunitymanager.model.MensajeConversacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Mock de ClaudeService activo en el perfil 'dev'.
 * No realiza ninguna llamada a la API de Anthropic — devuelve una respuesta fija
 * para poder desarrollar y probar el flujo sin consumir créditos.
 *
 * La implementación real (ClaudeServiceImpl) se creará en la integración de Fase 1.
 */
@Service
@Profile("dev")
public class ClaudeServiceMock implements ClaudeService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeServiceMock.class);

    @Override
    public ClaudeRespuesta enviarConversacion(String systemPrompt, List<MensajeConversacion> historial, UUID eventoId) {
        log.debug("[MOCK] Claude recibe {} mensajes para el evento {}", historial.size(), eventoId);
        String respuesta = """
                [MOCK — perfil dev] He recibido tu mensaje. \
                En el perfil de producción, aquí respondería Claude con contenido real \
                generado a partir de tu petición y la configuración del cliente.""";
        return ClaudeRespuesta.builder()
                .textoRespuesta(respuesta)
                .publicacionesCreadas(List.of())
                .build();
    }
}
