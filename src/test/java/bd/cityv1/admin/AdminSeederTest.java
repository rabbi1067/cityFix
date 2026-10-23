package bd.cityv1.admin;

import bd.cityv1.admin.register.Admin;
import bd.cityv1.admin.register.AdminRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminSeederTest {

    private final AdminRepository admins = mock(AdminRepository.class);
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(4);
    private static final String EMAIL = "owner@example.com";
    private static final String PASSWORD = "test-bootstrap-password";

    @Test
    void createsConfiguredSuperAdminWithAHashedPassword() {
        when(admins.findByEmail(EMAIL)).thenReturn(Optional.empty());
        new AdminSeeder(admins, passwords, "  OWNER@example.com  ", PASSWORD).run();

        ArgumentCaptor<Admin> saved = ArgumentCaptor.forClass(Admin.class);
        verify(admins).save(saved.capture());
        Admin admin = saved.getValue();
        assertEquals(EMAIL, admin.getEmail());
        assertEquals(List.of("SUPER_ADMIN"), admin.getRoles());
        assertTrue(admin.isEnabled());
        assertNotEquals(PASSWORD, admin.getPassword());
        assertTrue(passwords.matches(PASSWORD, admin.getPassword()));
    }

    @Test
    void promotesAndEnablesExistingAdminAndAppliesConfiguredPassword() {
        Admin admin = new Admin();
        admin.setEmail(EMAIL);
        admin.setRoles(List.of("ADMIN"));
        admin.setPassword(passwords.encode("old-test-password"));
        admin.setEnabled(false);
        admin.setBlockedUntil(LocalDateTime.now().plusDays(1));
        when(admins.findByEmail(EMAIL)).thenReturn(Optional.of(admin));

        new AdminSeeder(admins, passwords, EMAIL, PASSWORD).run();

        verify(admins).save(admin);
        assertEquals(List.of("ADMIN", "SUPER_ADMIN"), admin.getRoles());
        assertTrue(admin.isEnabled());
        assertNull(admin.getBlockedUntil());
        assertTrue(passwords.matches(PASSWORD, admin.getPassword()));
    }

    @Test
    void repeatedStartupDoesNotRewriteAnAlreadyConfiguredAccount() {
        Admin admin = new Admin();
        admin.setEmail(EMAIL);
        admin.setRoles(List.of("SUPER_ADMIN"));
        String hash = passwords.encode(PASSWORD);
        admin.setPassword(hash);
        when(admins.findByEmail(EMAIL)).thenReturn(Optional.of(admin));

        new AdminSeeder(admins, passwords, EMAIL, PASSWORD).run();

        verify(admins, never()).save(any());
        assertEquals(hash, admin.getPassword());
    }

    @Test
    void noCredentialsDoesNotCreateAnAccountWithADefaultPassword() {
        new AdminSeeder(admins, passwords, "", "").run();
        verifyNoInteractions(admins);
    }

    @Test
    void incompleteCredentialsFailWithoutCreatingAnAccount() {
        assertThrows(IllegalStateException.class,
                () -> new AdminSeeder(admins, passwords, EMAIL, "").run());
        assertThrows(IllegalStateException.class,
                () -> new AdminSeeder(admins, passwords, "", PASSWORD).run());
        verifyNoInteractions(admins);
    }
}
