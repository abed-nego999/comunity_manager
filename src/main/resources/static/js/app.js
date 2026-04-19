// ═══════════════════════ ESTADO COMPARTIDO ════════════════════════════════════
let clienteIdActivo = sessionStorage.getItem('clienteActivoId') || localStorage.getItem('clienteActivoId') || null;
const _urlParams     = new URLSearchParams(window.location.search);
const _eventoIdParam = _urlParams.get('eventoId');

// ═══════════════════════ NAVEGACIÓN SPA ══════════════════════════════════════
let panelActivo = null;

function navegarA(panel) {
    panelActivo = panel;
    const pChat = document.getElementById('panel-chat');
    const pEv   = document.getElementById('panel-eventos');
    pChat.style.display = panel === 'chat'    ? 'flex' : 'none';
    pEv.style.display   = panel === 'eventos' ? 'flex' : 'none';
    document.getElementById('navChat').classList.toggle('active', panel === 'chat');
    document.getElementById('navEventos').classList.toggle('active', panel === 'eventos');
    localStorage.setItem('panelActivo', panel);
    if (panel === 'eventos') {
        iniciarPolling();
    } else {
        detenerPolling();
    }
}

// ═══════════════════════ POLLING + SYNC DOT ══════════════════════════════════
let _pollingTimer = null;

function iniciarPolling() {
    actualizarSyncDot(!!evEvento);
    if (_pollingTimer) return;
    if (!evEvento) return;
    _pollingTimer = setInterval(pollPublicaciones, 4000);
}

function detenerPolling() {
    if (_pollingTimer) { clearInterval(_pollingTimer); _pollingTimer = null; }
    actualizarSyncDot(false);
}

function actualizarSyncDot(activo) {
    const dot = document.getElementById('syncDot');
    if (dot) dot.className = activo ? 'sync-dot active' : 'sync-dot';
}

async function pollPublicaciones() {
    if (!evEvento) return;
    try {
        const todas = await evFetchJson(`/api/v1/publicaciones?eventoId=${evEvento.id}`);
        actualizarPublicacionesIncremental(todas);
    } catch (_) { /* silencioso */ }
}

async function forzarPollInmediato() {
    await pollPublicaciones();
}

function actualizarPublicacionesIncremental(todas) {
    const nuevo = {
        PENDIENTE: todas.filter(p => p.estado === 'PENDIENTE'),
        APROBADA:  todas.filter(p => p.estado === 'APROBADA' || p.estado === 'ENVIADA'),
        RECHAZADA: todas.filter(p => p.estado === 'RECHAZADA')
    };
    document.getElementById('countPendiente').textContent = nuevo.PENDIENTE.length;
    document.getElementById('countAprobada').textContent  = nuevo.APROBADA.length;
    document.getElementById('countRechazada').textContent = nuevo.RECHAZADA.length;

    const listaActual  = evPubCache[evTabActiva] || [];
    const listaNueva   = nuevo[evTabActiva] || [];
    const idsActuales  = new Set(listaActual.map(p => p.id));
    const idsNuevos    = new Set(listaNueva.map(p => p.id));

    const nuevas      = listaNueva.filter(p => !idsActuales.has(p.id));
    const cambiadas   = listaNueva.filter(p => { const a = listaActual.find(x => x.id === p.id); return a && a.estado !== p.estado; });
    const eliminadas  = listaActual.filter(p => !idsNuevos.has(p.id));

    evPubCache = nuevo;

    if (!nuevas.length && !cambiadas.length && !eliminadas.length) return;

    const nuevasIds = new Set(nuevas.map(p => p.id));
    renderizarTabEv(evTabActiva, nuevasIds);
}

// ═══════════════════════ CHAT PANEL JS ═══════════════════════════════════════
let chatEventoId  = null;
let chatEvento    = null;
let pendingFiles  = [];
let mensajePendienteAutoEnvio = _urlParams.get('feedback') || _urlParams.get('mensaje') || null;

const eventoSelect         = document.getElementById('eventoSelect');
const nuevoEventoBtn       = document.getElementById('nuevoEventoBtn');
const editarEventoBtn      = document.getElementById('editarEventoBtn');
const chatPlaceholder      = document.getElementById('chatPlaceholder');
const placeholderTexto     = document.getElementById('placeholderTexto');
const chatActivo           = document.getElementById('chatActivo');
const mensajesDiv          = document.getElementById('mensajes');
const mensajeInput         = document.getElementById('mensajeInput');
const enviarBtn            = document.getElementById('enviarBtn');
const topbarInfo           = document.getElementById('topbarInfo');
const clipBtn              = document.getElementById('clipBtn');
const fileInputContexto    = document.getElementById('fileInputContexto');
const adjuntosPanelWrapper = document.getElementById('adjuntosPanelWrapper');
const adjuntosContadorBtn  = document.getElementById('adjuntosContadorBtn');
const adjuntosCount        = document.getElementById('adjuntosCount');
const adjuntosPanel        = document.getElementById('adjuntosPanel');
const adjuntosLista        = document.getElementById('adjuntosLista');
const pendingFilesContainer= document.getElementById('pendingFilesContainer');

function chatOnClienteChanged(clienteId) {
    clienteIdActivo = clienteId || null;
    chatEventoId = null;
    chatEvento = null;
    eventoSelect.innerHTML = '<option value="">— Selecciona un evento —</option>';
    eventoSelect.disabled = true;
    nuevoEventoBtn.style.display = 'none';
    editarEventoBtn.style.display = 'none';
    topbarInfo.style.display = 'none';
    ocultarChat();
    if (!clienteIdActivo) {
        placeholderTexto.textContent = 'Selecciona un cliente en la barra lateral para comenzar';
        return;
    }
    placeholderTexto.textContent = 'Selecciona o crea un evento para comenzar';
    chatCargarEventos(clienteIdActivo);
}

async function chatCargarEventos(clienteId) {
    try {
        const eventos = await fetchJson(`/api/v1/eventos?clienteId=${clienteId}`);
        eventos.forEach(e => {
            const opt = document.createElement('option');
            opt.value = e.id;
            opt.textContent = e.nombre + (e.fechaEvento ? ` (${e.fechaEvento})` : '');
            eventoSelect.appendChild(opt);
        });
        eventoSelect.disabled = false;
        nuevoEventoBtn.style.display = 'block';

        if (_eventoIdParam && eventoSelect.querySelector(`option[value="${_eventoIdParam}"]`)) {
            eventoSelect.value = _eventoIdParam;
            eventoSelect.dispatchEvent(new Event('change'));
        } else {
            const sessionEventoId = sessionStorage.getItem('eventoActivoId');
            if (sessionEventoId && eventoSelect.querySelector(`option[value="${sessionEventoId}"]`)) {
                eventoSelect.value = sessionEventoId;
                eventoSelect.dispatchEvent(new Event('change'));
            }
        }
    } catch (err) { mostrarToast('Error al cargar eventos: ' + err.message); }
}

eventoSelect.addEventListener('change', async () => {
    chatEventoId = eventoSelect.value || null;
    chatEvento = null;
    if (chatEventoId) sessionStorage.setItem('eventoActivoId', chatEventoId);
    else              sessionStorage.removeItem('eventoActivoId');
    ocultarChat();
    editarEventoBtn.style.display = 'none';
    clipBtn.disabled = true;
    if (!chatEventoId) { topbarInfo.style.display = 'none'; return; }

    try {
        const [mensajes, eventoData] = await Promise.all([
            fetchJson(`/api/v1/eventos/${chatEventoId}/conversacion`),
            fetchJson(`/api/v1/eventos/${chatEventoId}`)
        ]);
        chatEvento = eventoData;
        mostrarChatActivo();
        mensajesDiv.innerHTML = '';
        mensajes.forEach(m => agregarMensajeAlChat(m.rol.nombre, m.contenido, m.enviadoEn, m.adjuntos || []));

        if (mensajes.length > 0) {
            const ultimo = mensajes[mensajes.length - 1];
            if (ultimo.rol.nombre === 'Usuario') {
                const lastEl = mensajesDiv.lastElementChild;
                if (lastEl) agregarBadgeSinRespuesta(lastEl, ultimo.contenido);
            }
        }
        scrollAlFinal();
        topbarInfo.textContent = mensajes.length + ' mensaje' + (mensajes.length !== 1 ? 's' : '');
        topbarInfo.style.display = 'block';
        editarEventoBtn.style.display = 'block';
        clipBtn.disabled = false;
        await cargarAdjuntosEvento(chatEventoId);

        if (mensajePendienteAutoEnvio) {
            const texto = mensajePendienteAutoEnvio;
            mensajePendienteAutoEnvio = null;
            window.history.replaceState({}, '', window.location.pathname);
            mensajeInput.value = texto;
            await enviarMensaje();
        }
    } catch (err) { mostrarToast('Error al cargar conversación: ' + err.message); }
});

