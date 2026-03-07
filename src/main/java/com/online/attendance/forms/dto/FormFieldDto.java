package com.online.attendance.forms.dto;

import com.online.attendance.forms.FieldType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormFieldDto {
    private Long id;
    private String key;
    private String label;
    private String description;
    private FieldType type;
    private boolean required;
    private int sortOrder;
    private String optionsJson;
    private String accept;
}
