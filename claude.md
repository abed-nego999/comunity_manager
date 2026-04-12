# Community Manager — Contexto del proyecto

## Qué es este proyecto

Aplicación web local (`localhost:8080`) en **Java 26 + Spring Boot 3.4.x** para gestionar y publicar contenido en redes sociales para múltiples clientes. El usuario es el único operador. **Claude genera contenido de forma autónoma mediante Tool Use** y el usuario supervisa y aprueba todo antes de publicar.

---

## Documentación completa

Antes de escribir código, leer los ficheros en `/docs`:

| Fichero | Contenido |
|---|---|
| `docs/ER_COMMUNITY_MANAGER.md` | Esquema E-R completo con todas las entidades, campos y decisiones técnicas |
| `docs/REQUISITOS_COMMUNITY_MANAGER.md` | Requisitos funcionales, módulos, stack, fases de desarrollo y decisiones técnicas |
| `docs/openapi.yaml` | Especificación completa de la API REST |

---

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 26 |
| Framework | Spring Boot 3.4.x |
| Vistas | Thymeleaf |
| Base de datos | H2 en modo fichero |
| ORM | Spring Data JPA |
| Utilidades | Lombok |
| Paquete base | `com.esteban.comunitymanager` |
| Repositorio | GitHub — `comunity_manager` |

---

## Estructura del proyecto

```
src/main/java/com/esteban/comunitymanager/
├── controller/       ← Controladores REST (un fichero por grupo de endpoints)
├── service/          ← Lógica de negocio
├── repository/       ← Interfaces JPA
├── model/            ← Entidades JPA
├── dto/              ← Request y Response objects
├── config/           ← Configuración de Spring, AES, etc.
└── claude/           ← Integración con Anthropic API y Tool Use

src/main/resources/
├── application.properties
├── application-dev.properties
├── application-prod.properties
├── data.sql           ← Seed de tablas auxiliares
└── templates/         ← Vistas Thymeleaf
```

---

## Entidades principales (resumen)

Leer `docs/ER_COMMUNITY_MANAGER.md` para el detalle completo. Resumen rápido:

- **PLATAFORMA** — catálogo: Facebook, Instagram, YouTube, Blog Web
- **TIPO_PUBLICACION** — tipos por plataforma con flags `publicacion_automatica` y `programacion_externa`
- **CLIENTE** — entidad raíz. Cada cliente tiene su propia configuración, credenciales y eventos
- **CONFIGURACION_CLIENTE** — parámetros del system prompt de Claude (tono, restricciones, CTA)
- **INSTRUCCION_PLATAFORMA** — instrucciones específicas por plataforma para Claude. Unicidad compuesta `(id_configuracion, id_plataforma)`
- **CREDENCIAL** — tokens de Meta y YouTube cifrados con AES. Nunca se devuelven en claro por la API
- **EVENTO** — contenedor de trabajo. Agrupa conversación + publicaciones de un mismo evento
- **MENSAJE_CONVERSACION** — historial del chat ligado al evento. Se envía completo a Claude en cada turno
- **PUBLICACION** — post/reel/vídeo generado por Claude. Estados: `PENDIENTE → APROBADA → ENVIADA` o `RECHAZADA`
- **ADJUNTO** — ficheros multimedia. Origen: `MANUAL` o `GENERADO` (Ideogram)
- **IMAGEN_GENERADA** — imágenes de Ideogram ligadas a evento o publicación

---

## API REST (resumen)

Leer `docs/openapi.yaml` para el detalle completo. Grupos de endpoints:

```
GET/POST        /plataformas, /tipos-publicacion, /tipos-adjunto, /roles-conversacion
GET/POST/PUT    /clientes
GET/PUT         /clientes/{id}/configuracion
GET/PUT         /clientes/{id}/configuracion/instrucciones/{plataformaId}
GET/PUT/DELETE  /clientes/{id}/credenciales/{plataformaId}
GET/POST/PUT    /eventos
GET/POST        /eventos/{id}/conversacion
GET/POST/PUT    /publicaciones
PATCH           /publicaciones/{id}/aprobar
PATCH           /publicaciones/{id}/rechazar
PATCH           /publicaciones/{id}/solicitar-cambios
POST            /publicaciones/{id}/publicar
GET/POST/DELETE /publicaciones/{id}/adjuntos
```

---

## Integración con Claude (Tool Use)

Claude interactúa con la app llamando a los mismos endpoints REST que el frontend. El system prompt se construye dinámicamente en cada llamada concatenando:

1. Configuración general del cliente (`CONFIGURACION_CLIENTE`)
2. Instrucciones específicas por plataforma (`INSTRUCCION_PLATAFORMA`)
3. Lista de tools disponibles (endpoints que Claude puede llamar)

Claude **solo puede crear y actualizar** publicaciones. Las acciones de aprobar, rechazar y publicar son exclusivas del usuario.

---

## Seguridad de credenciales

- Tokens cifrados con **AES** mediante `AttributeConverter` de Spring Data JPA
- El vector de inicialización (`token_iv`) se guarda en base de datos
- La clave maestra vive en `.env` → variable `AES_SECRET_KEY`
- Los tokens **nunca se devuelven en claro** por la API

---

## Almacenamiento de ficheros

```
{storage.base-path}/clientes/{id}_{nombre}/eventos/{id}_{nombre}/adjuntos|generados/
```

- Ruta base configurable en `application.properties` → `storage.base-path=./storage`
- Nombres sanitizados: minúsculas, sin tildes, sin emojis, espacios a guiones

---

## Perfiles de entorno

| Perfil | Claude | Ideogram | H2 Console | Swagger UI |
|---|---|---|---|---|
| `dev` | Mock | Mock | ✅ `/h2-console` | ✅ `/swagger-ui.html` |
| `prod` | Real | Real | ❌ | ❌ |

---

## Convenciones de código

- **Un controlador por grupo de endpoints** — `ClienteController`, `EventoController`, `PublicacionController`, etc.
- **DTOs separados de entidades** — nunca exponer entidades JPA directamente en la API
- **Nombres en español** — variables, métodos, clases y endpoints en español para consistencia con el dominio
- **Lombok** — usar `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` en entidades y DTOs
- **UUIDs como PKs** — todas las entidades usan `UUID` como identificador
- **Seed en `data.sql`** — tablas auxiliares (PLATAFORMA, TIPO_PUBLICACION, ROL_CONVERSACION, TIPO_ADJUNTO) se insertan al arrancar

---

## Estado actual del desarrollo

| Fase | Descripción | Estado |
|---|---|---|
| 0 | Entorno de desarrollo | ✅ Completada |
| 1 | Spring Boot base + API REST + Tool Use + panel aprobación | 🔄 En curso |
| 2 | Meta Graph API (Facebook + Instagram) | ⏳ Pendiente |
| 3 | YouTube Data API v3 | ⏳ Pendiente |
| 4 | Ideogram 3 API | ⏳ Pendiente |