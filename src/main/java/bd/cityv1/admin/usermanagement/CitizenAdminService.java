package bd.cityv1.admin.usermanagement;

import bd.cityv1.admin.usermanagement.dto.AddCitizenRequest;
import bd.cityv1.admin.usermanagement.dto.UpdateCitizenRequest;
import bd.cityv1.citizen.register.Citizen;
import bd.cityv1.citizen.register.CitizenAddress;
import bd.cityv1.citizen.register.CitizenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CitizenAdminService {

    private final CitizenRepository citizenRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Citizen> listCitizens() {
        return citizenRepository.findAllByOrderByCreatedAtDesc();
    }

    public long countNewThisMonth() {
        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        return citizenRepository.countByCreatedAtAfter(startOfMonth);
    }

    public void addCitizen(AddCitizenRequest request) {
        if (citizenRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("email:This email is already registered.");
        }
        if (citizenRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("phone:This phone number is already registered.");
        }

        Citizen citizen = new Citizen();
        citizen.setName(request.getName());
        citizen.setPhone(request.getPhone());
        citizen.setEmail(request.getEmail());
        citizen.setNationalId(request.getNationalId());
        citizen.setGender(request.getGender());
        citizen.setOccupation(request.getOccupation());

        CitizenAddress address = new CitizenAddress();
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setZipCode(request.getZipCode());
        citizen.setAddress(address);

        citizen.setCitizenId(generateCitizenId());
        citizen.setPassword(passwordEncoder.encode(request.getPassword()));
        citizen.setCreatedAt(LocalDateTime.now());

        citizenRepository.save(citizen);
    }

    public void updateCitizen(Long id, UpdateCitizenRequest request) {
        Citizen citizen = citizenRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Citizen not found with id: " + id));

        if (!java.util.Objects.equals(citizen.getPhone(), request.getPhone())
                && citizenRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("phone:This phone number is already registered.");
        }

        citizen.setName(request.getName());
        citizen.setPhone(request.getPhone());
        citizen.setNationalId(request.getNationalId());
        citizen.setGender(request.getGender());
        citizen.setOccupation(request.getOccupation());

        if (citizen.getAddress() == null) {
            citizen.setAddress(new CitizenAddress());
        }
        citizen.getAddress().setStreet(request.getStreet());
        citizen.getAddress().setCity(request.getCity());
        citizen.getAddress().setZipCode(request.getZipCode());

        citizenRepository.save(citizen);
    }

    public void deleteCitizen(Long id) {
        citizenRepository.deleteById(id);
    }

    private String generateCitizenId() {
        long nextNumber = citizenRepository.count() + 1;
        return String.format("CTZ-%04d", nextNumber);
    }
}
