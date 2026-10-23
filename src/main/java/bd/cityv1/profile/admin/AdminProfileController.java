package bd.cityv1.profile.admin;

import bd.cityv1.admin.register.Admin;
import bd.cityv1.profile.admin.dto.UpdateAdminPersonalInfoDto;
import bd.cityv1.profile.common.ChangePasswordDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import bd.cityv1.storage.ImageDelivery;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminProfileController {

    private final AdminProfileService adminProfileService;
    private final ImageDelivery imageDelivery;

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        Admin admin = adminProfileService.getAdmin(authentication.getName());
        model.addAttribute("admin", admin);
        model.addAttribute("activePage", "profile");
        return "admin/profile";
    }

    @PostMapping("/profile/update-personal")
    @ResponseBody
    public ResponseEntity<?> updatePersonal(@Valid @RequestBody UpdateAdminPersonalInfoDto dto,
                                            Authentication authentication) {
        Admin updated = adminProfileService.updatePersonalInfo(authentication.getName(), dto);
        return ResponseEntity.ok(Map.of(
                "message", "Profile updated successfully!",
                "name", updated.getName()
        ));
    }

    @PostMapping("/profile/change-password")
    @ResponseBody
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordDto dto,
                                            Authentication authentication) {
        adminProfileService.changePassword(authentication.getName(), dto);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully!"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (first, next) -> first));
        return ResponseEntity.badRequest().body(Map.of("error", errors, "fields", fields));
    }

    @PostMapping("/profile/upload-avatar")
    @ResponseBody
    public ResponseEntity<?> uploadAvatar(@RequestParam("image") MultipartFile image,
                                          Authentication authentication) throws IOException {
        Admin updated = adminProfileService.updateAvatar(authentication.getName(), image);
        return ResponseEntity.ok(Map.of("message", "Profile photo updated!",
                "imagePath", imageDelivery.url(updated.getProfileImagePath(), 256)));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> handleImageUploadFailure(IOException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "We couldn't upload your image. Please try again."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}
