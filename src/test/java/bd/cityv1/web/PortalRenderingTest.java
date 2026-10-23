package bd.cityv1.web;

import bd.cityv1.admin.register.Admin;
import bd.cityv1.admin.register.AdminRepository;
import bd.cityv1.citizen.register.Citizen;
import bd.cityv1.citizen.register.CitizenAddress;
import bd.cityv1.citizen.register.CitizenRepository;
import bd.cityv1.complaint.common.ComplaintRepository;
import bd.cityv1.complaint.common.Complaint;
import bd.cityv1.complaint.common.ComplaintResolution;
import bd.cityv1.complaint.common.Priority;
import bd.cityv1.complaint.common.Status;
import bd.cityv1.forgotpassword.PasswordResetOtpRepository;
import bd.cityv1.storage.CloudinaryStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "app.bootstrap.super-admin-email=", "app.bootstrap.super-admin-password="
})
@AutoConfigureMockMvc
class PortalRenderingTest {

    @Autowired MockMvc mvc;
    @MockitoBean CitizenRepository citizens;
    @MockitoBean AdminRepository admins;
    @MockitoBean ComplaintRepository complaints;
    @MockitoBean PasswordResetOtpRepository otps;
    @MockitoBean PlatformTransactionManager transactions;
    @MockitoBean CloudinaryStorageService storage;
    private Citizen citizen;
    private Complaint complaint;

