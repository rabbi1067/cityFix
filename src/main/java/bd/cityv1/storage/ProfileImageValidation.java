package bd.cityv1.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

public final class ProfileImageValidation {
    private ProfileImageValidation() {}

    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Please select an image to upload");
        String contentType = file.getContentType();
        if (contentType == null || !Set.of("image/jpeg", "image/png", "image/webp")
                .contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only JPG, PNG, or WEBP images are allowed.");
        }
        if (file.getSize() > 5 * 1024 * 1024) throw new IllegalArgumentException("Image must be under 5MB");
    }
}
