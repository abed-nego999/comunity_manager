# Documento de Requisitos Técnicos
## Community Manager
**Versión:** 2.0  
**Fecha:** Abril 2026  
**Estado:** En desarrollo

---

## 1. Descripción general

Aplicación web local (`localhost:8080`) desarrollada en Java/Spring Boot que permite gestionar y publicar contenido en redes sociales para múltiples clientes. El usuario es el único operador. La IA (Claude) genera contenido de forma autónoma mediante Tool Use, y el usuario supervisa y aprueba todo antes de publicar.

El primer cliente es la Asociación de Bailes de Salón El Pisotón, pero la arquitectura está diseñada para incorporar nuevos clientes sin cambios estructurales.

---

## 2. Stack tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 26 |
| Framework | Spring Boot 3.4.x |
| Gestor de dependencias | Maven 3.9.x |
| Vistas | Thymeleaf |
| Base de datos | H2 en modo fichero |
| ORM | Spring Data JPA |
| Utilidades | Lombok |
| Recarga en desarrollo | Spring DevTools |
| IDE | IntelliJ IDEA 2026.1 |
| Copiloto IA | Claude Code (plugin IntelliJ) |
| Sistema operativo | Windows 11 |
| Repositorio | GitHub — `comunity_manager` |
| Paquete base | `com.esteban.comunitymanager` |

---

## 3. Contexto del primer cliente

| Campo | Valor |
|---|---|
| Nombre | Asociación de Bailes de Salón El Pisotón |
| Fundación | 1992 |
| Socios | 400+ |
| Ubicaciones | Alcobendas y San Sebastián de los Reyes |
| Email | abselpisoton@gmail.com |
| Teléfono | +34 605 97 80 08 |
| Web | www.abselpisoton.es |

---

## 4. Módulos funcionales

### 4.1 Módulo de entrada — Interfaz de petición

El usuario accede a una interfaz conversacional donde puede:

- Redactar una petición en lenguaje natural describiendo un evento o solicitud de contenido.
- Adjuntar fotos o vídeos asociados a la petición.
- Seleccionar el cliente activo mediante el switch general.

La interfaz se comporta como un chat normal, pero Claude puede generar publicaciones reales en base de datos por detrás mediante Tool Use sin intervención del usuario. Claude puede hacer preguntas de aclaración antes de generar contenido.

Cada conversación está ligada a un **Evento** concreto. Al crear un evento nuevo se abre una conversación nueva. El historial completo de la conversación se envía a Claude en cada turno — Claude no tiene memoria propia.

---

### 4.2 Módulo de generación de contenido — Integración con IA

**Generación de texto:**
- Motor: Anthropic API (Claude Sonnet 4.6).
- Claude opera mediante **Tool Use**: llama a endpoints de la API REST de Spring Boot para crear eventos, publicaciones e imágenes directamente en base de datos.
- El system prompt se construye a partir de los campos de `CONFIGURACION_CLIENTE` e `INSTRUCCION_PLATAFORMA`, y es editable desde el panel web sin reiniciar la aplicación.
- En perfil `dev`, las llamadas a Claude se simulan con mocks para no consumir créditos.

**Generación de cartelería:**
- Motor: Ideogram 3 API (`$0.08` por imagen).
- Se activa cuando el usuario solicita explícitamente la generación de un cartel.
- Claude genera el texto del cartel e Ideogram genera la imagen.
- En perfil `dev`, las llamadas a Ideogram se simulan con mocks.

**Adjuntos manuales:**
- Para posts que no requieran cartel generado por IA, el usuario adjunta la imagen o vídeo manualmente en el panel de aprobación antes de aprobar la publicación.

---

### 4.3 Módulo de eventos — Panel de control principal

Vista en cuadros donde cada tarjeta representa un **evento**. Un evento es un contenedor que agrupa la conversación con Claude y todas las publicaciones generadas para un mismo evento o campaña.

Cada tarjeta de evento muestra:

- Nombre y fecha del evento.
- Publicaciones futuras programadas.
- Imágenes generadas asociadas.
- Información general del evento.
- Acceso a la **ventana de detalle del evento**.

**Ventana de detalle del evento:**

| Sección | Contenido |
|---|---|
| Publicaciones pendientes | Drafts generados por Claude pendientes de aprobación |
| Publicaciones enviadas | Aprobadas y enviadas a la plataforma |
| Publicaciones rechazadas | Historial de publicaciones descartadas |

---

### 4.4 Módulo de aprobación — Panel de revisión

Para cada publicación generada, el usuario puede:

- **Aprobar** → la publicación pasa a estado `APROBADA`, lista para enviar.
- **Rechazar** → la publicación queda registrada como `RECHAZADA` en el historial.
- **Pedir cambios** → el usuario escribe feedback en texto libre y continúa la conversación para que Claude regenere el contenido.
- **Adjuntar archivo** → imagen o vídeo que acompañará a la publicación.
- **Establecer fecha de publicación** → solo disponible si `programacion_externa=true` en el tipo de publicación.

---

### 4.5 Módulo de publicación — Integraciones con plataformas