    @BeforeEach
    void fixtures() {
        citizen = new Citizen();
        citizen.setId(10L);
        citizen.setCitizenId("CTZ-TEST");
        citizen.setName("Test Citizen");
        citizen.setEmail("citizen@example.com");
        citizen.setPhone("01712345678");
        CitizenAddress address = new CitizenAddress();
        address.setCity("Dhaka");
        address.setStreet("Test street");
        address.setZipCode("1207");
        citizen.setAddress(address);
        citizen.setCreatedAt(LocalDateTime.now());
        when(citizens.findByEmail(citizen.getEmail())).thenReturn(Optional.of(citizen));
        when(citizens.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(citizen));
        when(citizens.findAll()).thenReturn(List.of(citizen));
        when(citizens.findById(10L)).thenReturn(Optional.of(citizen));

        complaint = new Complaint();
        complaint.setId(42L);
        complaint.setCitizen(citizen);
        complaint.setTitle("Broken road repaired");
        complaint.setDescription("The damaged road has been repaired.");
        complaint.setCategory("Roads");
        complaint.setLocation("Dhaka");
        complaint.setPriority(Priority.HIGH);
        complaint.setStatus(Status.RESOLVED);
        complaint.setFinalCost(1000.0);
        complaint.setImageUrl("https://res.cloudinary.com/demo/image/upload/v42/cityfix/before/proof.jpg");
        ComplaintResolution resolution = new ComplaintResolution();
        resolution.setResolutionImageUrl("https://res.cloudinary.com/demo/image/upload/v43/cityfix/after/proof.jpg");
        resolution.setResolvedAt(LocalDateTime.now());
        complaint.setResolution(resolution);
        citizen.setComplaints(List.of(complaint));
        when(complaints.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(complaint));
        when(complaints.findAll()).thenReturn(List.of(complaint));
        when(complaints.findAllByCitizen_IdOrderByCreatedAtDesc(10L)).thenReturn(List.of(complaint));
        when(complaints.findDetailsById(42L)).thenReturn(Optional.of(complaint));
        when(complaints.findById(42L)).thenReturn(Optional.of(complaint));

        Admin admin = new Admin();
        admin.setId(1L);
        admin.setName("Test Admin");
        admin.setEmail("admin@example.com");
        admin.setRoles(List.of("SUPER_ADMIN"));
        admin.setCreatedAt(LocalDateTime.now());
        when(admins.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(admins.findAll()).thenReturn(List.of(admin));
        when(admins.save(any(Admin.class))).thenAnswer(call -> call.getArgument(0));
        when(citizens.save(any(Citizen.class))).thenAnswer(call -> call.getArgument(0));
    }

    @ParameterizedTest
    @CsvSource({
            "/citizen/dashboard, CITIZEN, citizen@example.com, Citizen Overview",
            "/citizen/my-complaints, CITIZEN, citizen@example.com, My Complaints",
            "/citizen/create-complaint, CITIZEN, citizen@example.com, Report a New Issue",
            "/citizen/profile, CITIZEN, citizen@example.com, Test Citizen",
            "/admin/dashboard, SUPER_ADMIN, admin@example.com, Dashboard",
            "/admin/users, SUPER_ADMIN, admin@example.com, Test Citizen",
            "/admin/complaints, SUPER_ADMIN, admin@example.com, Complaints",
            "/admin/complaints/reports, SUPER_ADMIN, admin@example.com, Report",
            "/admin/profile, SUPER_ADMIN, admin@example.com, Test Admin",
            "/admin/admins, SUPER_ADMIN, admin@example.com, Test Admin"
    })
    void sidebarRoutesRenderCompletePage(String route, String role, String email, String content) throws Exception {
        var result = mvc.perform(get(route).with(user(email).roles(role)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(content)))
                .andExpect(content().string(containsString("</html>"))).andReturn();
        String previewDirectory = System.getProperty("portal.preview.dir");
        if (previewDirectory != null) {
            java.nio.file.Path output = java.nio.file.Path.of(previewDirectory, route.substring(1) + ".html");
            java.nio.file.Files.createDirectories(output.getParent());
            java.nio.file.Files.writeString(output, result.getResponse().getContentAsString());
        }
    }

    @Test
    void complaintImagesRenderInBothPortalsWithOptimizedUrls() throws Exception {
        for (String route : List.of("/citizen/my-complaints", "/admin/complaints/42/view")) {
            boolean citizenRoute = route.startsWith("/citizen");
            mvc.perform(get(route).with(user(citizenRoute ? "citizen@example.com" : "admin@example.com")
                            .roles(citizenRoute ? "CITIZEN" : "SUPER_ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("f_auto,q_auto,c_limit,w_1200/v42")))
                    .andExpect(content().string(containsString("f_auto,q_auto,c_limit,w_1200/v43")))
                    .andExpect(content().string(containsString("</html>")));
        }
    }

    @Test
    void emptyComplaintListsStillRenderDashboardAndHistory() throws Exception {
        citizen.setComplaints(List.of());
        when(complaints.findAllByCitizen_IdOrderByCreatedAtDesc(10L)).thenReturn(List.of());
        for (String route : List.of("/citizen/dashboard", "/citizen/my-complaints")) {
            mvc.perform(get(route).with(user("citizen@example.com").roles("CITIZEN")))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("</html>")));
        }
    }

    @ParameterizedTest
    @CsvSource({"/citizen/profile/update-personal,CITIZEN,citizen@example.com", "/admin/profile/update-personal,SUPER_ADMIN,admin@example.com"})
    void invalidProfileJsonIsRejectedBeforeSaving(String route, String role, String email) throws Exception {
        mvc.perform(post(route).with(user(email).roles(role)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"phone\":\"abc\",\"street\":\"\",\"city\":\"\",\"zipCode\":\"bad\",\"department\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.phone").exists());
        verify(citizens, never()).save(any());
        verify(admins, never()).save(any());
    }

    @Test
    void invalidCitizenEditKeepsTheModalAndSubmittedValues() throws Exception {
        mvc.perform(post("/admin/users/10/edit").with(user("admin@example.com").roles("SUPER_ADMIN"))
                        .param("name", "Changed Citizen").param("phone", "abc").param("street", "")
                        .param("city", "Dhaka").param("zipCode", "bad"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("editedCitizen", "phone", "street", "zipCode"))
                .andExpect(model().attribute("editCitizenId", 10L))
                .andExpect(content().string(containsString("value=\"Changed Citizen\"")))
                .andExpect(content().string(containsString("Enter a valid mobile number")));
        verify(citizens, never()).save(any());
    }

    @Test
    void validCitizenEditBindsTheAddressAndNormalizesPhone() throws Exception {
        mvc.perform(post("/admin/users/10/edit").with(user("admin@example.com").roles("SUPER_ADMIN"))
                        .param("name", "Changed Citizen").param("phone", "+880 1812345678")
                        .param("street", "New street").param("city", "Dhaka").param("zipCode", "1207"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/users"));
        verify(citizens).save(citizen);
        assertEquals("New street", citizen.getAddress().getStreet());
        assertEquals("01812345678", citizen.getPhone());
    }

    @Test
    void duplicatePhoneCannotOverwriteAnotherCitizensPhone() throws Exception {
        when(citizens.existsByPhone("01812345678")).thenReturn(true);
        mvc.perform(post("/admin/users/10/edit").with(user("admin@example.com").roles("SUPER_ADMIN"))
                        .param("name", "Changed Citizen").param("phone", "01812345678")
                        .param("street", "New street").param("city", "Dhaka").param("zipCode", "1207"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("editedCitizen", "phone"));
        verify(citizens, never()).save(any());
        assertEquals("01712345678", citizen.getPhone());
    }

    @Test
    void invalidRegistrationShowsErrorsAndCannotSave() throws Exception {
        mvc.perform(post("/register/save").param("name", "x").param("phone", "letters").param("email", "invalid")
                        .param("password", "123").param("confirmPassword", "123")
                        .param("address.street", "").param("address.city", "").param("address.zipCode", "bad"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("citizen", "name", "phone", "email", "password", "gender", "address.street", "address.zipCode"));
        verify(citizens, never()).save(any());
    }

    @Test
    void invalidAddCitizenReopensTheFormWithErrors() throws Exception {
        mvc.perform(post("/admin/users/add").with(user("admin@example.com").roles("SUPER_ADMIN"))
                        .param("name", "").param("phone", "invalid").param("password", "1"))
                .andExpect(status().isOk()).andExpect(model().attribute("showAddCitizen", true))
                .andExpect(model().attributeHasFieldErrors("newCitizen", "name", "phone", "password"));
        verify(citizens, never()).save(any());
    }

    @Test
    void invalidAddAdminCannotSave() throws Exception {
        mvc.perform(post("/admin/admins/add").with(user("admin@example.com").roles("SUPER_ADMIN"))
                        .param("name", "x").param("email", "bad").param("password", "1"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("addAdminRequest", "name", "email", "password"));
        verify(admins, never()).save(any());
    }

    @Test
    void invalidComplaintIsRejectedBeforeSaving() throws Exception {
        mvc.perform(multipart("/citizen/complaints/save").file(new MockMultipartFile("image", new byte[0]))
                        .with(user("citizen@example.com").roles("CITIZEN"))
                        .param("title", "x").param("description", "short").param("category", "forged").param("location", ""))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("complaint", "title", "description", "category", "location", "priority"));
        verify(complaints, never()).save(any());
    }

    @Test
    void nonFiniteResolutionCostIsRejected() throws Exception {
        mvc.perform(post("/admin/complaints/42/edit").with(user("admin@example.com").roles("SUPER_ADMIN"))
                        .param("status", "RESOLVED").param("priority", "HIGH").param("finalCost", "NaN"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("resolutionError"));
        verify(complaints, never()).save(any());
    }

    @Test
    void phoneAvailabilityEndpointHandlesCurrentAndTakenNumbers() throws Exception {
        mvc.perform(get("/citizen/profile/check-phone").param("phone", "+880 1712345678")
                        .with(user("citizen@example.com").roles("CITIZEN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.exists").value(false));
        when(citizens.existsByPhone("01812345678")).thenReturn(true);
        mvc.perform(get("/citizen/profile/check-phone").param("phone", "01812345678")
                        .with(user("citizen@example.com").roles("CITIZEN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    void portalsRejectTheWrongRole() throws Exception {
        mvc.perform(get("/admin/users").with(user("citizen@example.com").roles("CITIZEN")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/citizen/dashboard").with(user("admin@example.com").roles("SUPER_ADMIN")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource({"/citizen/profile/upload-avatar,CITIZEN,citizen@example.com", "/admin/profile/upload-avatar,SUPER_ADMIN,admin@example.com"})
    void profileUploadsStoreCloudinaryUrls(String route, String role, String email) throws Exception {
        MockMultipartFile avatar = new MockMultipartFile("image", "avatar.png", "image/png", new byte[]{1, 2, 3});
        when(storage.upload(any(), org.mockito.ArgumentMatchers.eq("avatars")))
                .thenReturn("https://res.cloudinary.com/demo/image/upload/v5/cityfix/avatars/avatar.png");
        mvc.perform(multipart(route).file(avatar).with(user(email).roles(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagePath").value("https://res.cloudinary.com/demo/image/upload/f_auto,q_auto,c_limit,w_256/v5/cityfix/avatars/avatar.png"));
        if ("CITIZEN".equals(role)) {
            verify(citizens).save(citizen);
            assertTrue(citizen.getProfileImagePath().startsWith("https://res.cloudinary.com/"));
        } else verify(admins).save(any(Admin.class));
    }

    @ParameterizedTest
    @CsvSource({"/citizen/profile/upload-avatar,CITIZEN,citizen@example.com", "/admin/profile/upload-avatar,SUPER_ADMIN,admin@example.com"})
    void invalidProfileUploadsCannotSave(String route, String role, String email) throws Exception {
        mvc.perform(multipart(route).file(new MockMultipartFile("image", "file.txt", "text/plain", new byte[]{1}))
                        .with(user(email).roles(role)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
        verify(storage, never()).upload(any(), any());
        verify(admins, never()).save(any());
        verify(citizens, never()).save(any());
    }
}
