package com.online.attendance.forms;

import com.online.attendance.company.Company;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "form_submissions", indexes = {
        @Index(name = "idx_form_submissions_form", columnList = "form_id"),
        @Index(name = "idx_form_submissions_company", columnList = "company_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_id", nullable = false, foreignKey = @ForeignKey(name = "fk_form_submissions_form"))
    private Form form;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_form_submissions_company"))
    private Company company;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "submitted_by_username", length = 200)
    private String submittedByUsername;

    @Column(name = "submitted_by_user_id")
    private Long submittedByUserId;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Lob
    @Column(name = "answers_json", nullable = false)
    private String answersJson;
}