enviarBtn.addEventListener('click', enviarMensaje);
mensajeInput.addEventListener('keydown', e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); enviarMensaje(); } });
mensajeInput.addEventListener('input', () => {
    mensajeInput.style.height = 'auto';
    mensajeInput.style.height = Math.min(mensajeInput.scrollHeight, 120) + 'px';
});

async function enviarMensaje() {
    const contenido = mensajeInput.value.trim();
    if (!contenido || !chatEventoId) return;
    mensajeInput.value = '';
    mensajeInput.style.height = 'auto';
    const archivosEnTurno = [...pendingFiles];
    limpiarPendingFiles();
    setBloqueado(true);

    // Fase 1: subir ficheros
    const adjuntosSubidos = [];
    if (archivosEnTurno.length > 0) {
        const procesandoId = mostrarProcesando(archivosEnTurno.length);
        scrollAlFinal();
        for (const archivo of archivosEnTurno) {
            try {
                const fd = new FormData();
                fd.append('fichero', archivo);
                const adj = await fetchMultipart(`/api/v1/eventos/${chatEventoId}/adjuntos`, fd);
                adjuntosSubidos.push(adj);
            } catch (uploadErr) {
                quitarProcesando(procesandoId);
                mostrarToast(`Error al subir "${archivo.name}": ${uploadErr.message}`);
                setBloqueado(false);
                mensajeInput.focus();
                return;
            }
        }
        quitarProcesando(procesandoId);
        await cargarAdjuntosEvento(chatEventoId);
    }

    // Fase 2: burbuja usuario
    agregarMensajeAlChat('Usuario', contenido, new Date().toISOString(), adjuntosSubidos);
    scrollAlFinal();

    // Fase 3: llamar a Claude
    const typingId = mostrarTyping();

    // Añadir referencias explícitas a los adjuntos subidos en este turno
    let contenidoEnviado = contenido;
    if (adjuntosSubidos.length > 0) {
        const refs = adjuntosSubidos
            .map(a => `[Adjunto subido: ${a.nombreFichero} | ID: ${a.id}]`)
            .join('\n');
        contenidoEnviado = contenido + '\n' + refs;
    }

    try {
        const respuesta = await fetchJson(`/api/v1/eventos/${chatEventoId}/conversacion`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ contenido: contenidoEnviado })
        });
        quitarTyping(typingId);
        agregarMensajeAlChat('Claude', respuesta.mensaje.contenido, respuesta.mensaje.enviadoEn, []);
        if (respuesta.publicacionesCreadas && respuesta.publicacionesCreadas.length > 0) {
            agregarBadgePublicaciones(respuesta.publicacionesCreadas.length);
        }
        scrollAlFinal();
    } catch (err) {
        quitarTyping(typingId);
        mostrarToast('Error al enviar mensaje: ' + err.message);
    } finally {
        setBloqueado(false);
        mensajeInput.focus();
    }
}

// Elimina las referencias internas "[Adjunto subido: ... | ID: ...]" del texto visible
function limpiarTextoMensaje(texto) {
    return texto.replace(/\[Adjunto subido: .+? \| ID: [0-9a-f-]+\]\n?/g, '').trim();
}

// Genera el HTML de los adjuntos de un mensaje (historial o turno nuevo)
function renderAdjuntos(adjuntos) {
    if (!adjuntos || adjuntos.length === 0) return '';
    const chips = adjuntos.map(adj => {
        if (adj._pendiente) {
            if (esImagen(adj.mime)) return `<div class="adjunto-upload-ph" data-pending title="${escapeHtml(adj.nombre)}"></div>`;
            return `<span class="adjunto-chip" data-pending>${esPdf(adj.mime)?'📄':'🖼'} <span class="adjunto-chip-nombre">${escapeHtml(adj.nombre)}</span></span>`;
        }
        const mime = adj.tipoMime || adj.mime || '';
        const nombre = adj.nombreFichero || adj.nombre || '';
        const ruta = adj.rutaFichero || '';
        if (esImagen(mime)) {
            const url = '/api/v1/ficheros?ruta=' + encodeURIComponent(ruta);
            return `<a href="${url}" target="_blank" rel="noopener noreferrer" style="display:inline-block">` +
                   `<img src="${url}" alt="${escapeHtml(nombre)}" title="${escapeHtml(nombre)}" ` +
                   `style="max-width:160px;max-height:120px;object-fit:cover;border-radius:8px;cursor:pointer;margin-top:4px"></a>`;
        }
        const icono = esPdf(mime) ? '📄' : esVideo(mime) ? '🎬' : '🖼';
        return `<span class="adjunto-chip">${icono} <a href="/api/v1/ficheros?ruta=${encodeURIComponent(ruta)}" ` +
               `target="_blank" rel="noopener noreferrer" class="adjunto-chip-nombre" title="${escapeHtml(nombre)}">${escapeHtml(nombre)}</a></span>`;
    }).join('');
    return `<div class="mensaje-adjuntos">${chips}</div>`;
}

function agregarMensajeAlChat(rol, contenido, timestamp, adjuntos) {
    const esClaude = rol === 'Claude';
    const div = document.createElement('div');
    div.className = 'mensaje ' + (esClaude ? 'claude' : 'usuario');
    const hora = timestamp ? formatearHora(timestamp) : '';
    const textoVisible = limpiarTextoMensaje(contenido);
    const adjuntosHtml = renderAdjuntos(adjuntos);
    div.innerHTML = `<div class="mensaje-burbuja">${escapeHtml(textoVisible)}${adjuntosHtml}</div><div class="mensaje-meta">${esClaude?'Claude':'Tú'}${hora?' · '+hora:''}</div>`;
    mensajesDiv.appendChild(div);
    return div;
}

function agregarBadgeSinRespuesta(mensajeEl, contenido) {
    const wrap = document.createElement('div');
    wrap.className = 'sin-respuesta-wrap';
    wrap.innerHTML = `<span class="badge-sin-resp">Sin respuesta</span><button class="btn-reintentar" type="button">Reintentar</button>`;
    wrap.querySelector('.btn-reintentar').addEventListener('click', async () => {
        wrap.remove();
        mensajeInput.value = contenido;
        await enviarMensaje();
    });
    mensajeEl.after(wrap);
}

