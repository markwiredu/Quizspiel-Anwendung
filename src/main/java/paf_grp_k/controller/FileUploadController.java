package paf_grp_k.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    // ✅ Ordner im Projektverzeichnis (neben build.gradle)
    private static final Path UPLOAD_DIR = Paths.get("uploads").toAbsolutePath().normalize();

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp"
    );

    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Keine Datei erhalten.");
            }

            // ✅ optional: Server-seitiges Limit (z.B. 10MB)
            if (file.getSize() > 10L * 1024 * 1024) {
                return ResponseEntity.badRequest().body("Datei zu groß (max. 10MB).");
            }

            // ✅ MIME-Check
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
                return ResponseEntity.badRequest().body("Ungültiger Dateityp: " + contentType);
            }

            Files.createDirectories(UPLOAD_DIR);

            String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
            String ext = "";

            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot < originalName.length() - 1) {
                ext = originalName.substring(dot).toLowerCase();
            }

            // Fallback Extension falls fehlt
            if (ext.isBlank()) {
                ext = switch (contentType) {
                    case "image/png" -> ".png";
                    case "image/jpeg" -> ".jpg";
                    case "image/gif" -> ".gif";
                    case "image/webp" -> ".webp";
                    default -> "";
                };
            }

            String newFileName = UUID.randomUUID() + ext;
            Path target = UPLOAD_DIR.resolve(newFileName);

            // ✅ überschreiben falls zufällig vorhanden
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // URL die im Browser funktioniert
            return ResponseEntity.ok("/uploads/" + newFileName);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Upload fehlgeschlagen: " + e.getMessage());
        }
    }
}
