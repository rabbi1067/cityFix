package bd.cityv1.validation;

import java.util.Locale;

public final class InputRules {
    public static final String PHONE = "01[3-9][0-9]{8}";
    public static final String PHONE_MESSAGE = "Enter a valid mobile number, e.g. 01712345678.";
    public static final String ZIP = "[0-9]{4}";
    public static final String ZIP_MESSAGE = "Enter a valid 4-digit postal code.";

    private InputRules() {}

    public static String text(String value) {
        return value == null ? null : value.trim();
    }

    public static String email(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    public static String phone(String value) {
        if (value == null) return null;
        String normalized = value.replaceAll("[\\s()\\-]", "");
        if (normalized.startsWith("+880")) return normalized.substring(3);
        if (normalized.startsWith("880")) return normalized.substring(2);
        return normalized;
    }
}