function agregarBadgePublicaciones(cantidad) {
    const div = document.createElement('div');
    div.className = 'publicaciones-badge';
    const pub  = cantidad > 1 ? 'publicaciones' : 'publicación';
    const crea = cantidad > 1 ? 'creadas' : 'creada';
    const enlace = document.createElement('a');
    enlace.href = '#';
    enlace.style.cssText = 'color:#7c6af7;font-weight:700;text-decoration:none;';
    enlace.textContent = 'Ver en Eventos →';
    enlace.addEventListener('mouseover',  () => enlace.style.textDecoration = 'underline');
    enlace.addEventListener('mouseout',   () => enlace.style.textDecoration = 'none');
    enlace.addEventListener('click', async e => {
        e.preventDefault();
        navegarA('eventos');
        // Si el panel eventos ya tiene el mismo evento, forzar poll inmediato
        if (evEvento && evEvento.id === chatEventoId) {
            await forzarPollInmediato();
        } else if (chatEventoId && evEvento && evEvento.id !== chatEventoId) {
            // Cargar el evento correcto en panel eventos
            const target = evTodosEventos.find(x => x.id === chatEventoId);
            if (target) {
                const card = document.querySelector(`#evEventosList [data-id="${chatEventoId}"]`);
                if (card) await evSeleccionarEvento(target, card);
            }
        }
    });
    div.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg> ✓ ${cantidad} ${pub} ${crea} — `;
    div.appendChild(enlace);
    mensajesDiv.appendChild(div);
}

function mostrarTyping() {
    const id = 'typing-' + Date.now();
    const div = document.createElement('div');
    div.id = id;
    div.className = 'mensaje claude';
    div.innerHTML = `<div class="mensaje-burbuja" style="padding:10px 14px;"><div class="typing-dots"><span></span><span></span><span></span></div></div>`;
    mensajesDiv.appendChild(div);
    scrollAlFinal();
    return id;
}
function quitarTyping(id) { const el = document.getElementById(id); if (el) el.remove(); }

function mostrarChatActivo() {
    chatPlaceholder.style.display = 'none';
    chatActivo.style.display = 'flex';
    mensajesDiv.innerHTML = '';
}

function ocultarChat() {
    chatActivo.style.display = 'none';
    chatPlaceholder.style.display = 'flex';
    mensajesDiv.innerHTML = '';
    adjuntosPanelWrapper.style.display = 'none';
    adjuntosPanel.style.display = 'none';
    adjuntosLista.innerHTML = '';
    clipBtn.disabled = true;
    limpiarPendingFiles();
}

function scrollAlFinal() { mensajesDiv.scrollTop = mensajesDiv.scrollHeight; }

function setBloqueado(b) {
    enviarBtn.disabled = b;
    mensajeInput.disabled = b;
    clipBtn.disabled = b || !chatEventoId;
    enviarBtn.innerHTML = b
        ? '<div class="spinner"></div>'
        : `<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg>`;
}

// Ficheros pendientes
clipBtn.addEventListener('click', () => { if (!chatEventoId) return; fileInputContexto.value = ''; fileInputContexto.click(); });
fileInputContexto.addEventListener('change', () => {
    Array.from(fileInputContexto.files).forEach(f => pendingFiles.push(f));
    fileInputContexto.value = '';
    renderPendingFiles();
    mensajeInput.focus();
});

function renderPendingFiles() {
    pendingFilesContainer.innerHTML = '';
    if (pendingFiles.length === 0) { pendingFilesContainer.style.display = 'none'; return; }
    pendingFilesContainer.style.display = 'flex';
    pendingFiles.forEach((file, idx) => {
        const chip = document.createElement('div');
        chip.className = 'pending-chip';
        chip.innerHTML = `<span>${esPdf(file.type)?'📄':'🖼'}</span><span class="pending-chip-nombre">${escapeHtml(file.name)}</span><button class="pending-chip-clear" type="button" title="Quitar">&times;</button>`;
        chip.querySelector('.pending-chip-clear').addEventListener('click', () => { pendingFiles.splice(idx, 1); renderPendingFiles(); });
        pendingFilesContainer.appendChild(chip);
    });
}
function limpiarPendingFiles() { pendingFiles = []; renderPendingFiles(); fileInputContexto.value = ''; }

function mostrarProcesando(cantidad) {
    const id = 'procesando-' + Date.now();
    const div = document.createElement('div');
    div.id = id;
    div.className = 'procesando-indicator';
    div.innerHTML = `<div class="spinner"></div> ${cantidad > 1 ? `Procesando ${cantidad} imágenes...` : 'Procesando imagen...'}`;
    mensajesDiv.appendChild(div);
    return id;
}
function quitarProcesando(id) { if (id) { const el = document.getElementById(id); if (el) el.remove(); } }

// Panel de adjuntos del evento
adjuntosContadorBtn.addEventListener('click', e => {
    e.stopPropagation();
    adjuntosPanel.style.display = adjuntosPanel.style.display !== 'none' ? 'none' : 'block';
});
document.addEventListener('click', e => { if (!adjuntosPanelWrapper.contains(e.target)) adjuntosPanel.style.display = 'none'; });

async function cargarAdjuntosEvento(idEvento) {
    try {
        const adjuntos = await fetchJson(`/api/v1/eventos/${idEvento}/adjuntos`);
        renderAdjuntosEnPanel(adjuntos);
    } catch (_) {}
}
function renderAdjuntosEnPanel(adjuntos) {
    adjuntosCount.textContent = adjuntos.length;
    adjuntosPanelWrapper.style.display = 'block';
    adjuntosLista.innerHTML = '';
    if (adjuntos.length === 0) { adjuntosLista.innerHTML = '<div class="adjuntos-panel-vacio">Sin ficheros adjuntos</div>'; return; }
    adjuntos.forEach(adj => {
        const item = document.createElement('div');
        item.className = 'adjunto-item';
        const icono = esPdf(adj.tipoMime)
            ? '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>'
            : '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>';
        item.innerHTML = `<span class="adjunto-icono">${icono}</span><span class="adjunto-nombre"><a href="/api/v1/ficheros?ruta=${encodeURIComponent(adj.rutaFichero)}" target="_blank" rel="noopener noreferrer" title="${escapeHtml(adj.nombreFichero)}">${escapeHtml(adj.nombreFichero)}</a></span><button class="btn-eliminar-adjunto" data-id="${adj.id}" title="Eliminar adjunto">&times;</button>`;
        item.querySelector('.btn-eliminar-adjunto').addEventListener('click', async e => {
            const adjId = e.currentTarget.dataset.id;
            try { await fetchDelete(`/api/v1/adjuntos/${adjId}?contexto=mensaje`); await cargarAdjuntosEvento(chatEventoId); }
            catch (err) { mostrarToast('Error al eliminar el adjunto: ' + err.message); }
        });
        adjuntosLista.appendChild(item);
    });
}

// Modal nuevo evento (chat)
nuevoEventoBtn.addEventListener('click', () => {
    document.getElementById('eventoNombre').value = '';
    document.getElementById('eventoFecha').value = '';
    document.getElementById('eventoDescripcion').value = '';
    document.getElementById('modalNuevoEvento').style.display = 'flex';
    document.getElementById('eventoNombre').focus();
});
document.getElementById('cancelarEvento').addEventListener('click', () => document.getElementById('modalNuevoEvento').style.display = 'none');
document.getElementById('modalNuevoEvento').addEventListener('click', e => { if (e.target === document.getElementById('modalNuevoEvento')) document.getElementById('modalNuevoEvento').style.display = 'none'; });
document.getElementById('crearEvento').addEventListener('click', async () => {
    const nombre = document.getElementById('eventoNombre').value.trim();
    if (!nombre) { mostrarToast('El nombre del evento es obligatorio'); return; }
    const body = { clienteId: clienteIdActivo, nombre, estado: 'ACTIVO' };
    const fecha = document.getElementById('eventoFecha').value;
    const desc  = document.getElementById('eventoDescripcion').value.trim();
    if (fecha) body.fechaEvento = fecha;
    if (desc)  body.descripcion = desc;
    try {
        const evento = await fetchJson('/api/v1/eventos', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
        const opt = document.createElement('option');
        opt.value = evento.id;
        opt.textContent = evento.nombre + (evento.fechaEvento ? ` (${evento.fechaEvento})` : '');
        eventoSelect.appendChild(opt);
        eventoSelect.value = evento.id;
        eventoSelect.dispatchEvent(new Event('change'));
        document.getElementById('modalNuevoEvento').style.display = 'none';
        mostrarToast('Evento "' + evento.nombre + '" creado');
    } catch (err) { mostrarToast('Error al crear el evento: ' + err.message); }
});

// Modal editar evento (chat)
editarEventoBtn.addEventListener('click', () => {
    if (!chatEvento) return;
    document.getElementById('editNombre').value = chatEvento.nombre || '';
    document.getElementById('editFecha').value = chatEvento.fechaEvento || '';
    document.getElementById('editDescripcion').value = chatEvento.descripcion || '';
    document.getElementById('modalEditarEvento').style.display = 'flex';
    document.getElementById('editNombre').focus();
});
document.getElementById('cancelarEditar').addEventListener('click', () => document.getElementById('modalEditarEvento').style.display = 'none');
document.getElementById('modalEditarEvento').addEventListener('click', e => { if (e.target === document.getElementById('modalEditarEvento')) document.getElementById('modalEditarEvento').style.display = 'none'; });
document.getElementById('guardarEditar').addEventListener('click', async () => {
    const nombre = document.getElementById('editNombre').value.trim();
    if (!nombre) { mostrarToast('El nombre del evento es obligatorio'); return; }
    const body = { clienteId: clienteIdActivo, nombre };
    const fecha = document.getElementById('editFecha').value;
    const desc  = document.getElementById('editDescripcion').value.trim();
    if (fecha) body.fechaEvento = fecha;
    if (desc)  body.descripcion = desc;
    try {
        const actualizado = await fetchJson(`/api/v1/eventos/${chatEventoId}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
        chatEvento = actualizado;
        const opt = eventoSelect.querySelector(`option[value="${chatEventoId}"]`);
        if (opt) opt.textContent = actualizado.nombre + (actualizado.fechaEvento ? ` (${actualizado.fechaEvento})` : '');
        document.getElementById('modalEditarEvento').style.display = 'none';
        mostrarToast('Evento actualizado correctamente');
    } catch (err) { mostrarToast('Error al actualizar el evento: ' + err.message); }
});