La publicación se realiza mediante un endpoint explícito `POST /publicaciones/{id}/publicar`, separado del cambio de estado. Esto evita mezclar una operación local (aprobar) con una llamada a una API externa (publicar).

Al publicar, la app guarda en `PUBLICACION`:
- `fecha_envio` — momento en que la app llama a la API de la plataforma.
- `fecha_publicacion` — fecha en que la plataforma publicará el contenido al público (puede ser futura si `programacion_externa=true`).
- `id_externo` — ID devuelto por Meta o YouTube para trazabilidad.

El comportamiento de publicación se determina por los flags del `TIPO_PUBLICACION`:

| publicacion_automatica | programacion_externa | Comportamiento |
|---|---|---|
| true | true | Envío a API con fecha futura — la plataforma gestiona el timing |
| true | false | Envío a API para publicación inmediata |
| false | false | Manual — el usuario publica a mano |

**No existe scheduler en la aplicación.** La programación de publicaciones se delega completamente a Meta y YouTube, que tienen sus propios planificadores.

| Plataforma | Tipo | publicacion_automatica | programacion_externa |
|---|---|---|---|
| Facebook | Post | true | true |
| Facebook | Evento | true | true |
| Facebook | Reel | true | false |
| Instagram | Post | true | true |
| Instagram | Story | true | false |
| YouTube | Vídeo | true | true |
| Blog Web | Post | false | false |

> **Nota:** La integración con WordPress REST API queda pospuesta indefinidamente. Los posts de blog se gestionan de forma manual.

---

### 4.6 Módulo de configuración

Panel web editable sin reiniciar la aplicación que permite configurar por cliente:

- Nombre y datos del cliente.
- Tono, restricciones y llamada a la acción predeterminada del system prompt de Claude.
- Instrucciones específicas por plataforma (`INSTRUCCION_PLATAFORMA`), editables de forma independiente para cada red social.
- Credenciales de APIs externas (cifradas con AES en base de datos).

---

### 4.7 Switch general de cliente

Selector visible en toda la aplicación que permite cambiar el contexto activo entre distintos clientes. Al cambiar de cliente:

- El system prompt de Claude carga la configuración del cliente seleccionado.
- El panel de eventos muestra solo los eventos de ese cliente.
- Las publicaciones se dirigen a las cuentas de redes sociales de ese cliente.

---

## 5. API REST

Toda la lógica de negocio se realiza mediante la API REST. Tanto el frontend (Thymeleaf) como Claude (Tool Use) interactúan con la aplicación a través de estos endpoints. La especificación completa está en `openapi.yaml`.

### Grupos de endpoints

| Grupo | Base path | Descripción |
|---|---|---|
| Auxiliares | `/plataformas`, `/tipos-publicacion`, `/tipos-adjunto`, `/roles-conversacion` | Catálogos de solo lectura |
| Clientes | `/clientes` | CRUD de clientes |
| Configuración | `/clientes/{id}/configuracion` | System prompt e instrucciones por plataforma |
| Credenciales | `/clientes/{id}/credenciales` | Tokens de acceso a APIs externas |
| Eventos | `/eventos` | Gestión de eventos y conversación con Claude |
| Publicaciones | `/publicaciones` | Ciclo de vida de publicaciones |
| Adjuntos | `/publicaciones/{id}/adjuntos` | Archivos multimedia |

### Ciclo de vida de una publicación vía API

```
POST   /publicaciones              → Claude crea borrador (estado: PENDIENTE)
PATCH  /publicaciones/{id}/aprobar → Usuario aprueba (estado: APROBADA)
POST   /publicaciones/{id}/publicar → App llama a Meta/YouTube (estado: ENVIADA)

PATCH  /publicaciones/{id}/rechazar          → Usuario rechaza (estado: RECHAZADA)
PATCH  /publicaciones/{id}/solicitar-cambios → Usuario pide cambios con feedback
PUT    /publicaciones/{id}                   → Claude actualiza contenido
```

---

## 6. Almacenamiento de ficheros

### 6.1 Estructura de carpetas

```
{storage.base-path}/
└── clientes/
    └── {id-cliente}_{nombre-cliente-sanitizado}/
        └── eventos/
            └── {id-evento}_{nombre-evento-sanitizado}/
                ├── adjuntos/       ← subidos manualmente por el usuario
                └── generados/     ← imágenes creadas por Ideogram
```

**Ejemplo:**
```
storage/
└── clientes/
    └── a1b2c3_abs-el-pisoton/
        └── eventos/
            └── f4e5d6_milonga-de-verano/
                ├── adjuntos/
                │   ├── foto-evento.jpg
                │   └── video-promo.mp4
                └── generados/
                    └── cartel-milonga.png
```

### 6.2 Configuración de la ruta base

```properties
storage.base-path=./storage
```

### 6.3 Sanitización de nombres

- Conversión a minúsculas.
- Espacios y caracteres especiales reemplazados por guiones.
- Tildes y caracteres no ASCII eliminados.
- Emojis eliminados.

Ejemplo: `"Milonga de Verano 🎶"` → `milonga-de-verano`

