package bd.cityv1.complaint;

import bd.cityv1.citizen.register.Citizen;
import bd.cityv1.complaint.citizen.dto.CreateComplaintRequest;
import bd.cityv1.complaint.common.Complaint;
import bd.cityv1.complaint.common.ComplaintRepository;
import bd.cityv1.complaint.common.Status;
import bd.cityv1.storage.CloudinaryStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final CostEstimationService costEstimationService;
    private final CloudinaryStorageService storageService;

    @Transactional(rollbackFor = IOException.class)
    public Complaint saveComplaint(CreateComplaintRequest dto, Citizen citizen, MultipartFile file) throws IOException {
        Complaint complaint = new Complaint();
        complaint.setTitle(dto.getTitle());
        complaint.setDescription(dto.getDescription());
        complaint.setCategory(dto.getCategory());
        complaint.setPriority(dto.getPriority());
        complaint.setLocation(dto.getLocation());
        complaint.setCitizen(citizen);

        if (file != null && !file.isEmpty()) {
            validateImage(file);
            String url = storageService.upload(file, "before");
            complaint.setImageUrl(url);
        }

        Double estimatedCost = costEstimationService.estimate(dto.getCategory(), dto.getPriority());
        complaint.setEstimatedCost(estimatedCost);

        return complaintRepository.save(complaint);
    }

    public Complaint saveComplaint(CreateComplaintRequest dto,
                                   Citizen citizen,
                                   String legacyImagePath) {
        return saveComplaintWithoutFile(dto, citizen);
    }

    private Complaint saveComplaintWithoutFile(CreateComplaintRequest dto,
                                               Citizen citizen) {
        Complaint complaint = new Complaint();
        complaint.setTitle(dto.getTitle());
        complaint.setDescription(dto.getDescription());
        complaint.setCategory(dto.getCategory());
        complaint.setPriority(dto.getPriority());
        complaint.setLocation(dto.getLocation());
        complaint.setCitizen(citizen);
        complaint.setEstimatedCost(
                costEstimationService.estimate(dto.getCategory(), dto.getPriority()));
        return complaintRepository.save(complaint);
    }

    public List<Complaint> findAllForCitizen(Long citizenId) {
        return complaintRepository.findAllByCitizen_IdOrderByCreatedAtDesc(citizenId);
    }

    public long countActive(Long citizenId) {
        return complaintRepository.countByCitizenIdAndStatusNot(citizenId, Status.RESOLVED);
    }

    public long countResolved(Long citizenId) {
        return complaintRepository.countByCitizenIdAndStatus(citizenId, Status.RESOLVED);
    }

    private void validateImage(MultipartFile file) {
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Image must be smaller than 10 MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !Set.of(
                "image/jpeg", "image/png", "image/webp").contains(
                contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only JPG, PNG, or WEBP images are allowed.");
        }
    }

    public Complaint findById(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found."));
    }

    @Transactional
    public void saveCitizenRating(Long complaintId, Long citizenId, Integer rating) {
        if (rating == null || rating < 1 || rating > 10) {
            throw new IllegalArgumentException("Please choose a rating between 0.5 and 5 stars.");
        }

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found."));

        if (complaint.getCitizen() == null || !citizenId.equals(complaint.getCitizen().getId())) {
            throw new IllegalArgumentException("You are not allowed to rate this complaint.");
        }

        if (complaint.getStatus() != Status.RESOLVED
                || complaint.getResolution() == null
                || !complaint.getResolution().hasResolutionImage()) {
            throw new IllegalArgumentException("A rating is available after the complaint has been resolved.");
        }

        if (complaint.getResolution().getCitizenRating() != null) {
            throw new IllegalArgumentException("Your rating has already been submitted and cannot be changed.");
        }

        complaint.getResolution().setCitizenRating(rating);
        complaintRepository.save(complaint);
    }
}