// ═══════════════════════ EVENTOS PANEL JS ════════════════════════════════════
let evEvento           = null;
let evTabActiva        = 'PENDIENTE';
let evPubCache         = {};
let evFeedbackPubId    = null;
let evAdjuntoUploadId  = null;
let evTodosEventos     = [];
let evMostrarCerrados  = false;

const evEventosList         = document.getElementById('evEventosList');
const evListPlaceholder     = document.getElementById('evListPlaceholder');
const evBtnNuevo            = document.getElementById('evBtnNuevoEvento');
const evToggleCerradosLabel = document.getElementById('evToggleCerradosLabel');
const evMostrarCerradosChk  = document.getElementById('evMostrarCerradosChk');
const evDetailPlaceholder   = document.getElementById('evDetailPlaceholder');
const evEventoDetailEl      = document.getElementById('evEventoDetail');
const evPublicacionesArea   = document.getElementById('evPublicacionesArea');
const evFileInputAdjunto    = document.getElementById('evFileInputAdjunto');

function evOnClienteChanged(clienteId) {
    evEvento = null;
    evTodosEventos = [];
    evPubCache = {};
    evEventosList.innerHTML = '';
    evOcultarDetalle();
    detenerPolling();

    if (!clienteId) {
        evListPlaceholder.style.display = 'block';
        evListPlaceholder.textContent = 'Selecciona un cliente en la barra lateral para ver sus eventos';
        evBtnNuevo.style.display = 'none';
        evToggleCerradosLabel.style.display = 'none';
        return;
    }
    evListPlaceholder.style.display = 'block';
    evListPlaceholder.textContent = 'Cargando eventos…';
    evBtnNuevo.style.display = 'block';
    evToggleCerradosLabel.style.display = 'flex';
    evCargarEventos(clienteId);
}

async function evCargarEventos(clienteId) {
    try {
        evTodosEventos = await evFetchJson(`/api/v1/eventos?clienteId=${clienteId}`);
        evAplicarFiltro();

        const autoSelectId = _eventoIdParam || sessionStorage.getItem('eventoActivoId');
        if (autoSelectId) {
            const target = evTodosEventos.find(e => e.id === autoSelectId);
            if (target) {
                let card = evEventosList.querySelector(`[data-id="${autoSelectId}"]`);
                if (!card && target.estado === 'CERRADO') {
                    evMostrarCerrados = true;
                    evMostrarCerradosChk.checked = true;
                    evAplicarFiltro();
                    card = evEventosList.querySelector(`[data-id="${autoSelectId}"]`);
                }
                if (card) await evSeleccionarEvento(target, card);
            }
        }
    } catch (err) {
        evListPlaceholder.textContent = 'Error al cargar eventos.';
        mostrarToast('Error: ' + err.message);
    }
}

evMostrarCerradosChk.addEventListener('change', () => {
    evMostrarCerrados = evMostrarCerradosChk.checked;
    evAplicarFiltro();
});

function evAplicarFiltro() {
    const visibles = evMostrarCerrados ? evTodosEventos : evTodosEventos.filter(e => e.estado !== 'CERRADO');
    evEventosList.innerHTML = '';
    if (visibles.length === 0) {
        evListPlaceholder.style.display = 'block';
        evListPlaceholder.textContent = evTodosEventos.length === 0
            ? 'No hay eventos. Crea el primero con + Nuevo.'
            : 'No hay eventos activos. Activa "Cerrados" para verlos.';
        return;
    }
    evListPlaceholder.style.display = 'none';
    visibles.forEach(e => evEventosList.appendChild(evCrearTarjeta(e)));
    if (evEvento) {
        const card = evEventosList.querySelector(`[data-id="${evEvento.id}"]`);
        if (card) card.classList.add('selected');
    }
}

function evCrearTarjeta(evento) {
    const card = document.createElement('div');
    card.className = 'evento-card';
    card.dataset.id = evento.id;
    const fechaTxt = evento.fechaEvento
        ? new Date(evento.fechaEvento + 'T00:00:00').toLocaleDateString('es-ES', { day:'2-digit', month:'short', year:'numeric' })
        : 'Sin fecha';
    const header = document.createElement('div');
    header.className = 'evento-card-header';
    const nombre = document.createElement('span');
    nombre.className = 'evento-nombre';
    nombre.textContent = evento.nombre;
    const badge = document.createElement('span');
    badge.className = 'badge-estado ' + evento.estado;
    badge.textContent = evFormatEstado(evento.estado);
    header.appendChild(nombre);
    header.appendChild(badge);
    const fecha = document.createElement('div');
    fecha.className = 'evento-fecha';
    fecha.textContent = fechaTxt;
    card.appendChild(header);
    card.appendChild(fecha);
    card.addEventListener('click', () => evSeleccionarEvento(evento, card));
    return card;
}

function evFormatEstado(estado) {
    return { BORRADOR: 'Borrador', ACTIVO: 'Activo', CERRADO: 'Cerrado' }[estado] || estado;
}

