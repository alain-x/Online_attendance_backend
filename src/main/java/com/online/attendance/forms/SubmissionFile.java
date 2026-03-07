package com.online.attendance.forms;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "form_submission_files", indexes = {
        @Index(name = "idx_form_submission_files_submission", columnList = "submission_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false, foreignKey = @ForeignKey(name = "fk_form_submission_files_submission"))
    private FormSubmission submission;

    @Column(name = "field_key", nullable = false, length = 64)
    private String fieldKey;

    @Column(name = "file_name", nullable = false, length = 300)
    private String fileName;

    @Column(name = "content_type", length = 200)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "disk_path", length = 800)
    private String diskPath;

    @Lob
    @Column(name = "file_bytes")
    private byte[] fileBytes;
}
