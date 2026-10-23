package bd.cityv1.profile.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import bd.cityv1.validation.InputRules;

public record UpdateAdminPersonalInfoDto(
        @NotBlank(message = "Name is required") @Size(min = 2, max = 100) String name,
        @NotBlank(message = "Phone number is required") @Pattern(regexp = InputRules.PHONE, message = InputRules.PHONE_MESSAGE) String phone,
        @NotBlank(message = "Department is required") @Size(max = 100) String department
) {
    public UpdateAdminPersonalInfoDto {
        name = InputRules.text(name);
        phone = InputRules.phone(phone);
        department = InputRules.text(department);
    }
    public UpdateAdminPersonalInfoDto() {
        this(null, null, null);
    }
}