async function evSeleccionarEvento(evento, cardEl) {
    document.querySelectorAll('#evEventosList .evento-card.selected').forEach(c => c.classList.remove('selected'));
    cardEl.classList.add('selected');
    evEvento = evento;
    evPubCache = {};
    sessionStorage.setItem('eventoActivoId', evento.id);

    document.getElementById('evDetailNombre').textContent = evento.nombre;
    const fechaTxt = evento.fechaEvento
        ? new Date(evento.fechaEvento + 'T00:00:00').toLocaleDateString('es-ES', { day:'2-digit', month:'long', year:'numeric' })
        : null;
    document.getElementById('evDetailMeta').textContent = [fechaTxt, evFormatEstado(evento.estado)].filter(Boolean).join(' · ');

    evDetailPlaceholder.style.display = 'none';
    evEventoDetailEl.style.display = 'flex';

    evTabActiva = 'PENDIENTE';
    document.querySelectorAll('#panel-eventos .tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelector('#panel-eventos .tab-btn[data-tab="PENDIENTE"]').classList.add('active');

    await evCargarPublicaciones(evento.id);

    // Arrancar polling si el panel está visible
    if (panelActivo === 'eventos') {
        detenerPolling();
        actualizarSyncDot(true);
        _pollingTimer = setInterval(pollPublicaciones, 4000);
    }
}

async function evCargarPublicaciones(eventoId) {
    evPublicacionesArea.innerHTML = '<div class="area-vacia">Cargando…</div>';
    try {
        const todas = await evFetchJson(`/api/v1/publicaciones?eventoId=${eventoId}`);
        evPubCache = {
            PENDIENTE: todas.filter(p => p.estado === 'PENDIENTE'),
            APROBADA:  todas.filter(p => p.estado === 'APROBADA' || p.estado === 'ENVIADA'),
            RECHAZADA: todas.filter(p => p.estado === 'RECHAZADA')
        };
        document.getElementById('countPendiente').textContent = evPubCache.PENDIENTE.length;
        document.getElementById('countAprobada').textContent  = evPubCache.APROBADA.length;
        document.getElementById('countRechazada').textContent = evPubCache.RECHAZADA.length;
        renderizarTabEv(evTabActiva, new Set());
    } catch (err) {
        evPublicacionesArea.innerHTML = '<div class="area-vacia">Error al cargar publicaciones.</div>';
        mostrarToast('Error: ' + err.message);
    }
}

function renderizarTabEv(tab, nuevasIds) {
    evTabActiva = tab;
    const lista = evPubCache[tab] || [];
    evPublicacionesArea.innerHTML = '';
    if (lista.length === 0) {
        const msg = { PENDIENTE:'No hay publicaciones pendientes', APROBADA:'No hay publicaciones aprobadas ni enviadas', RECHAZADA:'No hay publicaciones rechazadas' };
        const div = document.createElement('div');
        div.className = 'area-vacia';
        div.textContent = msg[tab] || 'Sin publicaciones';
        evPublicacionesArea.appendChild(div);
        return;
    }
    lista.forEach(pub => {
        const card = evCrearTarjetaPub(pub, tab);
        if (nuevasIds && nuevasIds.has(pub.id)) card.classList.add('pub-card-new');
        evPublicacionesArea.appendChild(card);
    });
}

function evFormatFecha(iso) {
    if (!iso) return null;
    const d = new Date(iso), p = n => String(n).padStart(2,'0');
    return `${p(d.getDate())}/${p(d.getMonth()+1)}/${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}`;
}

function evCrearTarjetaPub(pub, tab) {
    const card = document.createElement('div');
    card.className = 'pub-card';
    card.dataset.id = pub.id;

    const plataformaNombre = pub.tipoPublicacion?.plataforma?.nombre || 'Desconocida';
    const tipoNombre       = pub.tipoPublicacion?.nombre || '';
    const headerClass = { 'Facebook':'Facebook','Instagram':'Instagram','YouTube':'YouTube','Blog Web':'Blog' }[plataformaNombre] || 'other';

    const esProgramada = pub.fechaPublicacion && (pub.estado === 'PENDIENTE' || pub.estado === 'APROBADA');
    const partesFecha = [];
    if (pub.fechaGeneracion)  partesFecha.push('Generado: '  + evFormatFecha(pub.fechaGeneracion));
    if (pub.fechaEnvio)       partesFecha.push('Enviado: '   + evFormatFecha(pub.fechaEnvio));
    if (pub.fechaPublicacion && !esProgramada) partesFecha.push('Publicado: ' + evFormatFecha(pub.fechaPublicacion));
    const headerTxt = plataformaNombre + ' · ' + tipoNombre + (partesFecha.length ? ' · ' + partesFecha.join(' | ') : '');

    const header = document.createElement('div');
    header.className = 'pub-card-header pub-header-' + headerClass;
    header.title = headerTxt;
    header.textContent = headerTxt;
    card.appendChild(header);

    if (esProgramada) {
        const chip = document.createElement('div');
        chip.className = 'pub-programado';
        chip.textContent = '📅 Programado: ' + evFormatFecha(pub.fechaPublicacion);
        card.appendChild(chip);
    }

    const texto = document.createElement('div');
    texto.className = 'pub-texto';
    texto.textContent = pub.textoGenerado || '';
    card.appendChild(texto);

    const editable = pub.estado === 'PENDIENTE' || pub.estado === 'APROBADA';
    const adjDiv = document.createElement('div');
    adjDiv.className = 'pub-adjuntos';
    adjDiv.id = 'adjuntos-' + pub.id;
    card.appendChild(adjDiv);
    evCargarAdjuntosPub(pub.id, editable);

    const actionsDiv = document.createElement('div');
    actionsDiv.className = 'pub-card-actions';
    evAdjuntarBotones(actionsDiv, pub);
    card.appendChild(actionsDiv);
    return card;
}

function evAdjuntarBotones(container, pub) {
    function btn(texto, cls, handler) {
        const b = document.createElement('button');
        b.type = 'button';
        b.className = 'btn-accion ' + cls;
        b.textContent = texto;
        b.addEventListener('click', handler);
        return b;
    }
    if (pub.estado === 'PENDIENTE') {
        container.appendChild(btn('✓ Aprobar',       'btn-aprobar',    () => evAprobar(pub.id)));
        container.appendChild(btn('✗ Rechazar',      'btn-rechazar',   () => evRechazar(pub.id)));
        container.appendChild(btn('✎ Pedir cambios', 'btn-cambios',    () => evAbrirModalFeedback(pub.id)));
        container.appendChild(btn('+ Imagen',        'btn-anadir-img', () => evIniciarSubidaAdjunto(pub.id)));
    } else if (pub.estado === 'APROBADA') {
        container.appendChild(btn('↑ Publicar', 'btn-publicar',   () => evAbrirModalPublicar(pub.id, pub.fechaPublicacion)));
        container.appendChild(btn('✗ Rechazar', 'btn-rechazar',   () => evRechazar(pub.id)));
        container.appendChild(btn('+ Imagen',   'btn-anadir-img', () => evIniciarSubidaAdjunto(pub.id)));
    }
}

async function evCargarAdjuntosPub(pubId, editable) {
    if (editable === undefined) {
        const card = document.querySelector(`.pub-card[data-id="${pubId}"]`);
        editable = card ? card.querySelector('.btn-anadir-img') !== null : false;
    }
    try {
        const adj = await evFetchJson(`/api/v1/publicaciones/${pubId}/adjuntos`);
        evRenderAdjuntos(pubId, adj, editable);
    } catch (_) {}
}

function evRenderAdjuntos(pubId, adjuntos, editable) {
    const container = document.getElementById('adjuntos-' + pubId);
    if (!container) return;
    container.innerHTML = '';
    let imgIdx = 0;
    adjuntos.forEach(adj => {
        const url = '/api/v1/ficheros?ruta=' + encodeURIComponent(adj.rutaFichero);
        if (adj.tipoMime && adj.tipoMime.startsWith('image/')) {
            const esPrimero = imgIdx === 0;
            imgIdx++;
            const wrapper = document.createElement('div');
            wrapper.className = 'adjunto-img-wrapper';
            wrapper.dataset.adjuntoId = adj.id;
            if (esPrimero) {
                const badge = document.createElement('span');
                badge.className = 'badge-principal';
                badge.textContent = 'Principal';
                wrapper.appendChild(badge);
            }
            const a = document.createElement('a');
            a.href = url; a.target = '_blank'; a.rel = 'noopener'; a.title = adj.nombreFichero;
            const img = document.createElement('img');
            img.src = url; img.alt = adj.nombreFichero; img.className = 'pub-adjunto-img';
            a.appendChild(img);
            wrapper.appendChild(a);
            if (editable) {
                wrapper.draggable = true;
                const delBtn = document.createElement('button');
                delBtn.type = 'button'; delBtn.className = 'btn-del-adjunto';
                delBtn.title = 'Eliminar adjunto'; delBtn.textContent = '×';
                delBtn.addEventListener('click', async (e) => {
                    e.preventDefault(); e.stopPropagation();
                    try {
                        await fetchDelete(`/api/v1/publicaciones/${pubId}/adjuntos/${adj.id}`);
                        await evCargarAdjuntosPub(pubId, editable);
                    } catch (err) { mostrarToast('Error al eliminar adjunto: ' + err.message); }
                });
                wrapper.appendChild(delBtn);
            }
            container.appendChild(wrapper);
        } else {
            const chip = document.createElement('a');
            chip.href = url; chip.target = '_blank'; chip.rel = 'noopener';
            chip.className = 'ev-adjunto-chip';
            const icono = adj.tipoMime === 'video/mp4' ? '🎬' : adj.tipoMime === 'application/pdf' ? '📄' : '🖼';
            const nombre = document.createElement('span');
            nombre.className = 'ev-adjunto-chip-nombre'; nombre.title = adj.nombreFichero;
            nombre.textContent = icono + ' ' + adj.nombreFichero;
            chip.appendChild(nombre);
            if (editable) {
                const delBtn = document.createElement('button');
                delBtn.type = 'button'; delBtn.className = 'btn-del-adjunto';
                delBtn.title = 'Eliminar adjunto'; delBtn.textContent = '×';
                delBtn.addEventListener('click', async (e) => {
                    e.preventDefault(); e.stopPropagation();
                    try {
                        await fetchDelete(`/api/v1/publicaciones/${pubId}/adjuntos/${adj.id}`);
                        await evCargarAdjuntosPub(pubId, editable);
                    } catch (err) { mostrarToast('Error al eliminar adjunto: ' + err.message); }
                });
                chip.appendChild(delBtn);
            }
            container.appendChild(chip);
        }
    });
    if (editable) evSetupDragDrop(container, pubId, editable);
}

function evSetupDragDrop(container, pubId, editable) {
    let draggedEl = null;
    let placeholder = null;
    function draggableWrappers() { return Array.from(container.querySelectorAll('.adjunto-img-wrapper[data-adjunto-id]')); }
    function createPlaceholder() { const ph = document.createElement('div'); ph.className = 'drag-placeholder'; return ph; }
    function removePlaceholder() { placeholder?.remove(); placeholder = null; }
    function findInsertionPoint(e) {
        const wrappers = draggableWrappers().filter(w => w !== draggedEl);
        if (!wrappers.length) return null;
        let best = null, bestDist = Infinity;
        for (const w of wrappers) {
            const r = w.getBoundingClientRect();
            const cx = r.left + r.width / 2, cy = r.top + r.height / 2;
            const dist = Math.hypot(e.clientX - cx, e.clientY - cy);
            if (dist < bestDist) { bestDist = dist; best = w; }
        }
        if (!best) return null;
        const rect = best.getBoundingClientRect();
        return { wrapper: best, before: e.clientX < rect.left + rect.width / 2 };
    }
    container.addEventListener('dragover', (e) => {
        if (!draggedEl) return;
        e.preventDefault(); e.dataTransfer.dropEffect = 'move';
        if (!placeholder) placeholder = createPlaceholder();
        const point = findInsertionPoint(e);
        if (point) {
            if (point.before) container.insertBefore(placeholder, point.wrapper);
            else container.insertBefore(placeholder, point.wrapper.nextSibling);
        } else container.appendChild(placeholder);
    });
    container.addEventListener('dragleave', (e) => { if (!container.contains(e.relatedTarget)) removePlaceholder(); });
    container.addEventListener('drop', async (e) => {
        e.preventDefault();
        if (!draggedEl || !placeholder) { if (draggedEl) { draggedEl.style.display = ''; draggedEl = null; } removePlaceholder(); return; }
        container.insertBefore(draggedEl, placeholder);
        removePlaceholder(); draggedEl.style.display = ''; draggedEl = null;
        const items = draggableWrappers().map((w, i) => ({ adjuntoId: w.dataset.adjuntoId, orden: i }));
        evActualizarBadgePrincipal(container);
        try {
            await evFetchJson(`/api/v1/publicaciones/${pubId}/adjuntos/reordenar`, {
                method: 'PATCH', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ items })
            });
        } catch (err) { mostrarToast('Error al reordenar: ' + err.message); await evCargarAdjuntosPub(pubId, editable); }
    });
    draggableWrappers().forEach(wrapper => {
        wrapper.addEventListener('dragstart', (e) => {
            draggedEl = wrapper; e.dataTransfer.effectAllowed = 'move';
            const img = wrapper.querySelector('img.pub-adjunto-img');
            if (img) e.dataTransfer.setDragImage(img, img.offsetWidth / 2, img.offsetHeight / 2);
            requestAnimationFrame(() => { if (draggedEl === wrapper) wrapper.style.display = 'none'; });
        });
        wrapper.addEventListener('dragend', () => { if (draggedEl) { draggedEl.style.display = ''; draggedEl = null; } removePlaceholder(); });
    });
}

