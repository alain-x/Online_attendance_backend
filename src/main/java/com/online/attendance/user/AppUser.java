package com.online.attendance.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.online.attendance.company.Company;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_company_username", columnNames = {"company_id", "username"}),
        @UniqueConstraint(name = "uk_users_company_email", columnNames = {"company_id", "email"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column(length = 200)
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "company_id", foreignKey = @ForeignKey(name = "fk_user_company"))
    private Company company;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "profile_image_path", length = 1000)
    private String profileImagePath;

    @Column(name = "profile_image_bytes", columnDefinition = "BYTEA")
    private byte[] profileImageBytes;

    @Column(name = "profile_image_content_type", length = 120)
    private String profileImageContentType;
}
