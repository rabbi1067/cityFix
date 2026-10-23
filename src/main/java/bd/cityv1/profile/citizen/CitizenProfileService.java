package bd.cityv1.profile.citizen;

import bd.cityv1.citizen.register.Citizen;
import bd.cityv1.citizen.register.CitizenRepository;
import bd.cityv1.citizen.register.CitizenAddress;
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

    public boolean isPhoneTaken(String email, String phone) {
        String normalized = bd.cityv1.validation.InputRules.phone(phone);
        if (normalized == null || !normalized.matches(bd.cityv1.validation.InputRules.PHONE)) {
            throw new IllegalArgumentException(bd.cityv1.validation.InputRules.PHONE_MESSAGE);
        }
        Citizen citizen = getCitizen(email);
        return !java.util.Objects.equals(citizen.getPhone(), normalized)
                && citizenRepository.existsByPhone(normalized);
    }

    public Citizen updatePersonalInfo(String email, UpdatePersonalInfoDto dto) {
        Citizen citizen = getCitizen(email);

        boolean phoneChanged = !java.util.Objects.equals(citizen.getPhone(), dto.phone());
        if (phoneChanged && citizenRepository.existsByPhone(dto.phone())) {
            throw new RuntimeException("Phone number already in use");
        }

        citizen.setName(dto.name());
        citizen.setPhone(dto.phone());
        if (citizen.getAddress() == null) citizen.setAddress(new CitizenAddress());
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
        bd.cityv1.storage.ProfileImageValidation.validate(file);

        Citizen citizen = getCitizen(email);

        String previousUrl = citizen.getProfileImagePath();
        citizen.setProfileImagePath(storageService.upload(file, "avatars"));
        storageService.deleteAfterCommit(previousUrl);
        return citizenRepository.save(citizen);
    }
}