function evActualizarBadgePrincipal(container) {
    container.querySelectorAll('.adjunto-img-wrapper[data-adjunto-id]').forEach((w, i) => {
        const existing = w.querySelector('.badge-principal');
        if (i === 0) {
            if (!existing) { const badge = document.createElement('span'); badge.className = 'badge-principal'; badge.textContent = 'Principal'; w.insertBefore(badge, w.firstChild); }
        } else existing?.remove();
    });
}

function evCrearPlaceholderCarga(file) {
    const wrapper = document.createElement('div');
    wrapper.className = 'adjunto-img-wrapper';
    const inner = document.createElement('div');
    inner.className = 'adjunto-cargando';
    const img = document.createElement('img');
    const objectUrl = URL.createObjectURL(file);
    img.src = objectUrl; img.alt = file.name;
    inner.appendChild(img);
    const overlay = document.createElement('div');
    overlay.className = 'cargando-overlay';
    const spinner = document.createElement('div');
    spinner.className = 'spinner-ring';
    overlay.appendChild(spinner);
    const txt = document.createElement('span');
    txt.className = 'cargando-txt'; txt.textContent = 'Analizando…';
    overlay.appendChild(txt);
    inner.appendChild(overlay);
    wrapper.appendChild(inner);
    return { wrapper, objectUrl };
}

function evIniciarSubidaAdjunto(pubId) {
    evAdjuntoUploadId = pubId;
    evFileInputAdjunto.value = '';
    evFileInputAdjunto.click();
}

evFileInputAdjunto.addEventListener('change', async () => {
    const files = Array.from(evFileInputAdjunto.files);
    if (!files.length || !evAdjuntoUploadId || !evEvento) return;
    const pubId = evAdjuntoUploadId;
    evAdjuntoUploadId = null;
    const container = document.getElementById('adjuntos-' + pubId);
    if (!container) return;
    const placeholders = files.map(file => {
        const ph = evCrearPlaceholderCarga(file);
        container.appendChild(ph.wrapper);
        return ph;
    });
    const errores = [];
    for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const { wrapper: phWrapper, objectUrl } = placeholders[i];
        try {
            const formData = new FormData();
            formData.append('fichero', file);
            const adjunto = await evFetchMultipart(`/api/v1/eventos/${evEvento.id}/adjuntos`, formData);
            await evFetchJson(`/api/v1/adjuntos/${adjunto.id}/publicacion`, {
                method: 'PUT', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ publicacionId: pubId })
            });
        } catch (err) { errores.push(`"${file.name}": ${err.message}`); }
        URL.revokeObjectURL(objectUrl);
        phWrapper.remove();
        await evCargarAdjuntosPub(pubId, true);
        const cont2 = document.getElementById('adjuntos-' + pubId);
        if (cont2) {
            for (let j = i + 1; j < placeholders.length; j++) cont2.appendChild(placeholders[j].wrapper);
        }
    }
    if (errores.length > 0) mostrarToast('Errores al subir: ' + errores.join(' | '));
    else mostrarToast(files.length === 1 ? `"${files[0].name}" añadida correctamente` : `${files.length} imágenes añadidas correctamente`);
});

async function evAprobar(id) {
    try { await evFetchJson('/api/v1/publicaciones/' + id + '/aprobar', { method: 'PATCH' }); mostrarToast('Publicación aprobada'); await evCargarPublicaciones(evEvento.id); }
    catch (err) { mostrarToast('Error: ' + err.message); }
}
async function evRechazar(id) {
    try { await evFetchJson('/api/v1/publicaciones/' + id + '/rechazar', { method: 'PATCH' }); mostrarToast('Publicación rechazada'); await evCargarPublicaciones(evEvento.id); }
    catch (err) { mostrarToast('Error: ' + err.message); }
}
let evPublicarPubId      = null;
let evFechasRecomendadas = [];

