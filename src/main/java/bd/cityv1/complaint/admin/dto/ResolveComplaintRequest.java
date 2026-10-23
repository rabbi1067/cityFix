package bd.cityv1.complaint.admin.dto;


import bd.cityv1.complaint.common.Priority;
import bd.cityv1.complaint.common.Status;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record ResolveComplaintRequest(
        @NotNull(message = "Please select a status.") Status status,
        @NotNull(message = "Please select a priority.") Priority priority,
        @DecimalMin(value = "0", message = "Final cost cannot be negative.") Double finalCost,
        @Size(max = 1000, message = "Resolution notes must be at most 1000 characters.") String costNotes
) {
    public ResolveComplaintRequest() {
        this(null, null, null, null);
    }
}
