package com.esteban.comunitymanager.controller;

import com.esteban.comunitymanager.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador de la vista de eventos.
 * Renderiza el panel de gestión de eventos con listado, detalle y acciones de publicación.
 */
@Controller
@RequiredArgsConstructor
public class EventosViewController {

    private final ClienteService clienteService;

    @GetMapping("/eventos")
    public String eventos(Model model) {
        model.addAttribute("clientes", clienteService.listarClientes());
        model.addAttribute("initialPanel", "eventos");
        return "index";
    }
}