const evDiasSemanaMap = { MONDAY:1,TUESDAY:2,WEDNESDAY:3,THURSDAY:4,FRIDAY:5,SATURDAY:6,SUNDAY:0 };
const evDiasCortos    = ['Dom','Lun','Mar','Mié','Jue','Vie','Sáb'];
const evMesesCortos   = ['ene','feb','mar','abr','may','jun','jul','ago','sep','oct','nov','dic'];

function evFormatChip(fecha) {
    return evDiasCortos[fecha.getDay()] + ' ' + fecha.getDate()
        + ' ' + evMesesCortos[fecha.getMonth()]
        + ' · ' + String(fecha.getHours()).padStart(2,'0') + ':00';
}
function evToDatetimeLocalValue(fecha) {
    const p = n => String(n).padStart(2,'0');
    return fecha.getFullYear() + '-' + p(fecha.getMonth()+1) + '-' + p(fecha.getDate())
        + 'T' + p(fecha.getHours()) + ':' + p(fecha.getMinutes());
}
function evCalcFechasRecomendadas(franjas) {
    const ahora = new Date();
    const res = [];
    for (const f of franjas) {
        const c = new Date();
        c.setHours(f.hora, 0, 0, 0);
        let d = (evDiasSemanaMap[f.diaSemana] - c.getDay() + 7) % 7;
        if (d === 0 && c <= ahora) d = 7;
        c.setDate(c.getDate() + d);
        res.push(c);
    }
    res.sort((a, b) => a - b);
    return res.filter((f, i, arr) => i === 0 || f.getTime() !== arr[i-1].getTime()).slice(0, 3);
}

async function evAbrirModalPublicar(pubId, fechaPublicacionActual) {
    console.log("abrirModalPublicar llamada", pubId);
    const modal = document.getElementById('ev-modalPublicar');
    if (!modal) return;
    modal.style.display = 'flex';

    evPublicarPubId = pubId;
    evFechasRecomendadas = [];

    const inputFecha = document.getElementById('ev-inputFechaPublicacion');
    const checkInm   = document.getElementById('ev-checkInmediato');
    const seccChips  = document.getElementById('ev-seccFechasRecomendadas');
    const chipsContn = document.getElementById('ev-fechasChipsContainer');

    if (checkInm)   checkInm.checked = false;
    if (inputFecha) { inputFecha.disabled = false; inputFecha.style.opacity = '1'; }
    if (seccChips)  { seccChips.style.display = 'none'; seccChips.style.opacity = '1'; seccChips.style.pointerEvents = ''; }
    if (chipsContn) chipsContn.innerHTML = '';
    const btnConfEv = document.getElementById('ev-confirmarPublicar');
    if (btnConfEv) btnConfEv.disabled = false;

    if (inputFecha) {
        inputFecha.value = fechaPublicacionActual
            ? evToDatetimeLocalValue(new Date(fechaPublicacionActual))
            : '';
    }

    if (clienteIdActivo && chipsContn && seccChips && inputFecha) {
        try {
            const franjas = await evFetchJson('/api/v1/clientes/' + clienteIdActivo + '/insights');
            if (franjas && franjas.length > 0) {
                evFechasRecomendadas = evCalcFechasRecomendadas(franjas);
                if (evFechasRecomendadas.length > 0) {
                    if (!fechaPublicacionActual) inputFecha.value = evToDatetimeLocalValue(evFechasRecomendadas[0]);
                    evFechasRecomendadas.forEach((fecha, idx) => {
                        const chip = document.createElement('button');
                        chip.type = 'button';
                        chip.style.cssText = 'background:#f0f2f5;border:1px solid #dde0e4;border-radius:14px;padding:5px 13px;font-size:12px;font-weight:600;color:#1c1e21;cursor:pointer;white-space:nowrap;font-family:inherit';
                        chip.textContent = evFormatChip(fecha);
                        if (idx === 0 && !fechaPublicacionActual) chip.style.background = '#d4ccff';
                        chip.addEventListener('click', () => {
                            chipsContn.querySelectorAll('button').forEach(c => c.style.background = '#f0f2f5');
                            chip.style.background = '#d4ccff';
                            inputFecha.value = evToDatetimeLocalValue(fecha);
                        });
                        chipsContn.appendChild(chip);
                    });
                    seccChips.style.display = 'block';
                }
            }
        } catch (_) { /* Sin insights */ }
    }
}

document.getElementById('ev-checkInmediato').addEventListener('change', function() {
    const inputFecha = document.getElementById('ev-inputFechaPublicacion');
    const seccChips  = document.getElementById('ev-seccFechasRecomendadas');
    if (inputFecha) { inputFecha.disabled = this.checked; inputFecha.style.opacity = this.checked ? '.4' : '1'; }
    if (seccChips)  { seccChips.style.opacity = this.checked ? '.4' : '1'; seccChips.style.pointerEvents = this.checked ? 'none' : ''; }
});

document.getElementById('ev-cancelarPublicar').addEventListener('click', () => {
    document.getElementById('ev-modalPublicar').style.display = 'none';
    evPublicarPubId = null;
});
document.getElementById('ev-modalPublicar').addEventListener('click', e => {
    if (e.target === document.getElementById('ev-modalPublicar')) {
        document.getElementById('ev-modalPublicar').style.display = 'none';
        evPublicarPubId = null;
    }
});
document.getElementById('ev-confirmarPublicar').addEventListener('click', async () => {
    if (!evPublicarPubId) return;
    const checkInm   = document.getElementById('ev-checkInmediato');
    const inputFecha = document.getElementById('ev-inputFechaPublicacion');
    try {
        let options;
        if ((checkInm && checkInm.checked) || !inputFecha || !inputFecha.value) {
            options = { method: 'POST' };
        } else {
            options = { method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ fechaPublicacion: inputFecha.value + ':00' }) };
        }
        await evFetchJson('/api/v1/publicaciones/' + evPublicarPubId + '/publicar', options);
        document.getElementById('ev-modalPublicar').style.display = 'none';
        evPublicarPubId = null;
        mostrarToast('Publicación enviada');
        await evCargarPublicaciones(evEvento.id);
    } catch (err) { mostrarToast('Error: ' + err.message); }
});

function evAbrirModalFeedback(pubId) {
    evFeedbackPubId = pubId;
    document.getElementById('ev-feedbackTexto').value = '';
    document.getElementById('ev-modalFeedback').style.display = 'flex';
    document.getElementById('ev-feedbackTexto').focus();
}

document.getElementById('ev-cancelarFeedbackModal').addEventListener('click', () => {
    document.getElementById('ev-modalFeedback').style.display = 'none';
    evFeedbackPubId = null;
});
document.getElementById('ev-modalFeedback').addEventListener('click', e => {
    if (e.target === document.getElementById('ev-modalFeedback')) {
        document.getElementById('ev-modalFeedback').style.display = 'none';
        evFeedbackPubId = null;
    }
});
document.getElementById('ev-confirmarFeedbackModal').addEventListener('click', async () => {
    const feedback = document.getElementById('ev-feedbackTexto').value.trim();
    if (!feedback) { mostrarToast('Escribe el feedback antes de continuar'); return; }
    if (!evFeedbackPubId) return;
    try {
        const pub = await evFetchJson('/api/v1/publicaciones/' + evFeedbackPubId + '/solicitar-cambios', {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ feedback })
        });
        document.getElementById('ev-modalFeedback').style.display = 'none';
        const plataforma = pub.tipoPublicacion?.plataforma?.nombre || 'Plataforma';
        const tipo       = pub.tipoPublicacion?.nombre || 'Publicación';
        const mensaje    = 'Modifica la publicación de ' + plataforma + ' - ' + tipo + ' (ID: ' + pub.id + '): ' + feedback;
        // SPA: cambiar a panel chat y auto-enviar
        abrirChatConFeedback(pub.eventoId, mensaje);
    } catch (err) { mostrarToast('Error: ' + err.message); }
});

