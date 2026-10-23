package bd.cityv1.profile.admin;

import bd.cityv1.admin.register.Admin;
import bd.cityv1.admin.register.AdminRepository;
import bd.cityv1.admin.AdminContactInfo;
import bd.cityv1.profile.admin.dto.UpdateAdminPersonalInfoDto;
import bd.cityv1.profile.common.ChangePasswordDto;
import bd.cityv1.profile.common.PasswordChangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import bd.cityv1.storage.CloudinaryStorageService;
import bd.cityv1.storage.ProfileImageValidation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class AdminProfileService {

    private final AdminRepository adminRepository;
    private final PasswordChangeService passwordChangeService;
    private final CloudinaryStorageService storageService;

    public Admin getAdmin(String email) {
        return adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }

    public Admin updatePersonalInfo(String email, UpdateAdminPersonalInfoDto dto) {
        Admin admin = getAdmin(email);

        admin.setName(dto.name());
        if (admin.getContactInfo() == null) admin.setContactInfo(new AdminContactInfo());
        admin.getContactInfo().setPhone(dto.phone());
        admin.getContactInfo().setDepartment(dto.department());

        return adminRepository.save(admin);
    }

    public void changePassword(String email, ChangePasswordDto dto) {
        Admin admin = getAdmin(email);
        admin.setPassword(passwordChangeService.validateAndEncode(dto, admin.getPassword()));
        adminRepository.save(admin);
    }

    @Transactional(rollbackFor = IOException.class)
    public Admin updateAvatar(String email, MultipartFile file) throws IOException {
        ProfileImageValidation.validate(file);
        Admin admin = getAdmin(email);
        String previousUrl = admin.getProfileImagePath();
        admin.setProfileImagePath(storageService.upload(file, "avatars"));
        storageService.deleteAfterCommit(previousUrl);
        return adminRepository.save(admin);
    }
}
