package bd.cityv1.storage;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("imageDelivery")
public class ImageDelivery {

    private static final Pattern CLOUDINARY_IMAGE = Pattern.compile(
            "^(https://res\\.cloudinary\\.com/[a-zA-Z0-9_-]+/image/upload/)(v\\d+/.*)$");

    public String url(String originalUrl, int width) {
        if (originalUrl == null || originalUrl.isBlank()) return originalUrl;
        Matcher matcher = CLOUDINARY_IMAGE.matcher(originalUrl);
        if (!matcher.matches()) return originalUrl;
        int safeWidth = Math.max(100, Math.min(width, 1920));
        return matcher.group(1) + "f_auto,q_auto,c_limit,w_" + safeWidth + "/" + matcher.group(2);
    }
}
