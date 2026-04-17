-- ─────────────────────────────────────────────────────────────────────────────
-- Seed de tablas auxiliares
-- Se ejecuta cada arranque; MERGE INTO garantiza idempotencia.
-- ─────────────────────────────────────────────────────────────────────────────

-- ── PLATAFORMA ────────────────────────────────────────────────────────────────
MERGE INTO plataforma (id, nombre) KEY(id)
VALUES ('00000000-0000-0000-0000-000000000001', 'Facebook');

MERGE INTO plataforma (id, nombre) KEY(id)
VALUES ('00000000-0000-0000-0000-000000000002', 'Instagram');

MERGE INTO plataforma (id, nombre) KEY(id)
VALUES ('00000000-0000-0000-0000-000000000003', 'YouTube');

MERGE INTO plataforma (id, nombre) KEY(id)
VALUES ('00000000-0000-0000-0000-000000000004', 'Blog Web');

-- ── TIPO_PUBLICACION ──────────────────────────────────────────────────────────
-- Facebook
MERGE INTO tipo_publicacion (id, id_plataforma, nombre, publicacion_automatica, programacion_externa) KEY(id)
VALUES ('00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0000-000000000001', 'Post', true, true);

MERGE INTO tipo_publicacion (id, id_plataforma, nombre, publicacion_automatica, programacion_externa) KEY(id)
VALUES ('00000000-0000-0000-0001-000000000002', '00000000-0000-0000-0000-000000000001', 'Evento', true, true);

MERGE INTO tipo_publicacion (id, id_plataforma, nombre, publicacion_automatica, programacion_externa) KEY(id)
VALUES ('00000000-0000-0000-0001-000000000003', '00000000-0000-0000-0000-000000000001', 'Reel', true, false);

-- Instagram
MERGE INTO tipo_publicacion (id, id_plataforma, nombre, publicacion_automatica, programacion_externa) KEY(id)
VALUES ('00000000-0000-0000-0001-000000000004', '00000000-0000-0000-0000-000000000002', 'Post', true, true);

MERGE INTO tipo_publicacion (id, id_plataforma, nombre, publicacion_automatica, programacion_externa) KEY(id)
VALUES ('00000000-0000-0000-0001-000000000005', '00000000-0000-0000-0000-000000000002', 'Story', true, false);

-- YouTube
MERGE INTO tipo_publicacion (id, id_plataforma, nombre, publicacion_automatica, programacion_externa) KEY(id)
VALUES ('00000000-0000-0000-0001-000000000006', '00000000-0000-0000-0000-000000000003', 'Vídeo', true, true);

-- Blog Web
MERGE INTO tipo_publicacion (id, id_plataforma, nombre, publicacion_automatica, programacion_externa) KEY(id)
VALUES ('00000000-0000-0000-0001-000000000007', '00000000-0000-0000-0000-000000000004', 'Post', false, false);

-- ── ROL_CONVERSACION ──────────────────────────────────────────────────────────
MERGE INTO rol_conversacion (id, nombre) KEY(id)
VALUES ('00000000-0000-0000-0002-000000000001', 'Usuario');

MERGE INTO rol_conversacion (id, nombre) KEY(id)
VALUES ('00000000-0000-0000-0002-000000000002', 'Claude');

-- ── CLIENTE: ABS El Pisotón ───────────────────────────────────────────────────
MERGE INTO cliente (id, nombre, email, telefono, web, activo, creado_en) KEY(id)
VALUES (
    '00000000-0000-0000-0009-000000000001',
    'ABS El Pisotón',
    'abselpisoton@gmail.com',
    '+34 605 97 80 08',
    'www.abselpisoton.es',
    true,
    CURRENT_TIMESTAMP
);

