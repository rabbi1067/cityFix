package bd.cityv1.admin;


import bd.cityv1.admin.register.Admin;
import bd.cityv1.admin.register.AdminRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@Slf4j
@Component
public class AdminSeeder implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminEmail;
    private final String adminPassword;
    private static final String INITIAL_ROLE = "SUPER_ADMIN";

    public AdminSeeder(AdminRepository adminRepository, PasswordEncoder passwordEncoder,
                       @Value("${app.bootstrap.super-admin-email:}") String adminEmail,
                       @Value("${app.bootstrap.super-admin-password:}") String adminPassword) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (adminEmail.isBlank() && adminPassword.isBlank()) {
            return;
        }
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            throw new IllegalStateException("Set both SUPER_ADMIN_EMAIL and SUPER_ADMIN_PASSWORD.");
        }

        Admin admin = adminRepository.findByEmail(adminEmail).orElse(null);
        boolean changed = admin == null;
        if (admin == null) {
            admin = new Admin();
            admin.setName("System Admin");
            admin.setEmail(adminEmail);
        }
        if (admin.getPassword() == null || !passwordEncoder.matches(adminPassword, admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(adminPassword));
            changed = true;
        }
        List<String> roles = admin.getRoles() == null ? new ArrayList<>() : new ArrayList<>(admin.getRoles());
        if (!roles.contains(INITIAL_ROLE)) {
            roles.add(INITIAL_ROLE);
            admin.setRoles(roles);
            changed = true;
        }
        if (!admin.isEnabled() || admin.getBlockedUntil() != null) {
            admin.setEnabled(true);
            admin.setBlockedUntil(null);
            changed = true;
        }

        if (changed) {
            adminRepository.save(admin);
            log.info("Configured super admin account is ready.");
        }
    }
}
