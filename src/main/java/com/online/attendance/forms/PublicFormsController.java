package com.online.attendance.forms;

import com.online.attendance.forms.dto.FormDto;
import com.online.attendance.forms.dto.FormFieldDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/public/forms")
public class PublicFormsController {

    private final FormRepository formRepository;
    private final FormFieldRepository formFieldRepository;
    private final FormSubmissionRepository submissionRepository;
    private final SubmissionFileRepository submissionFileRepository;

    public PublicFormsController(
            FormRepository formRepository,
            FormFieldRepository formFieldRepository,
            FormSubmissionRepository submissionRepository,
            SubmissionFileRepository submissionFileRepository
    ) {
        this.formRepository = formRepository;
        this.formFieldRepository = formFieldRepository;
        this.submissionRepository = submissionRepository;
        this.submissionFileRepository = submissionFileRepository;
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> getByToken(@PathVariable String token) {
        Form form = formRepository.findByPublicToken(token).orElse(null);
        if (form == null || !form.isActive() || !form.isPublicEnabled()) {
            return ResponseEntity.status(404).body(Map.of("message", "Form not found"));
        }
        List<FormField> fields = formFieldRepository.findAllByFormIdOrderBySortOrderAsc(form.getId());
        return ResponseEntity.ok(toDto(form, fields));
    }

    @PostMapping(value = "/{token}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submitPublic(
            @PathVariable String token,
            @RequestParam("answersJson") String answersJson,
            MultipartHttpServletRequest multipart,
            HttpServletRequest request,
            Authentication authentication
    ) throws IOException {
        Form form = formRepository.findByPublicToken(token).orElse(null);
        if (form == null || !form.isActive() || !form.isPublicEnabled()) {
            return ResponseEntity.status(404).body(Map.of("message", "Form not found"));
        }
        if (form.isLoginRequired()) {
            return ResponseEntity.status(401).body(Map.of("message", "Login is required for this form"));
        }

        FormSubmission submission = FormSubmission.builder()
                .form(form)
                .company(form.getCompany())
                .submittedAt(Instant.now())
                .submittedByUsername(null)
                .submittedByUserId(null)
                .ipAddress(getRemoteIp(request))
                .answersJson(answersJson != null ? answersJson : "{}")
                .build();
        submission = submissionRepository.save(submission);

        saveSubmissionFiles(form, submission, multipart);

        return ResponseEntity.ok(Map.of("id", submission.getId()));
    }

    private FormDto toDto(Form form, List<FormField> fields) {
        List<FormFieldDto> outFields = new ArrayList<>();
        for (FormField f : fields) {
            outFields.add(FormFieldDto.builder()
                    .id(f.getId())
                    .key(f.getKey())
                    .label(f.getLabel())
                    .description(f.getDescription())
                    .type(f.getType())
                    .required(f.isRequired())
                    .sortOrder(f.getSortOrder())
                    .optionsJson(f.getOptionsJson())
                    .accept(f.getAccept())
                    .build());
        }

        return FormDto.builder()
                .id(form.getId())
                .companyId(form.getCompany() != null ? form.getCompany().getId() : null)
                .title(form.getTitle())
                .description(form.getDescription())
                .companyLogoUrl(form.getCompanyLogoUrl())
                .loginRequired(form.isLoginRequired())
                .publicEnabled(form.isPublicEnabled())
                .publicToken(form.getPublicToken())
                .fileStorageMode(form.getFileStorageMode())
                .active(form.isActive())
                .createdAt(form.getCreatedAt())
                .updatedAt(form.getUpdatedAt())
                .fields(outFields)
                .build();
    }

    private void saveSubmissionFiles(Form form, FormSubmission submission, MultipartHttpServletRequest multipart) throws IOException {
        // Reuse logic through a tiny local loop (no disk/DB choice per-file; uses form setting)
        if (multipart == null || multipart.getMultiFileMap() == null) {
            return;
        }

        Map<String, List<org.springframework.web.multipart.MultipartFile>> map = multipart.getMultiFileMap();
        for (Map.Entry<String, List<org.springframework.web.multipart.MultipartFile>> e : map.entrySet()) {
            String fieldKey = e.getKey();
            if (fieldKey == null || fieldKey.isBlank() || "answersJson".equals(fieldKey)) continue;

            List<org.springframework.web.multipart.MultipartFile> files = e.getValue();
            if (files == null) continue;

            for (org.springframework.web.multipart.MultipartFile mf : files) {
                if (mf == null || mf.isEmpty()) continue;

                SubmissionFile sf = SubmissionFile.builder()
                        .submission(submission)
                        .fieldKey(fieldKey)
                        .fileName(mf.getOriginalFilename())
                        .contentType(mf.getContentType())
                        .sizeBytes(mf.getSize())
                        .build();

                if (form.getFileStorageMode() == FileStorageMode.DB) {
                    sf.setFileBytes(mf.getBytes());
                } else {
                    // For public endpoints, we currently store disk path the same as authenticated endpoints.
                    // Folder is still under ./uploads/forms.
                    java.nio.file.Path base = java.nio.file.Paths.get("uploads", "forms").resolve(String.valueOf(submission.getId()));
                    java.nio.file.Files.createDirectories(base);
                    String originalName = (mf.getOriginalFilename() != null && !mf.getOriginalFilename().isBlank()) ? mf.getOriginalFilename() : ("upload-" + java.util.UUID.randomUUID());
                    String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
                    java.nio.file.Path target = base.resolve(java.util.UUID.randomUUID() + "_" + safeName);
                    java.nio.file.Files.write(target, mf.getBytes());
                    sf.setDiskPath(target.toString());
                }

                submissionFileRepository.save(sf);
            }
        }
    }

    private String getRemoteIp(HttpServletRequest request) {
        if (request == null) return null;
        String h = request.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) {
            return h.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