-- ── CONFIGURACION_CLIENTE: ABS El Pisotón ─────────────────────────────────────
MERGE INTO configuracion_cliente (id, id_cliente, tono, restricciones, cta_predeterminada, actualizado_en) KEY(id)
VALUES (
    '00000000-0000-0000-0009-000000000002',
    '00000000-0000-0000-0009-000000000001',
    'Tono entusiasta, positivo e inspirador.
Lenguaje claro, directo y emocional.
Evitar jerga técnica o compleja.
Usar expresiones dinámicas y cercanas al público aficionado al baile de salón.
Emojis con moderación: refuerzan el mensaje pero no saturan.
Transmitir los valores de la asociación: comunidad, diversión, cultura, ejercicio y amistad.
El estilo debe sentirse como si lo escribiera alguien que forma parte de la asociación y vive el baile con pasión, no como una comunicación corporativa fría.',
    'POLÍTICA DE ADMISIÓN
La asociación SOLO admite parejas (matrimonio, amigos o cualquier tipo de pareja).
NUNCA mencionar la posibilidad de acudir solo.
NUNCA incluir frases como "ven solo", "no hace falta pareja", "puedes venir sin pareja" o similares.
NUNCA especificar este requisito explícitamente en los textos. Simplemente no mencionarlo en ningún sentido.

DENOMINACIÓN DE PROFESORES
Los profesores de la asociación son: Alberto de la Muela Luna (Beto Luna), Sonia Elgueta Rodríguez, Jonathan Oliver Riaño, Alberto Fernández de Cano.
Los profesores amenizan las sesiones de baile. NO son DJs.
Referirse a ellos SIEMPRE como "profesor" / "profesora" o por su nombre.
NUNCA llamarles "DJ", "animador", "pinchadiscos" ni ningún término equivalente.

OTROS
No hacer promesas sobre el número de asistentes esperados si no se proporcionan datos.
No inventar precios, horarios ni ubicaciones que no hayan sido proporcionados.
No usar el término "discoteca" para referirse al venue de eventos. Usar siempre "Centro Municipal La Esfera" o simplemente "La Esfera".',
    'Llamadas a la acción predeterminadas según contexto:
- Eventos con reserva o inscripción: "¡Reserva tu plaza!" / "¡Inscríbete ya!"
- Eventos de entrada libre: "¡No te lo pierdas!" / "¡Ven a bailar con nosotros!"
- Eventos con aforo limitado: "¡Plazas limitadas, reserva ya!" / "¡No dejes pasar tu plaza!"
- Excursiones y salidas: "¡Apúntate a la aventura!" / "¡Reserva antes del [fecha límite]!"
- Cierre genérico: "Más información en abselpisoton.es o escríbenos a abselpisoton@gmail.com"
- Contacto telefónico: incluir 605 97 80 08 cuando el evento requiera reserva activa.',
    CURRENT_TIMESTAMP
);

-- ── INSTRUCCION_PLATAFORMA: Facebook ──────────────────────────────────────────
MERGE INTO instruccion_plataforma (id, id_configuracion, id_plataforma, instrucciones) KEY(id)
VALUES (
    '00000000-0000-0000-0009-000000000003',
    '00000000-0000-0000-0009-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'Canal: Facebook | Cuenta: ABS El Pisotón (@abspisoton)

LONGITUD: 150-250 palabras.

ESTRUCTURA:
1. Gancho inicial: Primera frase llamativa que capte la atención. Puede ser una pregunta, una afirmación entusiasta o una mini historia de dos líneas.
2. Descripción del evento: Párrafo que explica qué es, cuándo, dónde y qué van a vivir los asistentes.
3. Bloque de datos clave con emojis:
   - Lugar
   - Horario
   - Entrada / Precio
   - Servicios (si aplica: bar, cena, etc.)
   - Fecha límite de reserva (si aplica)
   - Contacto (si aplica)
4. Llamada a la acción: Frase final enérgica usando las CTAs definidas para El Pisotón.
5. Hashtags: Al final del texto, en línea separada.

HASHTAGS OBLIGATORIOS: #ElPisotón #BailesdeSalón #Alcobendas + hashtags específicos del tipo de evento.

TONO: Más narrativo y cálido que Instagram. Puede permitirse un párrafo más largo porque la audiencia de Facebook de la asociación está más fidelizada y lee más.'
);

