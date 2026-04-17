package com.esteban.comunitymanager.controller;

import com.esteban.comunitymanager.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador de la vista de chat.
 * Renderiza la interfaz conversacional donde el usuario interactúa con Claude
 * para generar publicaciones en redes sociales.
 */
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ClienteService clienteService;

    @GetMapping("/chat")
    public String chat(Model model) {
        model.addAttribute("clientes", clienteService.listarClientes());
        model.addAttribute("initialPanel", "chat");
        return "index";
    }
}
