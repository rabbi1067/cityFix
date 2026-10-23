package bd.cityv1.admin.usermanagement.dto;

import bd.cityv1.citizen.register.Gender;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import bd.cityv1.validation.InputRules;

@Data
public class UpdateCitizenRequest {

    @NotBlank(message = "Name cannot be empty")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters.")
    private String name;

    @NotBlank(message = "Phone cannot be empty")
    @Pattern(regexp = InputRules.PHONE, message = InputRules.PHONE_MESSAGE)
    private String phone;

    private String nationalId;
    private Gender gender;
    private String occupation;

    @NotBlank(message = "Street cannot be empty")
    @Size(max = 200)
    private String street;

    @NotBlank(message = "City cannot be empty")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "Zip code cannot be empty")
    @Pattern(regexp = InputRules.ZIP, message = InputRules.ZIP_MESSAGE)
    private String zipCode;
}
