package bd.cityv1.storage;

import bd.cityv1.citizen.register.Citizen;
import bd.cityv1.citizen.register.CitizenRepository;
import bd.cityv1.complaint.ComplaintService;
import bd.cityv1.complaint.CostEstimationService;
import bd.cityv1.complaint.admin.AdminComplaintService;
import bd.cityv1.complaint.admin.dto.ResolveComplaintRequest;
import bd.cityv1.complaint.citizen.dto.CreateComplaintRequest;
import bd.cityv1.complaint.common.*;
import bd.cityv1.profile.citizen.CitizenProfileService;
import bd.cityv1.profile.admin.AdminProfileService;
import bd.cityv1.admin.register.Admin;
import bd.cityv1.admin.register.AdminRepository;
import bd.cityv1.profile.common.PasswordChangeService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CloudinaryImageWorkflowTest {

    private final CloudinaryStorageService storage = mock(CloudinaryStorageService.class);
    private final ComplaintRepository complaints = mock(ComplaintRepository.class);
    private final MockMultipartFile image = new MockMultipartFile("image", "proof.png", "image/png", new byte[]{1, 2, 3});

    @Test
    void savesComplaintWithOnlyCloudinaryUrl() throws Exception {
        ComplaintService service = new ComplaintService(complaints, mock(CostEstimationService.class), storage);
        when(storage.upload(image, "before")).thenReturn("https://res.cloudinary.com/demo/proof.png");
        when(complaints.save(any(Complaint.class))).thenAnswer(call -> call.getArgument(0));
        CreateComplaintRequest request = new CreateComplaintRequest();
        request.setTitle("Broken road");
        request.setDescription("There is a large pothole");
        request.setPriority(Priority.HIGH);
        request.setCategory("Road");
        request.setLocation("Dhaka");

        Complaint saved = service.saveComplaint(request, new Citizen(), image);
        assertEquals("https://res.cloudinary.com/demo/proof.png", saved.getImageUrl());
        verify(complaints).save(saved);
    }

    @Test
    void failedUploadDoesNotSaveTheComplaint() throws Exception {
        ComplaintService service = new ComplaintService(complaints, mock(CostEstimationService.class), storage);
        when(storage.upload(image, "before")).thenThrow(new IOException("Offline"));
        assertThrows(IOException.class, () -> service.saveComplaint(new CreateComplaintRequest(), new Citizen(), image));
        verify(complaints, never()).save(any());
    }

    @Test
    void failedReplacementPreservesThePreviousResolutionPhoto() throws Exception {
        Complaint complaint = complaintWithResolution();
        when(complaints.findById(1L)).thenReturn(Optional.of(complaint));
        when(storage.upload(image, "after")).thenThrow(new IOException("Offline"));
        AdminComplaintService service = new AdminComplaintService(complaints, storage);

        assertThrows(IOException.class, () -> service.resolveOrUpdateComplaint(1L,
                new ResolveComplaintRequest(Status.IN_PROGRESS, Priority.HIGH, null, null), image));
        assertEquals("old-photo", complaint.getResolution().getResolutionImageUrl());
        verify(storage, never()).deleteAfterCommit(any());
        verify(complaints, never()).save(any());
    }

    @Test
    void successfulReplacementSchedulesOldPhotoDeletionAfterUpload() throws Exception {
        Complaint complaint = complaintWithResolution();
        when(complaints.findById(1L)).thenReturn(Optional.of(complaint));
        when(storage.upload(image, "after")).thenReturn("new-photo");
        when(complaints.save(any(Complaint.class))).thenAnswer(call -> call.getArgument(0));
        AdminComplaintService service = new AdminComplaintService(complaints, storage);

        Complaint saved = service.resolveOrUpdateComplaint(1L,
                new ResolveComplaintRequest(Status.IN_PROGRESS, Priority.HIGH, null, null), image);
        assertEquals("new-photo", saved.getResolution().getResolutionImageUrl());
        var order = inOrder(storage, complaints);
        order.verify(storage).upload(image, "after");
        order.verify(storage).deleteAfterCommit("old-photo");
        order.verify(complaints).save(complaint);
    }

    @Test
    void savesAvatarInCloudinaryInsteadOfLocalUploadsDirectory() throws Exception {
        CitizenRepository citizens = mock(CitizenRepository.class);
        Citizen citizen = new Citizen();
        citizen.setProfileImagePath("/uploads/old.png");
        when(citizens.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizen));
        when(citizens.save(citizen)).thenReturn(citizen);
        when(storage.upload(image, "avatars")).thenReturn("https://res.cloudinary.com/demo/avatar.png");
        CitizenProfileService service = new CitizenProfileService(citizens, mock(PasswordChangeService.class), storage);

        assertEquals("https://res.cloudinary.com/demo/avatar.png",
                service.updateAvatar("citizen@example.com", image).getProfileImagePath());
        verify(storage).deleteAfterCommit("/uploads/old.png");
    }

    private Complaint complaintWithResolution() {
        Complaint complaint = new Complaint();
        complaint.setStatus(Status.IN_PROGRESS);
        complaint.setPriority(Priority.HIGH);
        ComplaintResolution resolution = new ComplaintResolution();
        resolution.setResolutionImageUrl("old-photo");
        complaint.setResolution(resolution);
        return complaint;
    }

    @Test
    void savesAdminAvatarUrlAndSchedulesPreviousImageCleanup() throws Exception {
        AdminRepository admins = mock(AdminRepository.class);
        Admin admin = new Admin();
        admin.setProfileImagePath("old-avatar-url");
        when(admins.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(admins.save(admin)).thenReturn(admin);
        when(storage.upload(image, "avatars")).thenReturn("https://res.cloudinary.com/demo/new-avatar.png");
        AdminProfileService service = new AdminProfileService(admins, mock(PasswordChangeService.class), storage);

        assertEquals("https://res.cloudinary.com/demo/new-avatar.png",
                service.updateAvatar("admin@example.com", image).getProfileImagePath());
        verify(storage).deleteAfterCommit("old-avatar-url");
        verify(admins).save(admin);
    }

    @Test
    void failedAdminAvatarUploadKeepsThePreviousUrl() throws Exception {
        AdminRepository admins = mock(AdminRepository.class);
        Admin admin = new Admin();
        admin.setProfileImagePath("old-avatar-url");
        when(admins.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(storage.upload(image, "avatars")).thenThrow(new IOException("Offline"));
        AdminProfileService service = new AdminProfileService(admins, mock(PasswordChangeService.class), storage);

        assertThrows(IOException.class, () -> service.updateAvatar("admin@example.com", image));
        assertEquals("old-avatar-url", admin.getProfileImagePath());
        verify(admins, never()).save(any());
        verify(storage, never()).deleteAfterCommit(any());
    }
}
