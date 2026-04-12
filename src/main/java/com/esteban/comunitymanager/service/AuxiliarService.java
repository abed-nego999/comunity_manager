package com.esteban.comunitymanager.service;

import com.esteban.comunitymanager.dto.response.*;
import com.esteban.comunitymanager.model.TipoPublicacion;
import com.esteban.comunitymanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Acceso de solo lectura a las tablas auxiliares de catálogo.
 * Estos datos se insertan al arrancar desde data.sql y no cambian en producción.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuxiliarService {

    private final PlataformaRepository plataformaRepository;
    private final TipoPublicacionRepository tipoPublicacionRepository;
    private final TipoAdjuntoRepository tipoAdjuntoRepository;
    private final RolConversacionRepository rolConversacionRepository;

    public List<PlataformaResponse> listarPlataformas() {
        return plataformaRepository.findAll().stream()
                .map(PlataformaResponse::from)
                .toList();
    }

    public List<TipoPublicacionResponse> listarTiposPublicacion(UUID plataformaId) {
        List<TipoPublicacion> tipos = (plataformaId != null)
                ? tipoPublicacionRepository.findByPlataformaId(plataformaId)
                : tipoPublicacionRepository.findAll();
        return tipos.stream()
                .map(TipoPublicacionResponse::from)
                .toList();
    }

    public List<TipoAdjuntoResponse> listarTiposAdjunto() {
        return tipoAdjuntoRepository.findAll().stream()
                .map(TipoAdjuntoResponse::from)
                .toList();
    }

    public List<RolConversacionResponse> listarRolesConversacion() {
        return rolConversacionRepository.findAll().stream()
                .map(RolConversacionResponse::from)
                .toList();
    }
}
