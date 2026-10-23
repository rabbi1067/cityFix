package bd.cityv1.citizen.register;

import jakarta.persistence.Embeddable;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import bd.cityv1.validation.InputRules;

@Embeddable
@Data
public class CitizenAddress {
    @NotBlank(message = "Street is required.")
    @Size(max = 200, message = "Street must be at most 200 characters.")
    private String street;
    @NotBlank(message = "City is required.")
    @Size(max = 100, message = "City must be at most 100 characters.")
    private String city;
    @NotBlank(message = "Postal code is required.")
    @Pattern(regexp = InputRules.ZIP, message = InputRules.ZIP_MESSAGE)
    private String zipCode;
}
