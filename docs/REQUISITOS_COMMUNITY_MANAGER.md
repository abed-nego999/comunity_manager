# Documento de Requisitos Técnicos
## Community Manager
**Versión:** 3.0  
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
| Lenguaje | Java 25 |
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
- Adjuntar ficheros de contexto al evento (imágenes, PDFs) desde el chat.
- Seleccionar el cliente activo mediante el switch general en la barra lateral.

La interfaz se comporta como un chat normal, pero Claude puede generar publicaciones reales en base de datos por detrás mediante Tool Use. Claude puede hacer preguntas de aclaración antes de generar contenido.

Cada conversación está ligada a un **Evento** concreto. Al crear un evento nuevo se abre una conversación nueva. El historial completo de la conversación se envía a Claude en cada turno — Claude no tiene memoria propia.

---

### 4.2 Módulo de generación de contenido — Integración con IA

**Generación de texto:**
- Motor: Anthropic API (Claude Sonnet 4.6).
- Claude opera mediante **Tool Use**: llama a endpoints de la API REST de Spring Boot para crear publicaciones directamente en base de datos.
- El system prompt se construye dinámicamente en cada llamada concatenando:
    1. Parte fija desde `src/main/resources/prompts/system-prompt-base.txt`
    2. Configuración del cliente (`CONFIGURACION_CLIENTE` e `INSTRUCCION_PLATAFORMA`)
    3. Datos del evento activo (nombre, fecha, descripción)
- Los adjuntos de contexto del evento se envían a Claude como bloques de contenido (imágenes en base64, PDFs como documentos).

**Generación de cartelería:**
- Motor: Ideogram 3 API (`$0.08` por imagen).
- Se activa cuando el usuario solicita explícitamente la generación de un cartel.
- En perfil `dev`, las llamadas a Ideogram se simulan con mocks.

**Adjuntos manuales de publicación:**
- El usuario adjunta imagen o vídeo en el panel de aprobación antes de aprobar la publicación.

---

### 4.3 Módulo de eventos — Panel de control principal

Vista en cuadros donde cada tarjeta representa un **evento**.

Cada tarjeta muestra: nombre, fecha, estado y acceso al detalle.

Controles disponibles:
- **Nuevo evento** — formulario con nombre, fecha y descripción.
- **Editar evento** — permite modificar nombre y descripción del evento activo.

**Ventana de detalle del evento:**

| Sección | Contenido |
|---|---|
| Pendientes | Publicaciones en estado PENDIENTE |
| Aprobadas | Publicaciones en estado APROBADA o ENVIADA |
| Rechazadas | Publicaciones en estado RECHAZADA |

Cada publicación muestra badge de plataforma, texto truncado, fecha y botones de acción según estado.

---

### 4.4 Módulo de aprobación — Panel de revisión

Para cada publicación generada, el usuario puede:

- **Aprobar** → estado `APROBADA`.
- **Rechazar** → estado `RECHAZADA`.
- **Pedir cambios** → feedback en texto libre, Claude regenera.
- **Adjuntar archivo** → imagen o vídeo que acompañará la publicación.
- **Establecer fecha de publicación** → solo si `programacion_externa=true`.

---

### 4.5 Módulo de publicación — Integraciones con plataformas

La publicación se realiza mediante `POST /publicaciones/{id}/publicar`, separado del cambio de estado.

Al publicar se guarda: `fecha_envio`, `fecha_publicacion` e `id_externo`.

| publicacion_automatica | programacion_externa | Comportamiento |
|---|---|---|
| true | true | Envío a API con fecha futura |
| true | false | Envío a API inmediato |
| false | false | Manual |

**No existe scheduler en la aplicación.**

> La integración con WordPress REST API queda pospuesta indefinidamente.

---

### 4.6 Módulo de configuración

Panel web editable sin reiniciar la aplicación:

- Nombre y datos del cliente.
- Tono, restricciones y llamada a la acción del system prompt.
- Instrucciones específicas por plataforma.
- Credenciales de APIs externas (cifradas con AES).

---

### 4.7 Barra lateral de navegación

Barra lateral expandible visible en toda la aplicación:

- **Colapsada** (~60px): solo emojis.
- **Expandida** (~220px): emoji + texto.
- Estado persiste en `localStorage`.
- Secciones: 💬 Chat, 📅 Eventos.
- Selector de cliente activo — persiste en sesión al navegar.

---

## 5. Adjuntos de contexto del evento

Los adjuntos de contexto son ficheros que el usuario sube al evento para que Claude disponga de información adicional al generar contenido. Se distinguen de los adjuntos de publicación, que acompañan a un post concreto.

### 5.1 Formatos soportados

| Tipo | Extensiones | Cómo se envía a Claude |
|---|---|---|
| Imagen | jpg, jpeg, png, gif, webp | Base64 en bloque `image` |
| Documento | pdf | Bloque `document` |

**DOC/DOCX no están soportados** por la API de Anthropic. Deuda técnica aceptada — el usuario debe convertir a PDF antes de subir.

### 5.2 Almacenamiento

```
storage/clientes/{id}_{nombre}/eventos/{id}_{nombre}/contexto/
```

### 5.3 Flujo

