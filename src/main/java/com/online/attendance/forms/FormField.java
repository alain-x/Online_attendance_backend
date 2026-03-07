package com.online.attendance.forms;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "form_fields", indexes = {
        @Index(name = "idx_form_fields_form", columnList = "form_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_id", nullable = false, foreignKey = @ForeignKey(name = "fk_form_fields_form"))
    private Form form;

    @Column(nullable = false, length = 64)
    private String key;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FieldType type;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Lob
    @Column(name = "options_json")
    private String optionsJson;

    @Column(name = "accept", length = 300)
    private String accept;
}
