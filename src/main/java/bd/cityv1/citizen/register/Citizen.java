package bd.cityv1.citizen.register;

import bd.cityv1.complaint.common.Complaint;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import bd.cityv1.validation.InputRules;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Table(name = "citizens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Citizen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String citizenId;

    @NotBlank
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters.")
    private String name;

    @Column(unique = true, nullable = false)
    @NotBlank
    @Pattern(regexp = InputRules.PHONE, message = InputRules.PHONE_MESSAGE)
    private String phone;

    @Email
    @NotBlank
    @Size(max = 254, message = "Email must be at most 254 characters.")
    private String email;

    private String nationalId;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String occupation;

    @Embedded
    @Valid
    private CitizenAddress address;

    @NotBlank
    @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters")
    @lombok.ToString.Exclude
    private String password;

    @jakarta.persistence.Column(length = 500)
    private String profileImagePath;

    @OneToMany(mappedBy = "citizen", cascade = CascadeType.ALL)
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private List<Complaint> complaints = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();
}
