package bd.cityv1.complaint.common;

import bd.cityv1.citizen.register.Citizen;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
@Entity
@Table(name = "complaints")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(
            min = 5,
            max = 150,
            message = "Title must be between 5 and 150 characters"
    )
    @Column(nullable = false, length = 150)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(
            min = 10,
            max = 1000,
            message = "Description must be between 10 and 1000 characters"
    )
    @Column(nullable = false, length = 1000)
    private String description;

    @NotBlank(message = "Category is required")
    @Column(nullable = false)
    private String category;

    @NotNull(message = "Priority level is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @NotBlank(message = "Location is required")
    @Column(nullable = false)
    private String location;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id")
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Citizen citizen;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private Double estimatedCost;

    private Double finalCost;

    @Builder.Default
    @Column(name = "notification_read", nullable = false)
    private Boolean notificationRead = false;

    @OneToOne(
            mappedBy = "complaint",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private ComplaintResolution resolution;

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isBlank();
    }}