-- ── INSTRUCCION_PLATAFORMA: Instagram ─────────────────────────────────────────
MERGE INTO instruccion_plataforma (id, id_configuracion, id_plataforma, instrucciones) KEY(id)
VALUES (
    '00000000-0000-0000-0009-000000000004',
    '00000000-0000-0000-0009-000000000002',
    '00000000-0000-0000-0000-000000000002',
    'Canal: Instagram | Cuenta: @abspisoton

LONGITUD: 80-120 palabras en el cuerpo del texto + bloque de hashtags separado.

ESTRUCTURA:
1. Primera línea (gancho): CRÍTICO. Es lo único visible antes del "ver más". Debe ser impactante, directo y generar curiosidad o emoción inmediata. Máximo 10 palabras.
2. Cuerpo: Descripción breve y vibrante del evento. Datos esenciales integrados de forma natural con emojis.
3. Llamada a la acción: Una sola frase al final del cuerpo, directa.
4. Línea en blanco de separación.
5. Bloque de hashtags: 20-25 hashtags en bloque separado.

HASHTAGS OBLIGATORIOS: #ElPisotón #BailesdeSalón #Alcobendas #SanSebastiánDeLosReyes #BailesDeSalon #Baile #Dance #BaileDeSalon #Salsa #Bachata #Vals #Tango + hashtags específicos hasta completar 20-25 total.

TONO: Más directo, visual y enérgico que Facebook. Frases cortas. Emojis estratégicos. Sensación de urgencia y emoción.'
);

-- ── INSTRUCCION_PLATAFORMA: YouTube ───────────────────────────────────────────
MERGE INTO instruccion_plataforma (id, id_configuracion, id_plataforma, instrucciones) KEY(id)
VALUES (
    '00000000-0000-0000-0009-000000000005',
    '00000000-0000-0000-0009-000000000002',
    '00000000-0000-0000-0000-000000000003',
    'Canal: YouTube | Asociación: ABS El Pisotón

TÍTULO:
- Optimizado para búsqueda en YouTube.
- Incluir palabras clave relevantes: tipo de baile, nombre del evento, año, ciudad si aplica.
- Longitud: 60-70 caracteres máximo.
- Formato sugerido: [Tipo de evento] | [Nombre] | ABS El Pisotón [año]

DESCRIPCIÓN (150-200 palabras totales, tres párrafos):

Párrafo 1 - Descripción del vídeo:
Qué se ve en el vídeo, qué evento es, qué bailes aparecen, ambiente general. Escrito de forma atractiva para quien llega por búsqueda y no conoce la asociación.

Párrafo 2 - Información de la asociación:
ABS El Pisotón es una asociación de bailes de salón fundada en 1992, con más de 400 socios en Alcobendas y San Sebastián de los Reyes (Madrid).
Web: www.abselpisoton.es
Email: abselpisoton@gmail.com
Teléfono: 605 97 80 08

Párrafo 3 - Hashtags:
8-12 hashtags relevantes, en líneas o en bloque.'
);

-- ── INSTRUCCION_PLATAFORMA: Blog Web ──────────────────────────────────────────
MERGE INTO instruccion_plataforma (id, id_configuracion, id_plataforma, instrucciones) KEY(id)
VALUES (
    '00000000-0000-0000-0009-000000000006',
    '00000000-0000-0000-0009-000000000002',
    '00000000-0000-0000-0000-000000000004',
    'Canal: Blog de la asociación | URL base: https://www.abselpisoton.es/

ESTRUCTURA ANTES DEL EVENTO:

Título: Descriptivo y directo. Debe identificar claramente el evento.
Ejemplos: "Noche de Bailes de Salón en La Esfera con Jonathan Oliver", "Escapada a Segovia · 7 y 8 de marzo de 2026", "Gala Especial de San Isidro"

Subtítulo: Una sola frase evocadora que capture la esencia emocional del evento.
Ejemplos: "Una noche para bailar, disfrutar y compartir", "Cultura, gastronomía y baile en un fin de semana inolvidable"

Cuerpo:
1. Párrafo introductorio entusiasta (3-4 líneas): contexto del evento, por qué es especial, qué van a vivir los asistentes.
2. Párrafo de descripción de la actividad (3-5 líneas): detalle de qué ocurrirá, en qué orden, qué destacar.
3. Bloque de datos con emojis (Lugar, Horario, Entrada, Servicios, Reservas hasta, Contacto).
4. Frase de cierre con llamada a la acción.

ESTRUCTURA DESPUÉS DEL EVENTO (CRÓNICA):
Título y subtítulo: iguales que antes del evento.
Cuerpo ampliado:
1. Párrafo de apertura: cómo fue el evento, qué ambiente se vivió (en pasado).
2. Párrafo de desarrollo: momentos destacados, bailes, asistentes.
3. Párrafo de cierre: agradecimientos, próximos eventos.
Añadir título de galería (H3) y subtítulo de galería descriptivos.'
);
