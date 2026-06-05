package com.online.attendance.employee;

import com.online.attendance.user.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "employees", uniqueConstraints = {
        @UniqueConstraint(name = "uk_employees_code", columnNames = "employee_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", nullable = false, length = 50)
    private String employeeCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 100)
    private String department;

    @Column(length = 30)
    private String mobile;

    @Column(length = 100)
    private String designation;

    @Column(length = 50)
    private String category;

    @Column(name = "face_template_ref")
    private String faceTemplateRef;

    /** AI face descriptor (128 floats as JSON array). When set, verification uses euclidean distance. */
    @Column(name = "face_descriptor", columnDefinition = "TEXT")
    private String faceDescriptor;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "profile_image_path", length = 1000)
    private String profileImagePath;

    @Column(name = "profile_image_bytes", columnDefinition = "BYTEA")
    private byte[] profileImageBytes;

    @Column(name = "profile_image_content_type", length = 120)
    private String profileImageContentType;

    @Column(name = "hourly_rate_override", precision = 12, scale = 2)
    private BigDecimal hourlyRateOverride;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_employee_user"))
    private AppUser user;
}
