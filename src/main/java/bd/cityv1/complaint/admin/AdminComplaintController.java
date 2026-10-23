package bd.cityv1.complaint.admin;



import bd.cityv1.admin.register.Admin;
import bd.cityv1.admin.register.AdminRepository;

import bd.cityv1.complaint.admin.dto.ResolveComplaintRequest;
import bd.cityv1.complaint.common.Complaint;
import bd.cityv1.complaint.common.ComplaintRepository;
import bd.cityv1.complaint.common.Priority;
import bd.cityv1.complaint.common.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminComplaintController {

    private final ComplaintRepository complaintRepository;
    private final AdminRepository adminRepository;
    private final AdminComplaintService adminComplaintService;

    private void addSharedComplaintAttributes(Model model, Authentication authentication) {
        Admin admin = adminRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        model.addAttribute("admin", admin);

        model.addAttribute("complaints", complaintRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/complaints")
    public String manageComplaints(Model model, Authentication authentication) {

        addSharedComplaintAttributes(model, authentication);

        model.addAttribute("view", "list");
        model.addAttribute("activePage", "manage-complaints");

        return "admin/complaints";
    }

    @GetMapping("/complaints/{id}/view")
    public String viewComplaint(@PathVariable Long id, Model model, Authentication authentication) {

        Complaint complaint = complaintRepository.findDetailsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found with id: " + id));

        addSharedComplaintAttributes(model, authentication);

        model.addAttribute("view", "details");
        model.addAttribute("complaint", complaint);

        return "admin/complaints";
    }

    @PostMapping("/complaints/{id}/edit")
    public String updateComplaint(@PathVariable Long id,
                                  @RequestParam Status status,
                                  @RequestParam Priority priority,
                                  @RequestParam(required = false) Double finalCost,
                                  @RequestParam(required = false) String costNotes,
                                  @RequestParam(required = false) MultipartFile resolutionImage,
                                  RedirectAttributes redirectAttributes) throws IOException {

        ResolveComplaintRequest request = new ResolveComplaintRequest(status, priority, finalCost, costNotes);

        try {
            adminComplaintService.resolveOrUpdateComplaint(id, request, resolutionImage);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("resolutionError", ex.getMessage());
        } catch (IOException ex) {
            redirectAttributes.addFlashAttribute("resolutionError",
                    "We couldn't upload your image. Please try again.");
        }
        return "redirect:/admin/complaints/" + id + "/view";
    }

    @GetMapping("/complaints/{id}/delete")
    public String confirmDeleteComplaint(@PathVariable Long id, Model model, Authentication authentication) {

        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found with id: " + id));

        addSharedComplaintAttributes(model, authentication);

        model.addAttribute("view", "delete");
        model.addAttribute("complaint", complaint);

        return "admin/complaints";
    }

    public String updateComplaint(Long id,
                                  Status status,
                                  Priority priority,
                                  Double finalCost,
                                  String costNotes,
                                  MultipartFile resolutionImage) throws IOException {
        ResolveComplaintRequest request = new ResolveComplaintRequest(
                status, priority, finalCost, costNotes);
        adminComplaintService.resolveOrUpdateComplaint(id, request, resolutionImage);
        return "redirect:/admin/complaints/" + id + "/view";
    }

    @PostMapping("/complaints/{id}/delete")
    public String deleteComplaint(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {
        if (!complaintRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute(
                    "deleteError", "Complaint not found.");
            return "redirect:/admin/complaints";
        }

        complaintRepository.deleteById(id);
        redirectAttributes.addFlashAttribute(
                "deleteSuccess", "Complaint deleted successfully.");
        return "redirect:/admin/complaints";
    }

    public String deleteComplaint(Long id) {
        complaintRepository.deleteById(id);
        return "redirect:/admin/complaints";
    }
}
