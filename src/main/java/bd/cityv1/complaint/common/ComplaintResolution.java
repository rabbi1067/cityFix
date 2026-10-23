package bd.cityv1.complaint.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Entity
@Table(name = "complaint_resolutions")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintResolution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "complaint_id",
            nullable = false,
            unique = true
    )
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Complaint complaint;

    private LocalDateTime resolvedAt;

    @Column(length = 1000)
    private String costNotes;

    @Column(name = "resolution_image_url", length = 500)
    private String resolutionImageUrl;

    private Integer citizenRating;
    public boolean hasResolutionImage() {
        return resolutionImageUrl != null && !resolutionImageUrl.isBlank();
    }
}
