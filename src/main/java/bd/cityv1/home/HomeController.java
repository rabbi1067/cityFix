package bd.cityv1.home;

import bd.cityv1.citizen.register.Citizen;
import bd.cityv1.citizen.register.CitizenRepository;
import bd.cityv1.complaint.common.ComplaintRepository;
import bd.cityv1.home.dto.ComplaintHomeDto;
import bd.cityv1.storage.ImageDelivery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Controller
@Slf4j
public class HomeController {

    private final CitizenRepository citizenRepository;
    private final ComplaintRepository complaintRepository;
    private final ImageDelivery imageDelivery;

    @GetMapping("/")
    public String home(Model model) {

        // Citizens
        List<Map<String, Object>> citizens = citizenRepository.findAll()
                .stream()
                .map(citizen -> Map.<String, Object>of(
                        "id", citizen.getId()
                ))
                .toList();

        // Complaints
        List<ComplaintHomeDto> complaints = complaintRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(complaint -> new ComplaintHomeDto(
                        complaint.getId(),
                        complaint.getTitle(),
                        complaint.getDescription(),
                        complaint.getCategory(),
                        complaint.getLocation(),
                        complaint.getStatus() == null
                                ? "PENDING"
                                : complaint.getStatus().name(),

                        // Send a small CDN image for homepage cards.
                        complaint.hasImage()
                                ? imageDelivery.url(complaint.getImageUrl(), 480)
                                : null,

                        complaint.getCreatedAt() == null
                                ? null
                                : complaint.getCreatedAt().toString()
                ))
                .toList();

        model.addAttribute("activePage", "home");
        model.addAttribute("citizens", citizens);
        model.addAttribute("complaints", complaints);

        return "home";
    }

    @GetMapping("/about")
    public String about(Model model) {

        model.addAttribute("activePage", "about");

        List<Long> citizens = citizenRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(Citizen::getId)
                .toList();

        List<Map<String, String>> complaints = complaintRepository
                .findAll()
                .stream()
                .map(complaint -> Map.of(
                        "status",
                        complaint.getStatus() == null
                                ? ""
                                : complaint.getStatus().name()
                ))
                .toList();

        model.addAttribute("citizens", citizens);
        model.addAttribute("complaints", complaints);

        return "about";
    }

    @GetMapping("/contact")
    public String contact(Model model) {

        model.addAttribute("activePage", "contact");

        return "contact";
    }

    @GetMapping("/login")
    public String login(Model model) {

        model.addAttribute("activePage", "login");

        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {

        model.addAttribute("activePage", "register");
        model.addAttribute("citizen", new Citizen());

        return "register";
    }
}
