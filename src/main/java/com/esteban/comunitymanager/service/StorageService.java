package com.esteban.comunitymanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.UUID;

/**
 * Gestiona el almacenamiento de ficheros multimedia en disco.
 *
 * Estructura de carpetas:
 * {storage.base-path}/clientes/{id}_{nombre}/eventos/{id}_{nombre}/adjuntos|generados/
 *
 * Los nombres de carpeta se sanitizan: minúsculas, sin tildes, sin emojis, espacios a guiones.
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    @Value("${storage.base-path:./storage}")
    private String basePath;

    // ── Resolución de rutas ───────────────────────────────────────────────────

    public Path resolverRutaAdjunto(UUID idCliente, String nombreCliente,
                                    UUID idEvento, String nombreEvento,
                                    String nombreFichero) {
        return resolverRuta(idCliente, nombreCliente, idEvento, nombreEvento, "adjuntos", nombreFichero);
    }

    public Path resolverRutaGenerado(UUID idCliente, String nombreCliente,
                                     UUID idEvento, String nombreEvento,
                                     String nombreFichero) {
        return resolverRuta(idCliente, nombreCliente, idEvento, nombreEvento, "generados", nombreFichero);
    }

    private Path resolverRuta(UUID idCliente, String nombreCliente,
                               UUID idEvento, String nombreEvento,
                               String subcarpeta, String nombreFichero) {
        String dirCliente = idCliente + "_" + sanitizarNombre(nombreCliente);
        String dirEvento  = idEvento  + "_" + sanitizarNombre(nombreEvento);
        return Paths.get(basePath, "clientes", dirCliente, "eventos", dirEvento, subcarpeta, nombreFichero);
    }

    // ── Operaciones de fichero ────────────────────────────────────────────────

    /**
     * Guarda un MultipartFile en la ruta indicada, creando los directorios necesarios.
     *
     * @return ruta relativa al basePath, para almacenar en BBDD como ruta_fichero
     */
    public String guardarFichero(MultipartFile fichero, Path destino) throws IOException {
        Files.createDirectories(destino.getParent());
        Files.copy(fichero.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Fichero guardado en: {}", destino);
        return Paths.get(basePath).relativize(destino).toString().replace("\\", "/");
    }

    /**
     * Elimina el fichero indicado por su ruta relativa al basePath.
     * No lanza excepción si el fichero no existe.
     */
    public void eliminarFichero(String rutaRelativa) {
        Path ruta = Paths.get(basePath, rutaRelativa);
        try {
            Files.deleteIfExists(ruta);
            log.debug("Fichero eliminado: {}", ruta);
        } catch (IOException e) {
            log.warn("No se pudo eliminar el fichero {}: {}", ruta, e.getMessage());
        }
    }

    // ── Sanitización ─────────────────────────────────────────────────────────

    /**
     * Transforma un nombre en una cadena segura para usar como nombre de directorio:
     * minúsculas, sin tildes, sin emojis, espacios y caracteres especiales a guiones.
     *
     * Ejemplo: "Milonga de Verano 🎶" → "milonga-de-verano"
     */
    public String sanitizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "sin-nombre";
        }
        // Eliminar tildes y diacríticos
        String sinTildes = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "");
        // Minúsculas
        String lower = sinTildes.toLowerCase();
        // Eliminar caracteres no ASCII (emojis, caracteres especiales)
        String sinEmojis = lower.replaceAll("[^\\x00-\\x7F]", "");
        // Sustituir secuencias de caracteres no alfanuméricos por un guion
        String conGuiones = sinEmojis.replaceAll("[^a-z0-9]+", "-");
        // Eliminar guiones al inicio y al final
        return conGuiones.replaceAll("^-+|-+$", "");
    }
}
