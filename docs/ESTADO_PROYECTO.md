# Estado del Proyecto — Community Manager
**Versión:** 2.0  
**Fecha:** Abril 2026  
**Estado:** En desarrollo activo

---

## Descripción general

Aplicación web local (`localhost:8080`) en Java 25 + Spring Boot 3.4.x para gestionar y publicar contenido en redes sociales para múltiples clientes. El usuario es el único operador. Claude genera contenido mediante Tool Use y el usuario supervisa y aprueba antes de publicar.

Primer cliente: **ABS El Pisotón** — Asociación de Bailes de Salón de Alcobendas y San Sebastián de los Reyes.

---

## Estado por fases

| Fase | Descripción | Estado |
|---|---|---|
| 0 | Entorno de desarrollo | ✅ Completada |
| 1 | Spring Boot base + API REST + Tool Use + UI | ✅ Completada |
| 2 | Meta Graph API (Facebook + Instagram) | ⏳ Pendiente |
| 3 | YouTube Data API v3 | ⏳ Pendiente |
| 4 | Ideogram 3 API | ⏳ Pendiente |

---

## Lo que funciona actualmente

### Backend
- API REST completa según `openapi.yaml`
- Entidades JPA y repositorios para todas las tablas del E-R v5.5
- Seed automático de tablas auxiliares (PLATAFORMA, TIPO_PUBLICACION, ROL_CONVERSACION)
- Datos de El Pisotón insertados en BBDD (cliente, configuración, instrucciones por plataforma)
- Integración con Anthropic API (Claude Sonnet 4.6) — real en todos los perfiles
- Tool Use: Claude puede llamar a `crear_publicacion`, `actualizar_publicacion`, `crear_evento`, `asociar_adjunto_publicacion`, `listar_adjuntos_evento`, `listar_adjuntos_publicacion`, `guardar_descripcion_adjunto`
- System prompt construido en tres capas: base fija (`system-prompt-base.txt`) + configuración cliente (BBDD) + datos evento activo
- System prompt dinámico incluye: IDs de cliente y evento, tipos de publicación con UUIDs, publicaciones existentes del evento (últimas 10 aprobadas), adjuntos disponibles con descripciones
- Procesamiento previo de imágenes: antes de enviar el mensaje a Claude, cada imagen se analiza en una llamada independiente y se guarda `descripcion_ia` en BBDD
- El historial de conversación nunca contiene base64 — solo descripciones de texto
- Backup automático de configuración en `storage/clientes/{id}/config-backup.json`
- Endpoint para servir ficheros: `GET /api/v1/ficheros?ruta={ruta_relativa}`
- Eliminación segura de adjuntos: desasocia sin borrar si hay otras referencias activas

### Modelo de adjuntos (E-R v5.5)
- `ADJUNTO` — entidad central sin FKs directas a publicación ni mensaje
- `ADJUNTO_PUBLICACION` — tabla de asociación N-N adjunto ↔ publicación
- `ADJUNTO_MENSAJE` — tabla de asociación N-N adjunto ↔ mensaje
- `ADJUNTO.id_evento` siempre presente — permite cargar todos los adjuntos del evento para Claude
- `ADJUNTO.descripcion_ia` — descripción generada por Claude al analizar el fichero por primera vez

### Frontend
- Vista unificada — chat y eventos en el mismo documento HTML, sin recargas de página
- Navegación entre paneles solo cambia clases CSS, el DOM nunca se destruye
- Estado del chat preservado al navegar entre paneles
- Indicador "Claude está pensando..." visible aunque el usuario navegue a eventos
- Polling cada 4 segundos en el panel de eventos para detectar nuevas publicaciones
- Indicador visual "En vivo" en el panel de eventos
- Barra lateral expandible con selector de cliente
- Cabeceras de color por plataforma en publicaciones
- Tres fechas en cabecera de publicación: generación, envío y publicación
- Botones de acción según estado: Aprobar, Rechazar, Pedir cambios, Publicar
- Flujo de feedback: redirige al chat con el mensaje pre-enviado
- Aviso de publicaciones creadas con enlace al panel de eventos
- Thumbnails clicables de imágenes en mensajes del chat y en publicaciones
- Subida de múltiples ficheros a la vez con el botón 📎
- Flujo secuencial garantizado: imágenes subidas y procesadas ANTES de enviar mensaje a Claude
- Animación de carga al adjuntar imágenes
- Persistencia de evento y cliente activo en sessionStorage
- Eventos creados como ACTIVO por defecto
- Publicaciones ENVIADAS protegidas contra modificación (409 Conflict)

