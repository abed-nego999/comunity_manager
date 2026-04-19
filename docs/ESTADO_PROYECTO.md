# Estado del Proyecto — Community Manager
**Versión:** 3.0  
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
| 2 | Meta Graph API (Facebook + Instagram) | 🔜 Siguiente |
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
- System prompt en tres capas: base fija (`system-prompt-base.txt`) + configuración cliente (BBDD) + datos evento activo
- System prompt dinámico incluye: IDs de cliente y evento, tipos de publicación con UUIDs, publicaciones existentes (últimas 10 aprobadas), adjuntos disponibles con descripciones truncadas a 300 chars
- Procesamiento previo de imágenes: cada imagen se describe con Claude (max 200 tokens, ~3-5 líneas) antes de enviarse al chat
- El historial de conversación nunca contiene base64 — solo descripciones de texto
- Retry automático en error 429: espera 60s y reintenta una vez
- Backup automático de configuración en `storage/clientes/{id}/config-backup.json`
- Endpoint para servir ficheros: `GET /api/v1/ficheros?ruta={ruta_relativa}`
- Eliminación segura de adjuntos vía tablas de asociación N-N

### Modelo de adjuntos (E-R v5.5)
- `ADJUNTO` — entidad central sin FKs directas a publicación ni mensaje
- `ADJUNTO_PUBLICACION` — tabla de asociación N-N adjunto ↔ publicación
- `ADJUNTO_MENSAJE` — tabla de asociación N-N adjunto ↔ mensaje
- `ADJUNTO.id_evento` siempre presente
- `ADJUNTO.descripcion_ia` — descripción corta generada por Claude al analizar el fichero

### Frontend
- Vista unificada — chat y eventos en el mismo HTML, sin recargas de página
- Navegación entre paneles solo cambia clases CSS, el DOM nunca se destruye
- Estado del chat preservado al navegar entre paneles
- Indicador "Claude está pensando..." persiste aunque el usuario navegue a eventos
- Polling cada 4 segundos en el panel de eventos
- Indicador visual "En vivo" en el panel de eventos
- Barra lateral expandible con selector de cliente
- Cabeceras de color por plataforma en publicaciones
- Tres fechas en cabecera de publicación: generación, envío y publicación
- Botones de acción según estado y tipo: Aprobar, Rechazar, Pedir cambios, Publicar
- Botón "Publicar" solo aparece si `publicacion_automatica=true`
- Flujo de feedback: redirige al chat con el mensaje pre-enviado
- Thumbnails clicables en mensajes del chat y en publicaciones (todos los estados)
- Referencias `[Adjunto subido: ...]` filtradas del texto visible al usuario
- Subida de múltiples ficheros con el botón 📎
- Flujo secuencial: imágenes subidas y descritas ANTES de enviar mensaje a Claude
- Animación de carga al adjuntar imágenes
- Persistencia de evento y cliente activo en sessionStorage
- Eventos creados como ACTIVO por defecto
- Publicaciones ENVIADAS inmutables (409 Conflict)
- Selector de eventos se actualiza dinámicamente al crear un evento nuevo

---

## Decisiones técnicas importantes tomadas

### Stack y configuración
- Java 25 LTS con Spring Boot 3.4.1
- Workaround ASM: `mainClass` explícito en `spring-boot-maven-plugin`
- Lombok 1.18.38 — necesario para Java 25
- H2 en modo fichero — `ddl-auto=update`
- Claude real activo en todos los perfiles

### Adjuntos
- Modelo N-N con tablas de asociación — un adjunto puede estar en varias publicaciones y mensajes
- Procesamiento previo: imagen → llamada independiente a Claude → descripción corta → guardada en BBDD
- Base64 nunca en el historial de conversación
- Descripciones truncadas a 300 chars en el system prompt para controlar tokens
- Eliminación segura: fichero físico se borra solo cuando no quedan referencias

### System prompt y tokens
- Descripciones de adjuntos limitadas a max_tokens=200 y prompt de 3-5 líneas
- Truncado a 300 chars al incluirlas en el system prompt
- Retry con backoff 60s en error 429 rate_limit

### Publicaciones
- Estado: `PENDIENTE → APROBADA → ENVIADA` o `RECHAZADA`
- Sin estado PROGRAMADA — programación delegada a Meta/YouTube
- Tres fechas: `fecha_generacion`, `fecha_envio`, `fecha_publicacion`
- ENVIADAS inmutables

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
- `.env` contiene: `AES_SECRET_KEY`, `ANTHROPIC_API_KEY`, `META_APP_ID`, `META_APP_SECRET`

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
    └── fragments/layout.html         ← vista unificada
```

---

## Configuración de entorno

```properties
# application.properties
spring.datasource.url=jdbc:h2:file:./data/social-manager
spring.jpa.hibernate.ddl-auto=update
storage.base-path=./storage

# .env (excluido de git)
AES_SECRET_KEY=clave-de-32-caracteres-minimo
ANTHROPIC_API_KEY=sk-ant-...
META_APP_ID=...            ← necesario para Fase 2
META_APP_SECRET=...        ← necesario para Fase 2
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

## Fase 2 — Integración Meta Graph API

