package paf_grp_k.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * REST-Controller zum sicheren Hochladen von Dateien.
 *
 * <p>Diese Klasse stellt einen Endpunkt zum Upload von Profilbildern bereit.
 * Die Dateien werden serverseitig validiert, sicher im Dateisystem gespeichert
 * und anschließend über eine öffentliche URL bereitgestellt.</p>
 *
 * <p>Der Controller implementiert mehrere Sicherheitsmaßnahmen, u. a.:</p>
 * <ul>
 *     <li>Begrenzung der maximalen Dateigröße</li>
 *     <li>Whitelist für erlaubte MIME-Typen</li>
 *     <li>Schutz vor Pfad-Traversal-Angriffen</li>
 *     <li>Zufällige Dateinamen zur Kollisionsvermeidung</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    /**
     * Zielverzeichnis für hochgeladene Dateien.
     *
     * <p>Das Verzeichnis {@code uploads/} liegt relativ zum Projekt-Root
     * und wird bei Bedarf automatisch erstellt.</p>
     */
    private static final Path UPLOAD_DIR =
            Paths.get("uploads").toAbsolutePath().normalize();

    /**
     * Maximale erlaubte Dateigröße (10 MB).
     */
    private static final long MAX_SIZE = 10L * 1024 * 1024;

    /**
     * Erlaubte MIME-Typen für Bilddateien.
     *
     * <p>Die Validierung erfolgt anhand des Content-Types
     * und nicht ausschließlich über die Dateiendung.</p>
     */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/gif",
            "image/webp"
    );

    /**
     * Fallback-Zuordnung von MIME-Typen zu Dateiendungen.
     *
     * <p>Wird verwendet, wenn der Originaldateiname keine
     * oder keine gültige Endung enthält.</p>
     */
    private static final Map<String, String> EXT_BY_TYPE = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/jpg", ".jpg",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    /**
     * Lädt ein Profilbild hoch und speichert es serverseitig.
     *
     * <p>Der Endpunkt akzeptiert Multipart-Form-Data und führt
     * mehrere Validierungsschritte durch, bevor die Datei gespeichert wird.</p>
     *
     * <p>Mögliche HTTP-Antworten:</p>
     * <ul>
     *     <li>{@code 200 OK} – Upload erfolgreich, Rückgabe der öffentlichen URL</li>
     *     <li>{@code 400 Bad Request} – ungültige Datei, Größe oder Typ</li>
     *     <li>{@code 500 Internal Server Error} – Fehler beim Speichern</li>
     * </ul>
     *
     * @param file hochgeladene Bilddatei
     * @return {@link ResponseEntity} mit der URL der gespeicherten Datei
     */
    @PostMapping(
            value = "/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> uploadProfileImage(
            @RequestParam("file") MultipartFile file) {

        try {
            // Validierung: Datei vorhanden
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Keine Datei erhalten.");
            }

            // Validierung: Dateigröße
            if (file.getSize() > MAX_SIZE) {
                return ResponseEntity.badRequest()
                        .body("Datei zu groß (max. 10MB).");
            }

            // Validierung: MIME-Typ
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
                return ResponseEntity.badRequest()
                        .body("Ungültiger Bildtyp: " + contentType);
            }

            // Zielverzeichnis erstellen (falls nicht vorhanden)
            Files.createDirectories(UPLOAD_DIR);

            // Dateiendung bestimmen
            String extension = resolveExtension(
                    file.getOriginalFilename(), contentType);

            // Zufälliger Dateiname zur Vermeidung von Kollisionen
            String filename = UUID.randomUUID() + extension;

            Path target = UPLOAD_DIR.resolve(filename).normalize();

            // Sicherheitsprüfung: Pfad-Traversal verhindern
            if (!target.startsWith(UPLOAD_DIR)) {
                return ResponseEntity.badRequest()
                        .body("Ungültiger Dateipfad.");
            }

            // Datei speichern
            Files.copy(
                    file.getInputStream(),
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );

            // Öffentliche URL zurückgeben
            return ResponseEntity.ok("/uploads/" + filename);

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Upload fehlgeschlagen: " + e.getMessage());
        }
    }

    /**
     * Ermittelt eine sichere Dateiendung für die hochgeladene Datei.
     *
     * <p>Priorität:</p>
     * <ol>
     *     <li>Dateiendung aus dem Originaldateinamen</li>
     *     <li>Fallback anhand des MIME-Typs</li>
     * </ol>
     *
     * @param originalName ursprünglicher Dateiname
     * @param contentType MIME-Typ der Datei
     * @return Dateiendung inklusive Punkt (z. B. {@code ".jpg"})
     */
    private static String resolveExtension(
            String originalName, String contentType) {

        String clean = StringUtils.cleanPath(
                originalName == null ? "" : originalName);

        int dot = clean.lastIndexOf('.');
        if (dot > 0 && dot < clean.length() - 1) {
            return clean.substring(dot).toLowerCase();
        }

        return EXT_BY_TYPE.getOrDefault(contentType, "");
    }
}
