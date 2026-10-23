package bd.cityv1.profile.citizen;

import bd.cityv1.citizen.register.Citizen;
import bd.cityv1.citizen.register.CitizenRepository;
import bd.cityv1.profile.citizen.dto.UpdatePersonalInfoDto;
import bd.cityv1.profile.common.ChangePasswordDto;
import bd.cityv1.profile.common.PasswordChangeService;
import bd.cityv1.storage.CloudinaryStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class CitizenProfileService {

    private final CitizenRepository citizenRepository;
    private final PasswordChangeService passwordChangeService;
    private final CloudinaryStorageService storageService;

    public Citizen getCitizen(String email) {
        return citizenRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Citizen not found"));
    }

    public Citizen updatePersonalInfo(String email, UpdatePersonalInfoDto dto) {
        Citizen citizen = getCitizen(email);

        boolean phoneChanged = !citizen.getPhone().equals(dto.phone());
        if (phoneChanged && citizenRepository.existsByPhone(dto.phone())) {
            throw new RuntimeException("Phone number already in use");
        }

        citizen.setName(dto.name());
        citizen.setPhone(dto.phone());
        citizen.getAddress().setStreet(dto.street());
        citizen.getAddress().setCity(dto.city());
        citizen.getAddress().setZipCode(dto.zipCode());

        return citizenRepository.save(citizen);
    }

    public void changePassword(String email, ChangePasswordDto dto) {
        Citizen citizen = getCitizen(email);
        citizen.setPassword(passwordChangeService.validateAndEncode(dto, citizen.getPassword()));
        citizenRepository.save(citizen);
    }

    @Transactional(rollbackFor = IOException.class)
    public Citizen updateAvatar(String email, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Please select an image to upload");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("Image must be under 5MB");
        }

        Citizen citizen = getCitizen(email);

        String previousUrl = citizen.getProfileImagePath();
        citizen.setProfileImagePath(storageService.upload(file, "avatars"));
        storageService.deleteAfterCommit(previousUrl);
        return citizenRepository.save(citizen);
    }
}