### Qué cubre
- Publicar posts con imagen en Facebook (Page Post)
- Publicar posts con imagen en Instagram (Media + Publish)
- Programar publicaciones con fecha futura
- Publicar Reels en Facebook e Instagram

### Cómo obtener las credenciales de Meta

**Paso 1 — Crear la app en Meta for Developers**
1. Ve a https://developers.facebook.com
2. Inicia sesión con la cuenta de Facebook que administra la página
3. Pulsa "Mis apps" → "Crear app"
4. Tipo: "Empresa" → siguiente
5. Nombra la app (ej: "El Pisotón Community Manager")
6. Una vez creada, ve a Configuración → Básica
7. Copia el **ID de la app** y el **Secreto de la app**
8. Añádelos al `.env`:
```
META_APP_ID=123456789
META_APP_SECRET=abcdef1234567890abcdef1234567890
```

**Paso 2 — Añadir productos a la app**
1. En el panel izquierdo, pulsa "Añadir producto"
2. Añade **Graph API**
3. Añade **Instagram Graph API**

**Paso 3 — Obtener el Page Access Token de Facebook**
1. Ve a https://developers.facebook.com/tools/explorer
2. Selecciona tu app en el desplegable superior
3. Pulsa "Generar token de acceso de página"
4. Selecciona la página de El Pisotón
5. Marca los permisos: `pages_manage_posts`, `pages_read_engagement`, `instagram_basic`, `instagram_content_publish`
6. Copia el token generado

**Paso 4 — Convertir a token de larga duración (60 días)**
Los tokens del Explorer caducan en 1-2 horas. Llama a este endpoint para obtener uno de 60 días:
```
GET https://graph.facebook.com/v19.0/oauth/access_token
  ?grant_type=fb_exchange_token
  &client_id={META_APP_ID}
  &client_secret={META_APP_SECRET}
  &fb_exchange_token={short_lived_token}
```
El `access_token` devuelto dura 60 días y es el que se guarda en la app.

**Paso 5 — Obtener el Instagram Business Account ID**
La cuenta de Instagram debe estar vinculada a la página de Facebook. Llama a:
```
GET https://graph.facebook.com/v19.0/{page-id}
  ?fields=instagram_business_account
  &access_token={long_lived_token}
```
El `id` devuelto en `instagram_business_account` es el que necesita la app para publicar en Instagram.

**Paso 6 — Guardar las credenciales en la app**
Las credenciales se guardan cifradas en BBDD desde la UI:
1. Navega a la sección de configuración del cliente en la app
2. Selecciona la plataforma "Facebook" → pega el access_token → guardar
3. Selecciona la plataforma "Instagram" → pega el mismo access_token → guardar
4. La app cifra los tokens con AES antes de persistirlos — nunca se guardan en claro
5. El Page ID y el Instagram Business Account ID se configuran en `application.properties`:
```properties
meta.facebook.page-id=123456789012345
meta.instagram.business-account-id=987654321098765
```

### Endpoints que usará la integración
- `POST /{page-id}/feed` — publicar post en Facebook (inmediato o programado)
- `POST /{ig-user-id}/media` + `POST /{ig-user-id}/media_publish` — publicar en Instagram
- `POST /{page-id}/videos` — publicar Reel en Facebook
- Parámetro `published=false` + `scheduled_publish_time` (Unix timestamp) para programación futura

---

## Próximos pasos

1. Fase 2: integración Meta Graph API (Facebook + Instagram)
2. Fase 3: integración YouTube Data API v3
3. Fase 4: integración Ideogram 3 API
4. Futuro: Facebook Login OAuth para onboarding de nuevos clientes
5. Futuro: empaquetado como instalador Windows (.exe) para distribución open source

---

## Decisiones técnicas pendientes de implementar

### Facebook Login — Onboarding de clientes

**Contexto:** Actualmente las credenciales de Meta se configuran manualmente
obteniendo tokens desde Business Manager. Esto funciona para un único cliente
pero no escala bien cuando haya múltiples clientes.

**Solución propuesta:** Implementar el flujo OAuth de Facebook Login:
- Ruta `/oauth/facebook/callback` en Spring Boot
- El cliente hace clic en "Conectar con Facebook" en la UI de configuración
- Meta redirige con un code que la app intercambia por un User Access Token
- La app obtiene automáticamente el Page Access Token de cada página
- Se guarda cifrado en CREDENCIAL como siempre

**Ventajas:**
- El cliente conecta su cuenta sin necesidad de conocimientos técnicos
- Los Page Access Tokens son los correctos para publicar (evita el error
  "Unpublished posts must be posted as the page itself")
- Tokens renovables de forma independiente por cliente
- Arquitectura correcta para multi-cliente

**Requisitos Meta:**
- En modo desarrollo (app no revisada por Meta): funciona para usuarios
  añadidos como testers/admins de la app — válido para uso personal
- En producción con usuarios externos: requiere revisión de Meta
  (vídeo demostrativo, política de privacidad, etc.) — puede tardar semanas

**Cuándo implementarlo:** Cuando se añada el segundo cliente a la app.
En ese momento la gestión manual de tokens deja de ser viable.

---

*Documento actualizado el 17 de abril de 2026. Actualizar con cada sesión de desarrollo.*