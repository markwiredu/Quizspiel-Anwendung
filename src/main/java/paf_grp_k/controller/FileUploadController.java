package paf_grp_k.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * REST-Controller zum Hochladen von Dateien über HTTP-Endpunkte.
 *
 * <p>Aktuell unterstützt dieser Controller das Hochladen eines Profilbildes.
 * Die Datei wird im lokalen Dateisystem unter
 * {@code src/main/resources/static/uploads/} abgelegt und kann anschließend
 * über die zurückgegebene URL vom Frontend abgerufen werden.</p>
 */
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    /**
     * Lokales Verzeichnis zum Speichern hochgeladener Dateien.
     */
    private final String UPLOAD_DIR = "src/main/resources/static/uploads/";

    /**
     * Endpunkt zum Hochladen eines Profilbildes.
     *
     * <p>Akzeptiert eine Multipart-Datei, erzeugt einen eindeutigen Dateinamen
     * (UUID + Dateiendung), speichert die Datei im Upload-Verzeichnis und gibt
     * eine URL zurück, die vom Frontend genutzt werden kann.</p>
     *
     * @param file Die hochgeladene Bilddatei im Multipart-Format.
     * @return {@link ResponseEntity} mit der URL zur gespeicherten Datei
     *         oder einer Fehlermeldung im Falle eines Fehlers.
     */
    @PostMapping("/profile-image")
    public ResponseEntity<String> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        try {
            // Upload-Verzeichnis erstellen falls nicht vorhanden
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Dateiname generieren (UUID + Originalname)
            String originalFileName = file.getOriginalFilename();
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String newFileName = UUID.randomUUID().toString() + fileExtension;

            // Datei speichern
            Path filePath = uploadPath.resolve(newFileName);
            Files.copy(file.getInputStream(), filePath);

            // URL für Frontend zurückgeben
            String fileUrl = "/uploads/" + newFileName;
            return ResponseEntity.ok(fileUrl);

        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Upload fehlgeschlagen: " + e.getMessage());
        }
    }
}
