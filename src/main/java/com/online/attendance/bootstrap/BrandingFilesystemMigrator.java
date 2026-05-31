package com.online.attendance.bootstrap;

import com.online.attendance.system.SystemBranding;
import com.online.attendance.system.SystemBrandingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

/**
 * Imports legacy on-disk branding files into PostgreSQL so logos survive redeploys.
 */
@Component
@Order(1)
public class BrandingFilesystemMigrator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BrandingFilesystemMigrator.class);
    private static final String BRANDING_ID = "SYSTEM";

    private final SystemBrandingRepository brandingRepository;

    public BrandingFilesystemMigrator(SystemBrandingRepository brandingRepository) {
        this.brandingRepository = brandingRepository;
    }

    @Override
    public void run(String... args) {
        SystemBranding branding = brandingRepository.findById(BRANDING_ID).orElse(null);
        if (branding == null) {
            return;
        }

        boolean changed = false;

        if (!hasBytes(branding.getLogoBytes())) {
            byte[] fromPath = readIfExists(branding.getLogoPath());
            if (fromPath == null) {
                fromPath = readFirstFile(Paths.get("uploads", "system-logo"));
            }
            if (fromPath != null) {
                branding.setLogoBytes(fromPath);
                branding.setLogoContentType(guessContentType(branding.getLogoPath(), fromPath));
                branding.setLogoUrl("/api/system/logo/image");
                branding.setLogoPath(null);
                changed = true;
                log.info("Imported system logo into database ({} bytes)", fromPath.length);
            }
        } else if (isFilesystemUrl(branding.getLogoUrl())) {
            branding.setLogoUrl("/api/system/logo/image");
            changed = true;
        }

        if (!hasBytes(branding.getFaviconBytes())) {
            byte[] fromPath = readIfExists(branding.getFaviconPath());
            if (fromPath == null) {
                fromPath = readFirstFile(Paths.get("uploads", "system-favicon"));
            }
            if (fromPath != null) {
                branding.setFaviconBytes(fromPath);
                branding.setFaviconContentType(guessContentType(branding.getFaviconPath(), fromPath));
                branding.setFaviconUrl("/api/system/favicon/image");
                branding.setFaviconPath(null);
                changed = true;
                log.info("Imported system favicon into database ({} bytes)", fromPath.length);
            }
        } else if (isFilesystemUrl(branding.getFaviconUrl())) {
            branding.setFaviconUrl("/api/system/favicon/image");
            changed = true;
        }

        if (changed) {
            branding.setUpdatedAt(Instant.now());
            brandingRepository.save(branding);
        }
    }

    private static boolean hasBytes(byte[] bytes) {
        return bytes != null && bytes.length > 0;
    }

    private static boolean isFilesystemUrl(String url) {
        return url != null && (url.startsWith("/uploads/") || url.startsWith("uploads/"));
    }

    private static byte[] readIfExists(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        try {
            Path p = Paths.get(path);
            if (Files.isRegularFile(p)) {
                return Files.readAllBytes(p);
            }
        } catch (Exception ex) {
            log.debug("Could not read branding file {}: {}", path, ex.getMessage());
        }
        return null;
    }

    private static byte[] readFirstFile(Path dir) {
        try {
            if (!Files.isDirectory(dir)) {
                return null;
            }
            try (var stream = Files.list(dir)) {
                return stream.filter(Files::isRegularFile)
                        .findFirst()
                        .map(p -> {
                            try {
                                return Files.readAllBytes(p);
                            } catch (Exception ex) {
                                return null;
                            }
                        })
                        .orElse(null);
            }
        } catch (Exception ex) {
            log.debug("Could not scan {}: {}", dir, ex.getMessage());
            return null;
        }
    }

    private static String guessContentType(String path, byte[] bytes) {
        if (path != null) {
            String lower = path.toLowerCase();
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".gif")) return "image/gif";
            if (lower.endsWith(".webp")) return "image/webp";
            if (lower.endsWith(".ico")) return "image/x-icon";
            if (lower.endsWith(".svg")) return "image/svg+xml";
        }
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50) {
            return "image/png";
        }
        return "application/octet-stream";
    }
}
