package com.online.attendance.forms;

import com.online.attendance.company.Company;
import com.online.attendance.security.CurrentCompanyService;
import com.online.attendance.user.AppUser;
import com.online.attendance.user.UserRepository;
import com.online.attendance.forms.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/forms")
public class FormsController {

    private static final Logger log = LoggerFactory.getLogger(FormsController.class);

    private static final Path UPLOAD_DIR = Paths.get("uploads", "forms");

    private final FormRepository formRepository;
    private final FormFieldRepository formFieldRepository;
    private final FormSubmissionRepository submissionRepository;
    private final SubmissionFileRepository submissionFileRepository;
    private final CurrentCompanyService currentCompanyService;
    private final UserRepository userRepository;

    public FormsController(
            FormRepository formRepository,
            FormFieldRepository formFieldRepository,
            FormSubmissionRepository submissionRepository,
            SubmissionFileRepository submissionFileRepository,
            CurrentCompanyService currentCompanyService,
            UserRepository userRepository
    ) {
        this.formRepository = formRepository;
        this.formFieldRepository = formFieldRepository;
        this.submissionRepository = submissionRepository;
        this.submissionFileRepository = submissionFileRepository;
        this.currentCompanyService = currentCompanyService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @GetMapping
    public List<FormDto> list(Authentication authentication, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        List<Form> forms = formRepository.findAllByCompany_IdOrderByUpdatedAtDesc(company.getId());
        List<FormDto> out = new ArrayList<>();
        for (Form f : forms) {
            out.add(toDto(f, null, company));
        }
        return out;
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> get(Authentication authentication, @PathVariable Long id, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        Form form = formRepository.findByIdAndCompany_Id(id, company.getId()).orElse(null);
        if (form == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Form not found"));
        }
        List<FormField> fields = formFieldRepository.findAllByForm_IdOrderBySortOrderAsc(form.getId());
        return ResponseEntity.ok(toDto(form, fields, company));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(Authentication authentication, @Valid @RequestBody UpsertFormRequest request, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);

        Instant now = Instant.now();
        String token = generateToken();

        Form form = Form.builder()
                .company(company)
                .title(request.getTitle().trim())
                .description(trimToNull(request.getDescription()))
                .companyLogoUrl(trimToNull(request.getCompanyLogoUrl()) != null ? trimToNull(request.getCompanyLogoUrl()) : trimToNull(company.getLogoUrl()))
                .loginRequired(Boolean.TRUE.equals(request.getLoginRequired()))
                .publicEnabled(Boolean.TRUE.equals(request.getPublicEnabled()))
                .publicToken(token)
                .fileStorageMode(request.getFileStorageMode())
                .active(Boolean.TRUE.equals(request.getActive()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            form = formRepository.save(form);
            saveFields(form, request.getFields());
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unable to save form. Please verify fields and try again."));
        }

        List<FormField> fields = formFieldRepository.findAllByForm_IdOrderBySortOrderAsc(form.getId());
        return ResponseEntity.ok(toDto(form, fields, company));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody UpsertFormRequest request, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        Form form = formRepository.findByIdAndCompany_Id(id, company.getId()).orElse(null);
        if (form == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Form not found"));
        }

        form.setTitle(request.getTitle().trim());
        form.setDescription(trimToNull(request.getDescription()));
        String desiredLogo = trimToNull(request.getCompanyLogoUrl());
        form.setCompanyLogoUrl(desiredLogo != null ? desiredLogo : trimToNull(company.getLogoUrl()));
        form.setLoginRequired(Boolean.TRUE.equals(request.getLoginRequired()));
        form.setPublicEnabled(Boolean.TRUE.equals(request.getPublicEnabled()));
        form.setActive(Boolean.TRUE.equals(request.getActive()));
        form.setFileStorageMode(request.getFileStorageMode());

        if (form.getPublicToken() == null || form.getPublicToken().isBlank()) {
            form.setPublicToken(generateToken());
        }

        try {
            form.setUpdatedAt(Instant.now());
            form = formRepository.save(form);

            formFieldRepository.deleteByFormId(form.getId());
            saveFields(form, request.getFields());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Invalid form fields"));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unable to update form. Please verify fields and try again."));
        } catch (Exception e) {
            String errorId = UUID.randomUUID().toString();
            log.error("Forms update failed. errorId={}, formId={}, companyId={}", errorId, id, company.getId(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Unable to update form due to an unexpected server error.",
                    "errorId", errorId
            ));
        }

        List<FormField> fields = formFieldRepository.findAllByForm_IdOrderBySortOrderAsc(form.getId());
        return ResponseEntity.ok(toDto(form, fields, company));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @PostMapping("/{id}/rotate-token")
    public ResponseEntity<?> rotateToken(Authentication authentication, @PathVariable Long id, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        Form form = formRepository.findByIdAndCompany_Id(id, company.getId()).orElse(null);
        if (form == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Form not found"));
        }
        form.setPublicToken(generateToken());
        form.setUpdatedAt(Instant.now());
        form = formRepository.save(form);
        return ResponseEntity.ok(Map.of("publicToken", form.getPublicToken()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(Authentication authentication, @PathVariable Long id, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        Form form = formRepository.findByIdAndCompany_Id(id, company.getId()).orElse(null);
        if (form == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Form not found"));
        }

        long submissionCount = submissionRepository.countByForm_Id(form.getId());
        if (submissionCount > 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot delete a form that already has submissions."));
        }

        // Minimal delete: fields then form. (Submissions remain unless manually cleaned.)
        try {
            formFieldRepository.deleteByFormId(form.getId());
            formRepository.delete(form);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot delete this form due to related data."));
        } catch (Exception e) {
            String errorId = UUID.randomUUID().toString();
            log.error("Forms delete failed. errorId={}, formId={}, companyId={}", errorId, id, company.getId(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Unable to delete form due to an unexpected server error.",
                    "errorId", errorId
            ));
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/{id}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submitAuthenticated(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam("answersJson") String answersJson,
            MultipartHttpServletRequest multipart,
            HttpServletRequest request,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId
    ) throws IOException {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        Form form = formRepository.findByIdAndCompany_Id(id, company.getId()).orElse(null);
        if (form == null || !form.isActive()) {
            return ResponseEntity.status(404).body(Map.of("message", "Form not found"));
        }
        if (!form.isLoginRequired()) {
            return ResponseEntity.status(400).body(Map.of("message", "This form is not configured for login-required submission"));
        }

        String username = currentCompanyService.requireUsername(authentication);
        String companySlug = currentCompanyService.requireCompanySlug(authentication);
        AppUser user = userRepository.findByUsernameAndCompanySlug(username, companySlug).orElse(null);

        FormSubmission submission = FormSubmission.builder()
                .form(form)
                .company(company)
                .submittedAt(Instant.now())
                .submittedByUsername(username)
                .submittedByUserId(user != null ? user.getId() : null)
                .ipAddress(getRemoteIp(request))
                .answersJson(answersJson != null ? answersJson : "{}")
                .build();
        submission = submissionRepository.save(submission);

        saveSubmissionFiles(form, submission, multipart);

        return ResponseEntity.ok(Map.of("id", submission.getId()));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @GetMapping("/{id}/submissions")
    public ResponseEntity<?> listSubmissions(Authentication authentication, @PathVariable Long id, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        Form form = formRepository.findByIdAndCompany_Id(id, company.getId()).orElse(null);
        if (form == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Form not found"));
        }

        List<FormSubmission> subs = submissionRepository.findAllByForm_IdOrderBySubmittedAtDesc(form.getId());
        List<SubmissionDto> out = new ArrayList<>();
        for (FormSubmission s : subs) {
            out.add(SubmissionDto.builder()
                    .id(s.getId())
                    .formId(form.getId())
                    .companyId(company.getId())
                    .submittedAt(s.getSubmittedAt())
                    .submittedByUsername(s.getSubmittedByUsername())
                    .submittedByUserId(s.getSubmittedByUserId())
                    .answersJson(s.getAnswersJson())
                    .build());
        }
        return ResponseEntity.ok(out);
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @GetMapping("/submissions/{submissionId}/files")
    public ResponseEntity<?> listSubmissionFiles(Authentication authentication, @PathVariable Long submissionId, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        FormSubmission s = submissionRepository.findById(submissionId).orElse(null);
        if (s == null || s.getCompany() == null || !company.getId().equals(s.getCompany().getId())) {
            return ResponseEntity.status(404).body(Map.of("message", "Submission not found"));
        }

        List<SubmissionFile> files = submissionFileRepository.findAllBySubmissionId(s.getId());
        List<SubmissionFileDto> out = new ArrayList<>();
        for (SubmissionFile f : files) {
            out.add(SubmissionFileDto.builder()
                    .id(f.getId())
                    .submissionId(s.getId())
                    .fieldKey(f.getFieldKey())
                    .fileName(f.getFileName())
                    .contentType(f.getContentType())
                    .sizeBytes(f.getSizeBytes())
                    .build());
        }
        return ResponseEntity.ok(out);
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ADMIN')")
    @GetMapping(value = "/files/{fileId}")
    public ResponseEntity<?> downloadFile(Authentication authentication, @PathVariable Long fileId, @RequestHeader(value = "X-Company-Id", required = false) Long companyId) throws IOException {
        Company company = currentCompanyService.requireCompany(authentication, companyId);
        SubmissionFile f = submissionFileRepository.findById(fileId).orElse(null);
        if (f == null || f.getSubmission() == null || f.getSubmission().getCompany() == null || !company.getId().equals(f.getSubmission().getCompany().getId())) {
            return ResponseEntity.status(404).body(Map.of("message", "File not found"));
        }

        String filename = f.getFileName() != null ? f.getFileName() : ("file-" + f.getId());
        String contentType = f.getContentType() != null && !f.getContentType().isBlank() ? f.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        byte[] bytes;
        if (f.getFileBytes() != null && f.getFileBytes().length > 0) {
            bytes = f.getFileBytes();
        } else if (f.getDiskPath() != null && !f.getDiskPath().isBlank()) {
            Path p = Paths.get(f.getDiskPath());
            if (!Files.exists(p)) {
                return ResponseEntity.status(404).body(Map.of("message", "File not found"));
            }
            bytes = Files.readAllBytes(p);
        } else {
            return ResponseEntity.status(404).body(Map.of("message", "File not found"));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private void saveFields(Form form, List<UpsertFormFieldRequest> fields) {
        if (fields == null) {
            return;
        }
        for (UpsertFormFieldRequest r : fields) {
            if (r == null) continue;

            String key = trimToNull(r.getKey());
            String label = trimToNull(r.getLabel());
            if (key == null) {
                throw new IllegalArgumentException("Field key is required");
            }
            if (label == null) {
                throw new IllegalArgumentException("Field label is required");
            }
            if (r.getType() == null) {
                throw new IllegalArgumentException("Field type is required");
            }
            FormField f = FormField.builder()
                    .form(form)
                    .key(key)
                    .label(label)
                    .description(trimToNull(r.getDescription()))
                    .placeholder(trimToNull(r.getPlaceholder()))
                    .type(r.getType())
                    .required(Boolean.TRUE.equals(r.getRequired()))
                    .sortOrder(r.getSortOrder() != null ? r.getSortOrder() : 0)
                    .optionsJson(trimToNull(r.getOptionsJson()))
                    .accept(trimToNull(r.getAccept()))
                    .build();
            formFieldRepository.save(f);
        }
    }

    private void saveSubmissionFiles(Form form, FormSubmission submission, MultipartHttpServletRequest multipart) throws IOException {
        if (multipart == null) {
            return;
        }

        Map<String, List<MultipartFile>> map = multipart.getMultiFileMap();
        if (map == null || map.isEmpty()) {
            return;
        }

        Files.createDirectories(UPLOAD_DIR);

        for (Map.Entry<String, List<MultipartFile>> e : map.entrySet()) {
            String fieldKey = e.getKey();
            if (!StringUtils.hasText(fieldKey)) continue;
            if ("answersJson".equals(fieldKey)) continue;

            List<MultipartFile> files = e.getValue();
            if (files == null) continue;

            for (MultipartFile mf : files) {
                if (mf == null || mf.isEmpty()) continue;

                String originalName = StringUtils.hasText(mf.getOriginalFilename()) ? mf.getOriginalFilename() : ("upload-" + UUID.randomUUID());
                String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");

                SubmissionFile sf = SubmissionFile.builder()
                        .submission(submission)
                        .fieldKey(fieldKey)
                        .fileName(originalName)
                        .contentType(mf.getContentType())
                        .sizeBytes(mf.getSize())
                        .build();

                if (form.getFileStorageMode() == FileStorageMode.DB) {
                    sf.setFileBytes(mf.getBytes());
                } else {
                    Path dir = UPLOAD_DIR.resolve(String.valueOf(submission.getId()));
                    Files.createDirectories(dir);
                    Path target = dir.resolve(UUID.randomUUID() + "_" + safeName);
                    Files.write(target, mf.getBytes());
                    sf.setDiskPath(target.toString());
                }

                submissionFileRepository.save(sf);
            }
        }
    }

    private FormDto toDto(Form form, List<FormField> fields, Company companyOverride) {
        List<FormFieldDto> outFields = null;
        if (fields != null) {
            outFields = new ArrayList<>();
            for (FormField f : fields) {
                outFields.add(FormFieldDto.builder()
                        .id(f.getId())
                        .key(f.getKey())
                        .label(f.getLabel())
                        .description(f.getDescription())
                        .placeholder(f.getPlaceholder())
                        .type(f.getType())
                        .required(f.isRequired())
                        .sortOrder(f.getSortOrder())
                        .optionsJson(f.getOptionsJson())
                        .accept(f.getAccept())
                        .build());
            }
        }

        Company company = companyOverride;
        if (company == null) {
            // Only fall back to the relation if we weren't provided a company.
            // (This may be lazily loaded depending on environment.)
            company = form.getCompany();
        }

        return FormDto.builder()
                .id(form.getId())
                .companyId(company != null ? company.getId() : null)
                .title(form.getTitle())
                .description(form.getDescription())
                .companyLogoUrl(form.getCompanyLogoUrl() != null && !form.getCompanyLogoUrl().isBlank()
                        ? form.getCompanyLogoUrl()
                        : (company != null ? company.getLogoUrl() : null))
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

    private String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isBlank() ? null : t;
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
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
