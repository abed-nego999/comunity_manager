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

-- ── TIPO_ADJUNTO ──────────────────────────────────────────────────────────────
MERGE INTO tipo_adjunto (id, nombre) KEY(id)
VALUES ('00000000-0000-0000-0003-000000000001', 'Imagen');

MERGE INTO tipo_adjunto (id, nombre) KEY(id)
VALUES ('00000000-0000-0000-0003-000000000002', 'Vídeo');
