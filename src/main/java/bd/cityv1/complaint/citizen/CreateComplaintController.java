package bd.cityv1.complaint.citizen;

import bd.cityv1.citizen.register.Citizen;
import bd.cityv1.citizen.register.CitizenRepository;
import bd.cityv1.complaint.ComplaintService;
import bd.cityv1.complaint.citizen.dto.CreateComplaintRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/citizen")
@RequiredArgsConstructor
public class CreateComplaintController {

    private final ComplaintService complaintService;
    private final CitizenRepository citizenRepository;

    @GetMapping("/create-complaint")
    public String createComplaintForm(Authentication authentication, Model model) {

        Citizen citizen = citizenRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Logged-in citizen not found for email: " + authentication.getName()));

        model.addAttribute("activePage", "create-complaint");
        model.addAttribute("citizen", citizen);
        model.addAttribute("complaint", new CreateComplaintRequest());

        return "citizen/createComplaints";
    }

    @PostMapping("/complaints/save")
    public String saveComplaint(@Valid @ModelAttribute("complaint") CreateComplaintRequest dto,
                                BindingResult result,
                                @RequestParam("image") MultipartFile file,
                                Authentication authentication,
                                Model model) throws IOException {

        Citizen citizen = citizenRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Logged-in citizen not found for email: " + authentication.getName()));

        if (result.hasErrors()) {
            model.addAttribute("citizen", citizen);
            model.addAttribute("activePage", "create-complaint");
            return "citizen/createComplaints";
        }

        try {
            complaintService.saveComplaint(dto, citizen, file);
        } catch (IOException | IllegalArgumentException ex) {
            model.addAttribute("citizen", citizen);
            model.addAttribute("activePage", "create-complaint");
            model.addAttribute("uploadError", ex instanceof IOException
                    ? "We couldn't upload your image. Please try again." : ex.getMessage());
            return "citizen/createComplaints";
        }

        return "redirect:/citizen/create-complaint?success=true";
    }
}
