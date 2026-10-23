package bd.cityv1.citizen.register;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.WebDataBinder;

@Controller
@RequiredArgsConstructor
public class RegisterController {

    private final CitizenService citizenService;

    @InitBinder("citizen")
    public void registrationFields(WebDataBinder binder) {
        binder.setAllowedFields("name", "email", "phone", "nationalId", "gender", "occupation", "password",
                "address.street", "address.city", "address.zipCode");
    }

    @PostMapping("/register/save")
    public String register(@Valid @ModelAttribute("citizen") Citizen citizen,
                           BindingResult result,
                           @RequestParam(value = "confirmPassword", defaultValue = "") String confirmPassword,
                           Model model) {

        if (citizen.getAddress() == null) result.rejectValue("address", "required", "Address is required.");
        if (citizen.getGender() == null) result.rejectValue("gender", "required", "Please select a gender.");
        if (result.hasErrors()) {
            return "register";
        }

        if (!citizen.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            return "register";
        }

        if (citizenService.isEmailTaken(citizen.getEmail())) {
            model.addAttribute("error", "Email already registered");
            return "register";
        }

        if (citizenService.isPhoneTaken(citizen.getPhone())) {
            model.addAttribute("error", "Phone number already registered");
            return "register";
        }

        citizenService.registerCitizen(citizen);

        return "redirect:/login";
    }
}