function abrirChatConFeedback(eventoId, mensaje) {
    navegarA('chat');
    // Si el chat ya tiene este evento seleccionado, enviar directamente
    if (chatEventoId === eventoId) {
        mensajeInput.value = mensaje;
        setTimeout(() => enviarMensaje(), 50);
    } else {
        // Seleccionar el evento en el chat y dejar mensajePendienteAutoEnvio
        mensajePendienteAutoEnvio = mensaje;
        if (eventoSelect.querySelector(`option[value="${eventoId}"]`)) {
            eventoSelect.value = eventoId;
            eventoSelect.dispatchEvent(new Event('change'));
        }
    }
}

// Tabs del panel eventos
document.querySelectorAll('#panel-eventos .tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('#panel-eventos .tab-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        renderizarTabEv(btn.dataset.tab, new Set());
    });
});

// Botón "Abrir chat"
document.getElementById('evBtnAbrirChat').addEventListener('click', () => {
    if (!evEvento) return;
    navegarA('chat');
    if (eventoSelect.querySelector(`option[value="${evEvento.id}"]`)) {
        eventoSelect.value = evEvento.id;
        if (chatEventoId !== evEvento.id) eventoSelect.dispatchEvent(new Event('change'));
    }
});

// Modal nuevo evento (eventos)
evBtnNuevo.addEventListener('click', () => {
    document.getElementById('ev-eventoNombre').value = '';
    document.getElementById('ev-eventoFecha').value = '';
    document.getElementById('ev-eventoEstado').value = 'ACTIVO';
    document.getElementById('ev-eventoDescripcion').value = '';
    document.getElementById('ev-modalNuevoEvento').style.display = 'flex';
    document.getElementById('ev-eventoNombre').focus();
});
document.getElementById('ev-cancelarEvento').addEventListener('click', () => document.getElementById('ev-modalNuevoEvento').style.display = 'none');
document.getElementById('ev-modalNuevoEvento').addEventListener('click', e => { if (e.target === document.getElementById('ev-modalNuevoEvento')) document.getElementById('ev-modalNuevoEvento').style.display = 'none'; });
document.getElementById('ev-crearEvento').addEventListener('click', async () => {
    const nombre = document.getElementById('ev-eventoNombre').value.trim();
    if (!nombre) { mostrarToast('El nombre del evento es obligatorio'); return; }
    const body = { clienteId: clienteIdActivo, nombre, estado: document.getElementById('ev-eventoEstado').value };
    const fecha = document.getElementById('ev-eventoFecha').value;
    const desc  = document.getElementById('ev-eventoDescripcion').value.trim();
    if (fecha) body.fechaEvento = fecha;
    if (desc)  body.descripcion = desc;
    try {
        const evento = await evFetchJson('/api/v1/eventos', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
        document.getElementById('ev-modalNuevoEvento').style.display = 'none';
        mostrarToast('Evento "' + evento.nombre + '" creado');
        evTodosEventos.unshift(evento);
        evAplicarFiltro();
        const card = evEventosList.querySelector('[data-id="' + evento.id + '"]');
        if (card) await evSeleccionarEvento(evento, card);
    } catch (err) { mostrarToast('Error al crear el evento: ' + err.message); }
});

// Modal editar evento (eventos)
document.getElementById('evBtnEditarEvento').addEventListener('click', () => {
    if (!evEvento) return;
    document.getElementById('ev-editNombre').value      = evEvento.nombre || '';
    document.getElementById('ev-editFecha').value       = evEvento.fechaEvento || '';
    document.getElementById('ev-editEstado').value      = evEvento.estado || 'ACTIVO';
    document.getElementById('ev-editDescripcion').value = evEvento.descripcion || '';
    document.getElementById('ev-modalEditarEvento').style.display = 'flex';
    document.getElementById('ev-editNombre').focus();
});
document.getElementById('ev-cancelarEditar').addEventListener('click', () => document.getElementById('ev-modalEditarEvento').style.display = 'none');
document.getElementById('ev-modalEditarEvento').addEventListener('click', e => { if (e.target === document.getElementById('ev-modalEditarEvento')) document.getElementById('ev-modalEditarEvento').style.display = 'none'; });
document.getElementById('ev-guardarEditar').addEventListener('click', async () => {
    const nombre = document.getElementById('ev-editNombre').value.trim();
    if (!nombre) { mostrarToast('El nombre del evento es obligatorio'); return; }
    if (!evEvento) return;
    const body = { clienteId: clienteIdActivo, nombre, estado: document.getElementById('ev-editEstado').value };
    const fecha = document.getElementById('ev-editFecha').value;
    const desc  = document.getElementById('ev-editDescripcion').value.trim();
    if (fecha) body.fechaEvento = fecha;
    if (desc)  body.descripcion = desc;
    try {
        const actualizado = await evFetchJson('/api/v1/eventos/' + evEvento.id, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
        document.getElementById('ev-modalEditarEvento').style.display = 'none';
        const idx = evTodosEventos.findIndex(e => e.id === actualizado.id);
        if (idx >= 0) evTodosEventos[idx] = actualizado;
        evEvento = actualizado;
        evAplicarFiltro();
        document.getElementById('evDetailNombre').textContent = actualizado.nombre;
        const fechaTxt = actualizado.fechaEvento
            ? new Date(actualizado.fechaEvento + 'T00:00:00').toLocaleDateString('es-ES', { day:'2-digit', month:'long', year:'numeric' })
            : null;
        document.getElementById('evDetailMeta').textContent = [fechaTxt, evFormatEstado(actualizado.estado)].filter(Boolean).join(' · ');
        mostrarToast('Evento actualizado correctamente');
    } catch (err) { mostrarToast('Error al actualizar el evento: ' + err.message); }
});

function evOcultarDetalle() {
    evDetailPlaceholder.style.display = 'flex';
    evEventoDetailEl.style.display = 'none';
}

// ═══════════════════════ LISTENERS GLOBALES ═══════════════════════════════════
document.addEventListener('clienteGlobalChanged', e => {
    const id = e.detail.clienteId;
    clienteIdActivo = id || null;
    chatOnClienteChanged(id);
    evOnClienteChanged(id);
});

// ═══════════════════════ UTILIDADES ═══════════════════════════════════════════
async function fetchJson(url, options = {}) {
    const res = await fetch(url, options);
    if (!res.ok) { const err = await res.json().catch(() => ({ mensaje: res.statusText })); throw new Error(err.mensaje || res.statusText); }
    return res.json();
}
const evFetchJson = fetchJson;

async function fetchMultipart(url, formData) {
    const res = await fetch(url, { method: 'POST', body: formData });
    if (!res.ok) { const err = await res.json().catch(() => ({ mensaje: res.statusText })); throw new Error(err.mensaje || res.statusText); }
    return res.json();
}
const evFetchMultipart = fetchMultipart;

async function fetchDelete(url) {
    const res = await fetch(url, { method: 'DELETE' });
    if (!res.ok && res.status !== 204) { const err = await res.json().catch(() => ({ mensaje: res.statusText })); throw new Error(err.mensaje || res.statusText); }
}

function esPdf(mime)    { return mime === 'application/pdf'; }
function esImagen(mime) { return !!mime && mime.startsWith('image/'); }
function esVideo(mime)  { return mime === 'video/mp4'; }

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
function formatearHora(isoString) {
    try { return new Date(isoString).toLocaleTimeString('es-ES', { hour:'2-digit', minute:'2-digit' }); } catch { return ''; }
}
function mostrarToast(msg) {
    const toast = document.getElementById('toast');
    toast.textContent = msg;
    toast.classList.add('visible');
    setTimeout(() => toast.classList.remove('visible'), 3500);
}

// ═══════════════════════ INICIALIZACIÓN ═══════════════════════════════════════
(function init() {
    const initialPanel = document.getElementById('spa-root').dataset.initialPanel || 'chat';
    navegarA(initialPanel);
    // El layout dispara clienteGlobalChanged antes de que este script cargue,
    // así que inicializamos manualmente si ya hay cliente guardado.
    if (clienteIdActivo) {
        chatOnClienteChanged(clienteIdActivo);
        evOnClienteChanged(clienteIdActivo);
    }
})();