---

## Bugs conocidos / trabajo en curso

- Tarea pendiente de perrito: unificación de vistas en una sola página (prompt ya generado)

---

## Decisiones técnicas importantes tomadas

### Stack y configuración
- Java 25 LTS con Spring Boot 3.4.1
- Workaround ASM: `mainClass` especificado explícitamente en `spring-boot-maven-plugin`
- Lombok 1.18.38 — necesario para compatibilidad con Java 25
- H2 en modo fichero — `ddl-auto=update`
- Claude real activo en todos los perfiles (dev y prod)

### Adjuntos
- Modelo N-N con tablas de asociación — un adjunto puede estar en varias publicaciones y varios mensajes simultáneamente
- Procesamiento previo obligatorio: toda imagen/PDF se describe con Claude antes de enviarse al chat
- El base64 nunca viaja en el historial de conversación — solo `descripcion_ia` en texto
- Eliminación segura: se borra el fichero físico solo cuando no quedan referencias en ninguna tabla de asociación

### System prompt
- Parte fija: `src/main/resources/prompts/system-prompt-base.txt`
- Parte dinámica construida en `ClaudeServiceImpl` leyendo de BBDD
- Sin Markdown en textos para redes sociales (excepto YouTube)
- Claude nunca pide UUIDs al usuario — los tiene siempre en el system prompt

### Publicaciones
- Estado: `PENDIENTE → APROBADA → ENVIADA` o `RECHAZADA`
- Sin estado PROGRAMADA — la programación la gestiona Meta/YouTube
- Tres fechas: `fecha_generacion`, `fecha_envio`, `fecha_publicacion`
- Las publicaciones ENVIADAS son inmutables

### Almacenamiento
```
storage/
└── clientes/{id}_{nombre}/
    ├── config-backup.json
    └── eventos/{id}_{nombre}/
        ├── adjuntos/    ← ficheros subidos manualmente
        └── generados/  ← imágenes de Ideogram (futuro)
```

### Seguridad
- Credenciales Meta/YouTube cifradas con AES-256-GCM en H2
- `token_iv` en BBDD, clave maestra en `.env`
- `.env` contiene: `AES_SECRET_KEY`, `ANTHROPIC_API_KEY`

---

## Estructura de paquetes

```
src/main/java/com/esteban/comunitymanager/
├── controller/       ← AuxiliarController, ClienteController,
│                       ConfiguracionController, CredencialController,
│                       EventoController, PublicacionController,
│                       AdjuntoController
├── service/          ← ClienteService, EventoService, PublicacionService,
│                       AdjuntoService, StorageService, CredencialService,
│                       ConfiguracionService, AuxiliarService
├── repository/       ← Interfaces JPA
├── model/            ← Entidades JPA
├── dto/              ← Request y Response objects
├── config/           ← AesConverter, GlobalExceptionHandler
└── claude/           ← ClaudeServiceImpl, ClaudeRespuesta

src/main/resources/
├── application.properties
├── application-dev.properties
├── application-prod.properties
├── data.sql                          ← seed idempotente
├── prompts/
│   └── system-prompt-base.txt        ← parte fija del system prompt
└── templates/
    └── fragments/layout.html         ← vista unificada (en progreso)
```

---

## Próximos pasos

1. ✅ Perrito termina la unificación de vistas (prompt ya lanzado)
2. Fase 2: integración Meta Graph API (Facebook + Instagram)
3. Fase 3: integración YouTube Data API v3
4. Fase 4: integración Ideogram 3 API
5. Futuro: empaquetado como instalador Windows (.exe) para distribución open source

---

## Configuración de entorno

```properties
# application.properties
spring.datasource.url=jdbc:h2:file:./data/social-manager
spring.jpa.hibernate.ddl-auto=update
storage.base-path=./storage

# .env (excluido de git)
AES_SECRET_KEY=...
ANTHROPIC_API_KEY=sk-ant-...
```

```
# .gitignore incluye:
data/
storage/
.env
*.mv.db
*.trace.db
target/
.idea/
```

---

*Documento actualizado el 17 de abril de 2026. Actualizar con cada sesión de desarrollo.*