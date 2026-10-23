package bd.cityv1.complaint.admin;

import bd.cityv1.complaint.admin.dto.ResolveComplaintRequest;
import bd.cityv1.complaint.common.Complaint;
import bd.cityv1.complaint.common.ComplaintRepository;
import bd.cityv1.complaint.common.ComplaintResolution;
import bd.cityv1.complaint.common.Status;
import bd.cityv1.storage.CloudinaryStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminComplaintService {

    private final ComplaintRepository complaintRepository;
    private final CloudinaryStorageService storageService;
    public Complaint getComplaintById(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found with id: " + id));
    }

    @Transactional(rollbackFor = IOException.class)
    public Complaint resolveOrUpdateComplaint(Long complaintId,
                                              ResolveComplaintRequest request,
                                              MultipartFile resolutionImage) throws IOException {
        Complaint complaint = getComplaintById(complaintId);

        if (request.status() == null || request.priority() == null) {
            throw new IllegalArgumentException("Status and priority are required.");
        }
        if (request.finalCost() != null && (!Double.isFinite(request.finalCost()) || request.finalCost() < 0)) {
            throw new IllegalArgumentException("Final cost cannot be negative.");
        }

        boolean wasAlreadyResolved = complaint.getStatus() == Status.RESOLVED;

        if (wasAlreadyResolved) {
            if (request.status() != Status.RESOLVED) {
                throw new IllegalArgumentException(
                        "This complaint is already resolved. Status cannot be changed anymore.");
            }
            if (request.priority() != complaint.getPriority()) {
                throw new IllegalArgumentException(
                        "This complaint is already resolved. Priority cannot be changed anymore.");
            }
            if (!java.util.Objects.equals(request.finalCost(), complaint.getFinalCost())) {
                throw new IllegalArgumentException(
                        "This complaint is already resolved. Final cost cannot be changed anymore.");
            }
        }

        boolean hasCostNotes = request.costNotes() != null && !request.costNotes().isBlank();
        boolean hasResolutionImage = resolutionImage != null && !resolutionImage.isEmpty();
        boolean justResolved = request.status() == Status.RESOLVED;
        boolean alreadyHasResolutionImage = complaint.getResolution() != null
                && complaint.getResolution().hasResolutionImage();

        if (justResolved && request.finalCost() == null) {
            throw new IllegalArgumentException(
                    "Final cost is required when marking a complaint as resolved.");
        }

        if (justResolved && !hasResolutionImage && !alreadyHasResolutionImage) {
            throw new IllegalArgumentException(
                    "An After Resolved photo is required before marking a complaint as resolved.");
        }

        if (hasResolutionImage) {
            validateImage(resolutionImage);
        }

        complaint.setStatus(request.status());
        complaint.setPriority(request.priority());
        complaint.setFinalCost(request.finalCost());

        if (justResolved || hasCostNotes || hasResolutionImage) {
            ComplaintResolution resolution = complaint.getResolution();
            if (resolution == null) {
                resolution = new ComplaintResolution();
                resolution.setComplaint(complaint);
                complaint.setResolution(resolution);
            }

            if (justResolved && resolution.getResolvedAt() == null) {
                resolution.setResolvedAt(LocalDateTime.now());
            }

            if (hasCostNotes) {
                resolution.setCostNotes(request.costNotes());
            }

            if (hasResolutionImage) {
                String previousUrl = resolution.getResolutionImageUrl();
                String url = storageService.upload(resolutionImage, "after");
                resolution.setResolutionImageUrl(url);
                storageService.deleteAfterCommit(previousUrl);
            }
        }

        return complaintRepository.save(complaint);
    }

    private void validateImage(MultipartFile file) {
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Image must be smaller than 10 MB.");
        }
        resolveContentType(file);
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String normalized = contentType.toLowerCase(Locale.ROOT);
            if (Set.of("image/jpeg", "image/png", "image/webp").contains(normalized)) {
                return normalized;
            }
        }

        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lower = filename.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".webp")) return "image/webp";
        }
        throw new IllegalArgumentException("Only JPG, PNG, or WEBP images are allowed.");
    }
}
