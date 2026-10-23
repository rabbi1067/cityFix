package bd.cityv1.profile.citizen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import bd.cityv1.validation.InputRules;

public record UpdatePersonalInfoDto(
        @NotBlank(message = "Name cannot be empty") @Size(min = 2, max = 100) String name,
        @NotBlank(message = "Phone cannot be empty") @Pattern(regexp = InputRules.PHONE, message = InputRules.PHONE_MESSAGE) String phone,
        @NotBlank(message = "Street cannot be empty") @Size(max = 200) String street,
        @NotBlank(message = "City cannot be empty") @Size(max = 100) String city,
        @NotBlank(message = "Zip code cannot be empty") @Pattern(regexp = InputRules.ZIP, message = InputRules.ZIP_MESSAGE) String zipCode
) {
    public UpdatePersonalInfoDto {
        name = InputRules.text(name);
        phone = InputRules.phone(phone);
        street = InputRules.text(street);
        city = InputRules.text(city);
        zipCode = InputRules.text(zipCode);
    }
    public UpdatePersonalInfoDto() {
        this(null, null, null, null, null);
    }
}
