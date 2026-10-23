package bd.cityv1.admin.usermanagement.dto;

import bd.cityv1.citizen.register.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import bd.cityv1.validation.InputRules;

@Data
public class AddCitizenRequest {

    @NotBlank(message = "Name cannot be empty")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters.")
    private String name;

    @NotBlank(message = "Phone cannot be empty")
    @Pattern(regexp = InputRules.PHONE, message = InputRules.PHONE_MESSAGE)
    private String phone;

    @Email(message = "Enter a valid email")
    @NotBlank(message = "Email cannot be empty")
    @Size(max = 254)
    private String email;

    private String nationalId;
    @NotNull(message = "Please select a gender.")
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

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters.")
    private String password;
}
