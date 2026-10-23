package bd.cityv1.profile.citizen;

import bd.cityv1.citizen.register.Citizen;
import bd.cityv1.profile.citizen.dto.UpdatePersonalInfoDto;
import bd.cityv1.profile.common.ChangePasswordDto;
import bd.cityv1.storage.ImageDelivery;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/citizen")
public class CitizenProfileController {

    private final CitizenProfileService citizenProfileService;
    private final ImageDelivery imageDelivery;

    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        Citizen citizen = citizenProfileService.getCitizen(authentication.getName());
        model.addAttribute("citizen", citizen);
        model.addAttribute("activePage", "profile");
        return "citizen/profile";
    }

    @PostMapping("/profile/update-personal")
    @ResponseBody
    public ResponseEntity<?> updatePersonal(@Valid @RequestBody UpdatePersonalInfoDto dto,
                                            Authentication authentication) {
        Citizen updated = citizenProfileService.updatePersonalInfo(authentication.getName(), dto);
        return ResponseEntity.ok(Map.of(
                "message", "Profile updated successfully!",
                "name", updated.getName()
        ));
    }

    @PostMapping("/profile/change-password")
    @ResponseBody
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordDto dto,
                                            Authentication authentication) {
        citizenProfileService.changePassword(authentication.getName(), dto);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully!"));
    }

    @PostMapping("/profile/upload-avatar")
    @ResponseBody
    public ResponseEntity<?> uploadAvatar(@RequestParam("image") MultipartFile image,
                                          Authentication authentication) throws IOException {
        Citizen updated = citizenProfileService.updateAvatar(authentication.getName(), image);
        return ResponseEntity.ok(Map.of(
                "message", "Profile photo updated!",
                "imagePath", imageDelivery.url(updated.getProfileImagePath(), 256)
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(Map.of("error", errors));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> handleImageUploadFailure(IOException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "We couldn't upload your image. Please try again."));
    }
}
