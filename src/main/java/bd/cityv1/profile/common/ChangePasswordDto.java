package bd.cityv1.profile.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordDto(
        @NotBlank(message = "Current password is required") String currentPassword,
        @NotBlank(message = "New password is required")
        @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters") String newPassword,
        @NotBlank(message = "Confirm password is required") String confirmPassword
) {}
