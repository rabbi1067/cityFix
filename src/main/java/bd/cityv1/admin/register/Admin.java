package bd.cityv1.admin.register;

import bd.cityv1.admin.AdminContactInfo;
import bd.cityv1.admin.superadmin.AdminPosition;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
@Entity
@Table(name = "admins")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @Column(unique = true, nullable = false)
    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 6, message = "Password must be at least 6 characters")
    @lombok.ToString.Exclude
    private String password;

    @Embedded
    private AdminContactInfo contactInfo = new AdminContactInfo();

    @Column(length = 500)
    private String profileImagePath;


    @Enumerated(EnumType.STRING)
    private AdminPosition position = AdminPosition.GENERAL_ADMIN;


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "admin_roles", joinColumns = @JoinColumn(name = "admin_id"))
    @Column(name = "role")
    private List<String> roles = new ArrayList<>();


    @Column(nullable = false)
    private boolean enabled = true;


    private LocalDateTime blockedUntil;

    private LocalDateTime createdAt = LocalDateTime.now();


    @PostLoad
    private void ensureContactInfoNotNull() {
        if (contactInfo == null) {
            contactInfo = new AdminContactInfo();
        }
    }
}
