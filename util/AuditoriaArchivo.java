package bioroute.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Maneja el registro de eventos en archivo de auditoria.
 * Cumple con el uso opcional de archivos solicitado por la consigna.
 */
public final class AuditoriaArchivo {

    private static final Path RUTA_LOG = Paths.get("logs", "auditoria-bioroute.log");
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AuditoriaArchivo() { }

    /**
     * Registra un evento con timestamp en el archivo de auditoria.
     */
    public static void registrar(String evento) {
        try {
            if (!Files.exists(RUTA_LOG.getParent())) {
                Files.createDirectories(RUTA_LOG.getParent());
            }
            String linea = LocalDateTime.now().format(FORMATO) + " - " + evento + System.lineSeparator();
            Files.write(RUTA_LOG, linea.getBytes(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // No interrumpimos el flujo principal si falla la auditoria
            System.err.println("Advertencia: no se pudo registrar evento de auditoria: " + e.getMessage());
        }
    }

    /**
     * Lee todos los eventos auditados (uso opcional para futuros reportes).
     */
    public static List<String> leerEventos() {
        try {
            if (!Files.exists(RUTA_LOG)) return new ArrayList<>();
            return Files.readAllLines(RUTA_LOG);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
}
