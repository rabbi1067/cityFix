package bd.cityv1.complaint.citizen.dto;

import bd.cityv1.complaint.common.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import jakarta.validation.constraints.Pattern;

@Data
public class CreateComplaintRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    private String description;

    @NotBlank(message = "Category is required")
    @Pattern(regexp = "Roads|Sanitation|Lighting|Parks|Water|Other", message = "Please select a valid category.")
    private String category;

    @NotNull(message = "Priority level is required")
    private Priority priority;

    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location must be at most 255 characters.")
    private String location;
}
