package com.online.attendance.system;

import com.online.attendance.system.dto.SystemBrandingResponse;
import com.online.attendance.system.dto.UpdateSystemBrandingRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemBrandingController {

    private static final Logger log = LoggerFactory.getLogger(SystemBrandingController.class);

    private static final String BRANDING_ID = "SYSTEM";
    private static final String LOGO_API_URL = "/api/system/logo/image";
    private static final String FAVICON_API_URL = "/api/system/favicon/image";

    private final SystemBrandingRepository brandingRepository;

    public SystemBrandingController(SystemBrandingRepository brandingRepository) {
        this.brandingRepository = brandingRepository;
    }

    private SystemBranding getOrCreate() {
        return brandingRepository.findById(BRANDING_ID).orElseGet(() -> {
            SystemBranding created = new SystemBranding();
            created.setId(BRANDING_ID);
            created.setUpdatedAt(Instant.now());
            return brandingRepository.save(created);
        });
    }

    private static boolean hasImage(byte[] bytes) {
        return bytes != null && bytes.length > 0;
    }

    private static String logoApiUrl(SystemBranding branding) {
        return hasImage(branding.getLogoBytes()) ? LOGO_API_URL : null;
    }

    private static String faviconApiUrl(SystemBranding branding) {
        return hasImage(branding.getFaviconBytes()) ? FAVICON_API_URL : null;
    }

    @GetMapping("/branding")
    public ResponseEntity<SystemBrandingResponse> getBranding() {
        SystemBranding branding = getOrCreate();
        return ResponseEntity.ok(new SystemBrandingResponse(
                logoApiUrl(branding),
                faviconApiUrl(branding),
                branding.getSystemName()
        ));
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

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @DeleteMapping("/branding")
    public ResponseEntity<?> deleteBranding() {
        SystemBranding branding = getOrCreate();
        branding.setSystemName(null);
        branding.setUpdatedAt(Instant.now());
        brandingRepository.save(branding);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @GetMapping("/logo")
    public ResponseEntity<Map<String, String>> getLogo() {
        SystemBranding branding = brandingRepository.findById(BRANDING_ID).orElse(null);
        String url = branding != null ? logoApiUrl(branding) : null;
        Map<String, String> body = new java.util.HashMap<>();
        body.put("logoUrl", url);
        return ResponseEntity.ok(body);
    }

    @Transactional(readOnly = true)
    @GetMapping("/logo/image")
    public ResponseEntity<?> getLogoImage() {
        var view = brandingRepository.findLogoViewById(BRANDING_ID).orElse(null);
        if (view == null || !hasImage(view.getImageBytes())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "System logo not set"));
        }
        return imageResponse(view.getImageBytes(), view.getContentType(), MediaType.IMAGE_PNG_VALUE);
    }

    @GetMapping("/favicon")
    public ResponseEntity<Map<String, String>> getFavicon() {
        SystemBranding branding = brandingRepository.findById(BRANDING_ID).orElse(null);
        String url = branding != null ? faviconApiUrl(branding) : null;
        Map<String, String> body = new java.util.HashMap<>();
        body.put("faviconUrl", url);
        return ResponseEntity.ok(body);
    }

    @Transactional(readOnly = true)
    @GetMapping("/favicon/image")
    public ResponseEntity<?> getFaviconImage() {
        var view = brandingRepository.findFaviconViewById(BRANDING_ID).orElse(null);
        if (view == null || !hasImage(view.getImageBytes())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "System favicon not set"));
        }
        return imageResponse(view.getImageBytes(), view.getContentType(), "image/x-icon");
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @Transactional
    @PostMapping(value = "/favicon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFavicon(@RequestPart("file") @NotNull MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is required"));
        }
        SystemBranding branding = getOrCreate();
        branding.setFaviconBytes(file.getBytes());
        branding.setFaviconContentType(resolveContentType(file.getContentType(), file.getOriginalFilename()));
        branding.setFaviconUrl(FAVICON_API_URL);
        branding.setFaviconPath(null);
        branding.setUpdatedAt(Instant.now());
        brandingRepository.save(branding);
        return ResponseEntity.ok(Map.of("faviconUrl", FAVICON_API_URL));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @DeleteMapping("/favicon")
    public ResponseEntity<?> deleteFavicon() {
        SystemBranding branding = getOrCreate();
        branding.setFaviconUrl(null);
        branding.setFaviconPath(null);
        branding.setFaviconBytes(null);
        branding.setFaviconContentType(null);
        branding.setUpdatedAt(Instant.now());
        brandingRepository.save(branding);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @Transactional
    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadLogo(@RequestPart("file") @NotNull MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is required"));
        }
        SystemBranding branding = getOrCreate();
        branding.setLogoBytes(file.getBytes());
        branding.setLogoContentType(resolveContentType(file.getContentType(), file.getOriginalFilename()));
        branding.setLogoUrl(LOGO_API_URL);
        branding.setLogoPath(null);
        branding.setUpdatedAt(Instant.now());
        brandingRepository.save(branding);
        return ResponseEntity.ok(Map.of("logoUrl", LOGO_API_URL));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @DeleteMapping("/logo")
    public ResponseEntity<?> deleteLogo() {
        SystemBranding branding = getOrCreate();
        branding.setLogoUrl(null);
        branding.setLogoPath(null);
        branding.setLogoBytes(null);
        branding.setLogoContentType(null);
        branding.setUpdatedAt(Instant.now());
        brandingRepository.save(branding);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    private static ResponseEntity<byte[]> imageResponse(byte[] bytes, String contentType, String defaultType) {
        String resolved = (contentType != null && !contentType.isBlank()) ? contentType : defaultType;
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("public, max-age=86400");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(resolved))
                .body(bytes);
    }

    private static String resolveContentType(String fromMultipart, String filename) {
        if (fromMultipart != null && !fromMultipart.isBlank()) {
            return fromMultipart;
        }
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".gif")) return "image/gif";
            if (lower.endsWith(".webp")) return "image/webp";
            if (lower.endsWith(".ico")) return "image/x-icon";
            if (lower.endsWith(".svg")) return "image/svg+xml";
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
