package com.online.attendance.system;

import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.online.attendance.system.dto.SystemBrandingResponse;
import com.online.attendance.system.dto.UpdateSystemBrandingRequest;
import jakarta.validation.Valid;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Instant;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/system")
public class SystemBrandingController {

    private static final Path DIR = Paths.get("uploads", "system-logo");
    private static final String BRANDING_ID = "SYSTEM";

    private final SystemBrandingRepository brandingRepository;

    public SystemBrandingController(SystemBrandingRepository brandingRepository) {
        this.brandingRepository = brandingRepository;
    }

    private SystemBranding getOrCreate() {
        SystemBranding branding = brandingRepository.findById(BRANDING_ID).orElse(null);
        if (branding != null) {
            return branding;
        }
        SystemBranding created = new SystemBranding();
        created.setId(BRANDING_ID);
        return brandingRepository.save(created);
    }

    @GetMapping("/branding")
    public ResponseEntity<?> getBranding(HttpServletRequest request) {
        SystemBranding branding = getOrCreate();
        String url;
        if (branding.getLogoBytes() != null && branding.getLogoBytes().length > 0) {
            url = "/api/system/logo/image";
        } else {
            url = branding.getLogoUrl();
        }
        return ResponseEntity.ok(new SystemBrandingResponse(toAbsoluteUrl(request, url), branding.getSystemName()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @PutMapping("/branding")
    public ResponseEntity<?> updateBranding(@Valid @RequestBody UpdateSystemBrandingRequest body) {
        SystemBranding branding = getOrCreate();
        if (body.getSystemName() != null) {
            String v = body.getSystemName().trim();
            branding.setSystemName(v.isBlank() ? null : v);
        }
        branding.setUpdatedAt(Instant.now());
        brandingRepository.save(branding);
        return ResponseEntity.ok(Map.of("updated", true));
    }

    private String toAbsoluteUrl(HttpServletRequest request, String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        if (!url.startsWith("/")) {
            return url;
        }
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80) || ("https".equalsIgnoreCase(scheme) && port == 443);
        return defaultPort ? String.format("%s://%s%s", scheme, host, url) : String.format("%s://%s:%d%s", scheme, host, port, url);
    }

    @GetMapping("/logo")
    public ResponseEntity<?> getLogo(HttpServletRequest request) throws IOException {
        SystemBranding branding = brandingRepository.findById(BRANDING_ID).orElse(null);
        if (branding != null) {
            if (branding.getLogoBytes() != null && branding.getLogoBytes().length > 0) {
                return ResponseEntity.ok(Map.of("logoUrl", toAbsoluteUrl(request, "/api/system/logo/image")));
            }
            if (branding.getLogoUrl() != null && !branding.getLogoUrl().isBlank()) {
                return ResponseEntity.ok(Map.of("logoUrl", toAbsoluteUrl(request, branding.getLogoUrl())));
            }
        }

        Files.createDirectories(DIR);
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(DIR)) {
            for (Path p : ds) {
                if (Files.isRegularFile(p)) {
                    String filename = p.getFileName().toString();
                    String url = "/uploads/system-logo/" + filename;
                    SystemBranding seed = SystemBranding.builder()
                            .id(BRANDING_ID)
                            .logoUrl(url)
                            .logoPath(p.toString())
                            .updatedAt(Instant.now())
                            .build();
                    brandingRepository.save(seed);
                    return ResponseEntity.ok(Map.of("logoUrl", toAbsoluteUrl(request, url)));
                }
            }
        }

        return ResponseEntity.ok(Map.of("logoUrl", null));
    }

    @GetMapping("/logo/image")
    public ResponseEntity<?> getLogoImage() {
        SystemBranding branding = brandingRepository.findById(BRANDING_ID).orElse(null);
        if (branding == null || branding.getLogoBytes() == null || branding.getLogoBytes().length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "System logo not set"));
        }
        String contentType = branding.getLogoContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.IMAGE_PNG_VALUE;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate");
        headers.set(HttpHeaders.PRAGMA, "no-cache");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(contentType))
                .body(branding.getLogoBytes());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadLogo(HttpServletRequest request, @RequestPart("file") @NotNull MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is required"));
        }

        Files.createDirectories(DIR);

        SystemBranding existing = brandingRepository.findById(BRANDING_ID).orElse(null);
        if (existing != null && existing.getLogoPath() != null && !existing.getLogoPath().isBlank()) {
            try {
                Files.deleteIfExists(Paths.get(existing.getLogoPath()));
            } catch (Exception ignored) {
            }
        } else {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(DIR)) {
                for (Path p : ds) {
                    if (Files.isRegularFile(p)) {
                        Files.deleteIfExists(p);
                    }
                }
            }
        }

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null) {
            int idx = original.lastIndexOf('.');
            if (idx >= 0 && idx < original.length() - 1) {
                ext = original.substring(idx);
            }
        }

        String filename = "system-logo-" + UUID.randomUUID() + ext;
        Path out = DIR.resolve(filename);
        Files.write(out, file.getBytes());

        String url = "/uploads/system-logo/" + filename;
        SystemBranding branding = existing != null ? existing : new SystemBranding();
        branding.setId(BRANDING_ID);
        branding.setLogoUrl(url);
        branding.setLogoPath(out.toString());
        branding.setLogoBytes(file.getBytes());
        branding.setLogoContentType(file.getContentType());
        branding.setUpdatedAt(Instant.now());
        brandingRepository.save(branding);

        return ResponseEntity.ok(Map.of("logoUrl", toAbsoluteUrl(request, "/api/system/logo/image")));
    }
}