1. El usuario sube un fichero de contexto desde el chat o el panel del evento.
2. Se guarda en disco y se registra en `ADJUNTO_CONTEXTO`.
3. En cada llamada a Claude, la app carga todos los adjuntos de contexto del evento y los incluye como bloques de contenido en la petición a la API de Anthropic.

---

## 6. API REST

Toda la lógica de negocio se realiza mediante la API REST. Especificación completa en `openapi.yaml`.

### Grupos de endpoints

| Grupo | Base path | Descripción |
|---|---|---|
| Auxiliares | `/plataformas`, `/tipos-publicacion`, `/tipos-adjunto`, `/roles-conversacion` | Catálogos de solo lectura |
| Clientes | `/clientes` | CRUD de clientes |
| Configuración | `/clientes/{id}/configuracion` | System prompt e instrucciones |
| Credenciales | `/clientes/{id}/credenciales` | Tokens de acceso cifrados |
| Eventos | `/eventos` | Gestión de eventos y conversación |
| Adjuntos contexto | `/eventos/{id}/adjuntos-contexto` | Ficheros de contexto para Claude |
| Publicaciones | `/publicaciones` | Ciclo de vida de publicaciones |
| Adjuntos | `/publicaciones/{id}/adjuntos` | Archivos multimedia de publicaciones |

### Ciclo de vida de una publicación

```
POST   /publicaciones              → Claude crea borrador (PENDIENTE)
PATCH  /publicaciones/{id}/aprobar → Usuario aprueba (APROBADA)
POST   /publicaciones/{id}/publicar → App llama a Meta/YouTube (ENVIADA)
PATCH  /publicaciones/{id}/rechazar → Usuario rechaza (RECHAZADA)
PATCH  /publicaciones/{id}/solicitar-cambios → Feedback del usuario
PUT    /publicaciones/{id}          → Claude actualiza contenido
```

---

## 7. Almacenamiento de ficheros

```
{storage.base-path}/
└── clientes/
    └── {id-cliente}_{nombre-sanitizado}/
        └── eventos/
            └── {id-evento}_{nombre-sanitizado}/
                ├── adjuntos/     ← adjuntos de publicaciones
                ├── contexto/     ← ficheros de contexto para Claude
                └── generados/   ← imágenes creadas por Ideogram
```

Ruta base configurable: `storage.base-path=./storage`

Sanitización: minúsculas, sin tildes, sin emojis, espacios a guiones.

---

## 8. Seguridad de credenciales

- Tokens cifrados con AES mediante `AttributeConverter`.
- `token_iv` en base de datos; clave maestra en `.env`.
- Tokens nunca devueltos en claro por la API.

```properties
# .env
AES_SECRET_KEY=clave-de-32-caracteres-minimo
ANTHROPIC_API_KEY=sk-ant-...
```

---

## 9. Configuración técnica de la base de datos

```properties
spring.datasource.url=jdbc:h2:file:./data/social-manager
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

---

## 10. Perfiles de entorno

| Perfil | Claude | Ideogram | H2 Console | Swagger UI |
|---|---|---|---|---|
| `dev` | Real | Mock | ✅ | ✅ |
| `prod` | Real | Real | ❌ | ❌ |

---

## 11. Costes en producción

| Servicio | Uso estimado | Coste/mes |
|---|---|---|
| Anthropic API (Claude Sonnet 4.6) | ~24 posts/mes | ~$0.50 |
| Ideogram API | ~6 carteles/mes | ~$0.50 |
| Meta Graph API | Ilimitado | $0.00 |
| YouTube Data API v3 | 10.000 unidades/día gratis | $0.00 |
| **Total** | | **~$1.00/mes** |

---

## 12. Fases de desarrollo

| Fase | Descripción | Estado |
|---|---|---|
| 0 | Entorno de desarrollo | ✅ Completada |
| 1 | Spring Boot base + API REST + Tool Use + chat + eventos | 🔄 En curso |
| 2 | Meta Graph API (Facebook + Instagram) | ⏳ Pendiente |
| 3 | YouTube Data API v3 | ⏳ Pendiente |
| 4 | Ideogram 3 API | ⏳ Pendiente |

---

## 13. Decisiones técnicas registradas

- H2 en modo fichero. `ddl-auto=update` para no perder datos al redesplegar.
- Claude real activo en todos los perfiles — el mock está desactivado.
- System prompt construido en tres capas: base fija (txt) + configuración cliente (BBDD) + datos evento activo.
- Adjuntos de contexto separados de adjuntos de publicación — entidades y propósitos distintos.
- Adjuntos de contexto enviados a Claude en cada llamada como bloques de contenido.
- DOC/DOCX no soportados por Anthropic — deuda técnica aceptada.
- Aprobación y publicación son operaciones separadas.
- Sin scheduler — programación delegada a Meta/YouTube.
- Credenciales cifradas con AES. Tokens nunca en claro.
- Nombres de carpeta sanitizados automáticamente.
- Barra lateral con estado persistente en localStorage.
- Selector de cliente activo persiste en sesión al navegar entre vistas.

---

*Documento actualizado el 14 de abril de 2026. Actualizar con cada decisión técnica relevante.*