### 6.4 Gitignore

La carpeta `storage/` se incluye en `.gitignore`.

---

## 7. Seguridad de credenciales

Las API keys y tokens de acceso a Meta Graph API y YouTube Data API se almacenan cifrados en H2 mediante AES:

- `access_token` y `refresh_token` se cifran antes de persistir usando un `AttributeConverter` de Spring Data JPA.
- El vector de inicialización AES (`token_iv`) se guarda junto al token cifrado en base de datos (no es secreto).
- La clave maestra de cifrado vive en `.env` y nunca toca la base de datos ni el repositorio.
- Los tokens **nunca se devuelven en claro** desde la API REST — los endpoints de credenciales solo devuelven metadatos (plataforma, fecha de expiración).

```properties
# .env (excluido de git)
AES_SECRET_KEY=clave-maestra-de-32-caracteres-minimo
```

---

## 8. Configuración técnica de la base de datos

```properties
spring.datasource.url=jdbc:h2:file:./data/social-manager
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=admin
spring.datasource.password=admin
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

Las carpetas `data/` y `storage/` y los ficheros `*.mv.db`, `*.trace.db` y `.env` se incluyen en `.gitignore`.

---

## 9. Perfiles de entorno

### Perfil `dev`
- Mocks activos para Claude e Ideogram (sin gasto de créditos).
- Consola H2 disponible en `/h2-console`.
- Swagger UI disponible en `/swagger-ui.html`.
- Logs en nivel DEBUG.
- Spring DevTools activo.

### Perfil `prod`
- APIs reales activas.
- Consola H2 desactivada.
- Swagger UI desactivado.
- Logs en nivel ERROR/INFO.

---

## 10. Costes en producción

| Servicio | Uso estimado | Coste/mes |
|---|---|---|
| Anthropic API (Claude Sonnet 4.6) | ~24 posts/mes × ~500 tokens | ~$0.50 |
| Ideogram API | ~6 carteles/mes × $0.08 | ~$0.50 |
| Meta Graph API | Ilimitado | $0.00 |
| YouTube Data API v3 | 10.000 unidades/día gratis | $0.00 |
| **Total** | | **~$1.00/mes** |

---

## 11. Fases de desarrollo

| Fase | Descripción | Estado |
|---|---|---|
| 0 | Entorno de desarrollo (Node, Maven, IntelliJ, Claude Code) | ✅ Completada |
| 1 | Proyecto Spring Boot base + API REST + integración Anthropic (Tool Use) + panel de aprobación | 🔄 En curso |
| 2 | Integración Meta Graph API (Facebook + Instagram) + endpoint `/publicar` | ⏳ Pendiente |
| 3 | Integración YouTube Data API v3 | ⏳ Pendiente |
| 4 | Generación de cartelería con Ideogram 3 API | ⏳ Pendiente |

---

## 12. Decisiones técnicas registradas

- H2 en modo fichero para persistencia local sin instalar nada adicional.
- `ddl-auto=update` para no perder datos al redesplegar durante el desarrollo.
- Mocks de APIs externas en perfil `dev` para desarrollo sin coste.
- Toda la lógica de negocio se expone mediante API REST. Tanto el frontend como Claude (Tool Use) usan los mismos endpoints.
- Claude opera mediante Tool Use llamando directamente a los endpoints de la API REST.
- El system prompt se construye a partir de `CONFIGURACION_CLIENTE` e `INSTRUCCION_PLATAFORMA`, editable desde el panel sin tocar código.
- La conversación con Claude es parte del Evento — cada evento tiene su propio historial aislado.
- Aprobación y publicación son operaciones separadas — `PATCH /aprobar` cambia estado local, `POST /publicar` llama a la API externa.
- Sin scheduler en Spring Boot — la programación de publicaciones se delega a Meta/YouTube mediante el flag `programacion_externa` en `TIPO_PUBLICACION`.
- Dos fechas en `PUBLICACION`: `fecha_envio` (cuando la app llama a la API) y `fecha_publicacion` (cuando la plataforma publica al público).
- `PUBLICACION.id_externo` almacena el ID devuelto por Meta o YouTube para trazabilidad.
- Ciclo de vida simplificado: `PENDIENTE → APROBADA → ENVIADA` o `PENDIENTE → RECHAZADA`.
- Credenciales de APIs externas cifradas con AES en H2. Clave maestra en `.env`. Tokens nunca devueltos en claro por la API.
- Ficheros multimedia organizados en `storage/clientes/{id}_{nombre}/eventos/{id}_{nombre}/` con subcarpetas `adjuntos/` y `generados/`.
- Ruta base de almacenamiento configurable en `application.properties`.
- Nombres de carpeta sanitizados automáticamente (minúsculas, sin tildes, sin emojis, espacios a guiones).
- `TIPO_PUBLICACION` controla el comportamiento de publicación mediante dos flags: `publicacion_automatica` y `programacion_externa`. No varía por cliente.
- Swagger UI disponible en perfil `dev` mediante springdoc-openapi.

---

*Documento actualizado el 12 de abril de 2026. Actualizar con cada decisión técnica relevante.*
