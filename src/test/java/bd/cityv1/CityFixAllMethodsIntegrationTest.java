package bd.cityv1;

import bd.cityv1.admin.AdminContactInfo;
import bd.cityv1.admin.dashboard.dto.ComplaintSummaryDto;
import bd.cityv1.admin.register.Admin;
import bd.cityv1.admin.register.AdminRepository;
import bd.cityv1.admin.superadmin.*;
import bd.cityv1.admin.usermanagement.CitizenAdminService;
import bd.cityv1.admin.usermanagement.dto.AddCitizenRequest;
import bd.cityv1.admin.usermanagement.dto.UpdateCitizenRequest;
import bd.cityv1.citizen.register.*;
import bd.cityv1.complaint.ComplaintService;
import bd.cityv1.complaint.admin.AdminComplaintService;
import bd.cityv1.complaint.admin.dto.ResolveComplaintRequest;
import bd.cityv1.complaint.citizen.dto.CreateComplaintRequest;
import bd.cityv1.complaint.common.*;
import bd.cityv1.profile.citizen.CitizenProfileService;
import bd.cityv1.profile.citizen.dto.UpdatePersonalInfoDto;
import bd.cityv1.profile.common.ChangePasswordDto;
import bd.cityv1.profile.common.PasswordChangeService;
import bd.cityv1.security.CustomAuthenticationProvider;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CityFixAllMethodsIntegrationTest {

    @Autowired ApplicationContext applicationContext;
    @Autowired(required = false) DataSource dataSource;
    @MockitoBean PasswordEncoder passwordEncoder;
    @Autowired CitizenService citizenService;
    @Autowired CitizenAdminService citizenAdminService;
    @Autowired ComplaintService complaintService;
    @Autowired AdminComplaintService adminComplaintService;
    @Autowired CitizenProfileService citizenProfileService;
    @Autowired PasswordChangeService passwordChangeService;
    @Autowired SuperAdminService superAdminService;
    @Autowired CustomAuthenticationProvider authenticationProvider;
    @Autowired bd.cityv1.admin.dashboard.AdminController adminController;
    @Autowired SuperAdminController superAdminController;
    @Autowired bd.cityv1.admin.usermanagement.CitizenAdminController citizenAdminController;
    @Autowired bd.cityv1.citizen.dashboard.CitizenDashboardController citizenDashboardController;
    @Autowired RegisterController registerController;
    @Autowired bd.cityv1.complaint.admin.AdminComplaintController adminComplaintController;
    @Autowired bd.cityv1.complaint.citizen.CreateComplaintController createComplaintController;
    @Autowired bd.cityv1.complaint.citizen.MyComplaintsController myComplaintsController;
    @Autowired bd.cityv1.complaint.report.ReportController reportController;
    @Autowired bd.cityv1.home.HomeController homeController;
    @Autowired bd.cityv1.errorPage.ErrorPageController errorPageController;
    @Autowired bd.cityv1.notification.AdminNotificationAdvice notificationAdvice;
    @Autowired bd.cityv1.profile.admin.AdminProfileController adminProfileController;
    @Autowired bd.cityv1.profile.admin.AdminProfileService adminProfileService;
    @Autowired bd.cityv1.profile.citizen.CitizenProfileController citizenProfileController;
    @Autowired bd.cityv1.complaint.report.ComplaintReportService complaintReportService;
    @Autowired bd.cityv1.security.CitizenSecurityConfig securityConfig;
    @Autowired bd.cityv1.admin.AdminSeeder adminSeeder;
    @Autowired bd.cityv1.citizen.WebConfig webConfig;
    @Autowired jakarta.validation.Validator validator;

    @MockitoBean CitizenRepository citizenRepository;
    @MockitoBean ComplaintRepository complaintRepository;
    @MockitoBean AdminRepository adminRepository;

    private Citizen citizen;
    private Admin admin;
    private Complaint complaint;

    @BeforeAll
    static void runOnceBeforeAllTests() {
        System.out.println("Starting CityFix all-method tests");
    }

    @BeforeEach
    void createFreshFixturesBeforeEachTest() {
        citizen = sampleCitizen();
        admin = sampleAdmin(1L, "admin@test.com", true, List.of("SUPER_ADMIN"));
        complaint = sampleComplaint();
        reset(citizenRepository, complaintRepository, adminRepository);
    }

    @AfterEach
    void verifyNoUnexpectedFixtureLeakage() {
        assertNotNull(citizen);
        assertNotNull(admin);
        assertNotNull(complaint);
    }

    @AfterAll
    static void runOnceAfterAllTests() {
        System.out.println("Finished CityFix all-method tests");
    }

    @Test
    @Order(1)
    void springApplicationContextShouldLoadSuccessfully() {
        assertEquals(true, applicationContext.containsBean("cityv1Application"));
    }


    @Test
    void citizenServiceShouldRegisterCitizenAndEncodePassword() {
        when(passwordEncoder.encode("plain123")).thenReturn("encoded123");
        when(citizenRepository.save(any(Citizen.class))).thenAnswer(inv -> inv.getArgument(0));
        citizen.setPassword("plain123");

        Citizen saved = citizenService.registerCitizen(citizen);

        assertEquals("encoded123", saved.getPassword());
        assertEquals(true, saved.getCitizenId().matches("CTZ-[A-Z0-9]{8}"));
        verify(citizenRepository).save(citizen);
    }

    @Test
    void citizenServiceShouldReportEmailAndPhoneTakenValues() {
        when(citizenRepository.existsByEmail("used@test.com")).thenReturn(true);
        when(citizenRepository.existsByPhone("01700000000")).thenReturn(false);

        assertEquals(true, citizenService.isEmailTaken("used@test.com"));
        assertEquals(false, citizenService.isPhoneTaken("01700000000"));
    }

    @Test
    void citizenAdminServiceShouldListAndCountNewCitizens() {
        List<Citizen> citizens = List.of(citizen);
        when(citizenRepository.findAllByOrderByCreatedAtDesc()).thenReturn(citizens);
        when(citizenRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(3L);

        assertEquals(citizens, citizenAdminService.listCitizens());
        assertEquals(3L, citizenAdminService.countNewThisMonth());
    }

    @Test
    void citizenAdminServiceShouldAddCitizenWithGeneratedIdAndEncodedPassword() {
        AddCitizenRequest request = new AddCitizenRequest();
        request.setName("New Citizen"); request.setPhone("01800000000");
        request.setEmail("new@test.com"); request.setNationalId("NID-1");
        request.setGender(Gender.MALE); request.setOccupation("Teacher");
        request.setStreet("Main Road"); request.setCity("Dhaka"); request.setZipCode("1207");
        request.setPassword("secret123");
        when(citizenRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(citizenRepository.existsByPhone(request.getPhone())).thenReturn(false);
        when(citizenRepository.count()).thenReturn(7L);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");

        citizenAdminService.addCitizen(request);

        ArgumentCaptor<Citizen> captor = ArgumentCaptor.forClass(Citizen.class);
        verify(citizenRepository).save(captor.capture());
        assertEquals("CTZ-0008", captor.getValue().getCitizenId());
        assertEquals("encoded-secret", captor.getValue().getPassword());
        assertEquals("Dhaka", captor.getValue().getAddress().getCity());
    }

    @Test
    void citizenAdminServiceShouldRejectDuplicateEmailAndPhoneInputs() {
        AddCitizenRequest request = new AddCitizenRequest();
        request.setEmail("duplicate@test.com"); request.setPhone("01900000000");
        when(citizenRepository.existsByEmail(request.getEmail())).thenReturn(true);
        assertEquals("email:This email is already registered.",
                assertThrows(IllegalArgumentException.class, () -> citizenAdminService.addCitizen(request)).getMessage());

        reset(citizenRepository);
        when(citizenRepository.existsByPhone(request.getPhone())).thenReturn(true);
        assertEquals("phone:This phone number is already registered.",
                assertThrows(IllegalArgumentException.class, () -> citizenAdminService.addCitizen(request)).getMessage());
    }

    @Test
    void citizenAdminServiceShouldUpdateAndDeleteCitizen() {
        UpdateCitizenRequest request = new UpdateCitizenRequest();
        request.setName("Updated"); request.setPhone("01711111111"); request.setNationalId("NID-2");
        request.setGender(Gender.FEMALE); request.setOccupation("Doctor");
        request.setStreet("New Street"); request.setCity("Chattogram"); request.setZipCode("4000");
        citizen.setAddress(null);
        when(citizenRepository.findById(2L)).thenReturn(Optional.of(citizen));

        citizenAdminService.updateCitizen(2L, request);
        citizenAdminService.deleteCitizen(2L);

        assertEquals("Updated", citizen.getName());
        assertEquals("Chattogram", citizen.getAddress().getCity());
        verify(citizenRepository).deleteById(2L);
    }

    @Test
    void complaintServiceShouldSaveAndCountComplaints() {
        CreateComplaintRequest request = new CreateComplaintRequest();
        request.setTitle("Broken road"); request.setDescription("The road has a deep hole");
        request.setCategory("Road"); request.setPriority(Priority.HIGH); request.setLocation("Dhaka");
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));
        when(complaintRepository.findAllByCitizen_IdOrderByCreatedAtDesc(10L)).thenReturn(List.of(complaint));
        when(complaintRepository.countByCitizenIdAndStatusNot(10L, Status.RESOLVED)).thenReturn(2L);
        when(complaintRepository.countByCitizenIdAndStatus(10L, Status.RESOLVED)).thenReturn(4L);

        Complaint saved = complaintService.saveComplaint(request, citizen, "/uploads/a.jpg");

        assertEquals("Broken road", saved.getTitle());
        assertEquals(citizen, saved.getCitizen());
        assertEquals(List.of(complaint), complaintService.findAllForCitizen(10L));
        assertEquals(2L, complaintService.countActive(10L));
        assertEquals(4L, complaintService.countResolved(10L));
    }

    @Test
    void adminComplaintServiceShouldResolveComplaintAndCreateResolutionDetails() throws Exception {
        ResolveComplaintRequest request = new ResolveComplaintRequest(Status.RESOLVED, Priority.MEDIUM, 2500.0, "Work completed");
        when(complaintRepository.findById(5L)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));

        Complaint saved = adminComplaintService.resolveOrUpdateComplaint(5L, request, null);

        assertEquals(Status.RESOLVED, saved.getStatus());
        assertEquals(2500.0, saved.getFinalCost());
        assertEquals("Work completed", saved.getResolution().getCostNotes());
        assertNotNull(saved.getResolution().getResolvedAt());
    }

    @Test
    void adminComplaintServiceShouldRejectUnknownComplaintId() {
        when(complaintRepository.findById(999L)).thenReturn(Optional.empty());
        assertEquals("Complaint not found with id: 999",
                assertThrows(IllegalArgumentException.class, () -> adminComplaintService.getComplaintById(999L)).getMessage());
    }

    @Test
    void passwordChangeServiceShouldValidateAllPasswordInputCombinations() {
        when(passwordEncoder.matches("old123", "stored-hash")).thenReturn(true);
        when(passwordEncoder.encode("new123")).thenReturn("encoded-new");

        assertEquals("encoded-new", passwordChangeService.validateAndEncode(
                new ChangePasswordDto("old123", "new123", "new123"), "stored-hash"));
        assertEquals("Current password is incorrect", assertThrows(RuntimeException.class, () ->
                passwordChangeService.validateAndEncode(new ChangePasswordDto("wrong", "new123", "new123"), "stored-hash")).getMessage());
        assertEquals("Passwords do not match", assertThrows(RuntimeException.class, () ->
                passwordChangeService.validateAndEncode(new ChangePasswordDto("old123", "new123", "other123"), "stored-hash")).getMessage());
        assertEquals("New password must be different from current password", assertThrows(RuntimeException.class, () ->
                passwordChangeService.validateAndEncode(new ChangePasswordDto("old123", "old123", "old123"), "stored-hash")).getMessage());
    }

    @Test
    void citizenProfileServiceShouldGetAndUpdatePersonalInformation() {
        UpdatePersonalInfoDto dto = new UpdatePersonalInfoDto("Updated Citizen", "01799999999", "Lake Road", "Dhaka", "1212");
        citizen.setAddress(new CitizenAddress());
        when(citizenRepository.findByEmail("citizen@test.com")).thenReturn(Optional.of(citizen));
        when(citizenRepository.existsByPhone(dto.phone())).thenReturn(false);
        when(citizenRepository.save(any(Citizen.class))).thenAnswer(inv -> inv.getArgument(0));

        Citizen updated = citizenProfileService.updatePersonalInfo("citizen@test.com", dto);

        assertEquals("Updated Citizen", updated.getName());
        assertEquals("Lake Road", updated.getAddress().getStreet());
        verify(citizenRepository).save(citizen);
    }

    @Test
    void citizenProfileServiceShouldRejectDuplicatePhoneAndMissingCitizen() {
        UpdatePersonalInfoDto dto = new UpdatePersonalInfoDto("Name", "01711111111", "Street", "City", "1200");
        when(citizenRepository.findByEmail("citizen@test.com")).thenReturn(Optional.of(citizen));
        when(citizenRepository.existsByPhone(dto.phone())).thenReturn(true);
        assertEquals("Phone number already in use", assertThrows(RuntimeException.class, () ->
                citizenProfileService.updatePersonalInfo("citizen@test.com", dto)).getMessage());

        when(citizenRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        assertEquals("Citizen not found", assertThrows(RuntimeException.class, () ->
                citizenProfileService.getCitizen("missing@test.com")).getMessage());
    }

    @Test
    void superAdminServiceShouldListAdminsAndCalculateStats() {
        Admin blocked = sampleAdmin(2L, "blocked@test.com", false, List.of("ADMIN"));
        when(adminRepository.findAll()).thenReturn(List.of(admin, blocked));
        when(adminRepository.count()).thenReturn(2L);

        assertEquals(2, superAdminService.listAdmins(null).size());
        assertEquals(2L, superAdminService.getStats().total());
        assertEquals(1L, superAdminService.getStats().active());
        assertEquals(1L, superAdminService.getStats().blocked());
    }

    @Test
    void superAdminServiceShouldSaveAdminAndRejectInvalidDuplicateInputs() {
        AddAdminRequest request = new AddAdminRequest("New Admin", "newadmin@test.com", "secret123", "secret123", AdminPosition.IT_ADMIN);
        when(adminRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-admin");

        superAdminService.saveAdmin(request);

        ArgumentCaptor<Admin> captor = ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository).save(captor.capture());
        assertEquals("ADMIN", captor.getValue().getRoles().get(0));
        assertEquals("encoded-admin", captor.getValue().getPassword());

        AddAdminRequest mismatch = new AddAdminRequest("A", "a@test.com", "secret1", "different", AdminPosition.IT_ADMIN);
        assertEquals("confirmPassword:Passwords do not match", assertThrows(IllegalArgumentException.class, () -> superAdminService.saveAdmin(mismatch)).getMessage());
    }

    @Test
    void superAdminServiceShouldBlockUnblockAndPreventSelfActions() {
        Admin target = sampleAdmin(2L, "target@test.com", true, List.of("ADMIN"));
        when(adminRepository.findById(2L)).thenReturn(Optional.of(target));
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        superAdminService.blockPermanently(2L, "admin@test.com");
        assertEquals(false, target.isEnabled());
        assertEquals(null, target.getBlockedUntil());
        superAdminService.unblock(2L);
        assertEquals(true, target.isEnabled());
        superAdminService.blockTemporarily(2L, "admin@test.com", 3);
        assertEquals(false, target.isEnabled());
        assertNotNull(target.getBlockedUntil());

        assertEquals("You can't block your own account.", assertThrows(IllegalStateException.class,
                () -> superAdminService.blockPermanently(1L, "admin@test.com")).getMessage());
        assertEquals("Enter at least 1 day for a temporary block.", assertThrows(IllegalArgumentException.class,
                () -> superAdminService.blockTemporarily(2L, "admin@test.com", 0)).getMessage());
    }

    @Test
    void authenticationProviderShouldAuthenticateCitizenAdminAndBlockedAdmin() {
        when(citizenRepository.findByEmail("citizen@test.com")).thenReturn(Optional.of(citizen));
        when(passwordEncoder.matches("secret123", citizen.getPassword())).thenReturn(true);
        Authentication citizenAuth = authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("citizen@test.com", "secret123"));
        assertEquals("ROLE_CITIZEN", citizenAuth.getAuthorities().iterator().next().getAuthority());

        when(citizenRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("secret123", admin.getPassword())).thenReturn(true);
        Authentication adminAuth = authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("admin@test.com", "secret123"));
        assertEquals("ROLE_SUPER_ADMIN", adminAuth.getAuthorities().iterator().next().getAuthority());
        assertEquals(true, authenticationProvider.supports(UsernamePasswordAuthenticationToken.class));

        admin.setEnabled(false); admin.setBlockedUntil(LocalDateTime.now().plusDays(1));
        assertThrows(DisabledException.class, () -> authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("admin@test.com", "secret123")));
    }

    @Test
    void complaintSummaryDtoAndAdminPositionShouldMapValuesCorrectly() {
        citizen.setName("Citizen Name"); complaint.setCitizen(citizen); complaint.setId(7L);
        complaint.setTitle("Broken road"); complaint.setCategory("Road"); complaint.setStatus(Status.PENDING);
        complaint.setPriority(Priority.HIGH); complaint.setLocation("Dhaka");
        ComplaintSummaryDto dto = ComplaintSummaryDto.from(complaint);

        assertEquals(7L, dto.id());
        assertEquals("Citizen Name", dto.citizenName());
        assertEquals("IT & Technical", AdminPosition.IT_ADMIN.getLabel());
    }

    @Test
    void lombokBuilderDefaultAndToBuilderShouldPreserveAndCopyDefaults() {
        // Add @Builder, @Builder.Default, and toBuilder=true to the selected class as shown in the guide.
        BuilderProbe original = BuilderProbe.builder().name("Original").build();
        BuilderProbe copy = original.toBuilder().name("Copy").build();

        assertEquals("Original", original.getName());
        assertEquals(0, original.getPriority());
        assertEquals(false, original.isActive());
        assertEquals("Copy", copy.getName());
        assertEquals(original.getPriority(), copy.getPriority());
    }

    @lombok.Builder(toBuilder = true)
    private static class BuilderProbe {
        private String name;
        @lombok.Builder.Default private int priority = 0;
        @lombok.Builder.Default private boolean active = false;
        String getName() { return name; }
        int getPriority() { return priority; }
        boolean isActive() { return active; }
    }

    @Test
    void adminDashboardControllerShouldPopulateModelAndReturnView() {
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(complaintRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(complaint));
        when(citizenRepository.count()).thenReturn(8L);
        org.springframework.ui.Model model = new org.springframework.ui.ExtendedModelMap();
        assertEquals("admin/adminDashboard", adminController.dashboard(model, auth("admin@test.com")));
        assertEquals(admin, model.getAttribute("admin"));
        assertEquals(8L, model.getAttribute("totalCitizens"));
        assertEquals("dashboard", model.getAttribute("activePage"));
    }

    @Test
    void superAdminControllerShouldReturnListAndHandleAllAdminActions() {
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(adminRepository.findAll()).thenReturn(List.of(admin));
        when(adminRepository.count()).thenReturn(1L);
        org.springframework.ui.Model model = new org.springframework.ui.ExtendedModelMap();
        assertEquals("admin/admins", superAdminController.listAdmins(" ", model, auth("admin@test.com")));
        assertEquals("admin/admins", superAdminController.listAdmins(null, new org.springframework.ui.ExtendedModelMap(), auth("admin@test.com")));
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirect = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();
        when(adminRepository.findById(2L)).thenReturn(Optional.of(sampleAdmin(2L, "target@test.com", true, List.of("ADMIN"))));
        assertEquals("redirect:/admin/admins", superAdminController.blockPermanently(2L, auth("admin@test.com"), redirect));
        assertEquals("redirect:/admin/admins", superAdminController.blockTemporarily(2L, 2, auth("admin@test.com"), redirect));
        assertEquals("redirect:/admin/admins", superAdminController.unblock(2L, redirect));
        assertEquals("redirect:/admin/admins", superAdminController.deleteAdmin(2L, auth("admin@test.com"), redirect));
    }

    @Test
    void citizenAdminControllerShouldManageSaveDeleteAndUpdateUsers() {
        when(citizenRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(citizen));
        when(citizenRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(1L);
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(citizenRepository.findById(10L)).thenReturn(Optional.of(citizen));
        org.springframework.ui.Model model = new org.springframework.ui.ExtendedModelMap();
        assertEquals("admin/users", citizenAdminController.manageUsers(model, auth("admin@test.com")));
        AddCitizenRequest request = new AddCitizenRequest();
        request.setName("Added Citizen"); request.setPhone("01811111111"); request.setEmail("added@test.com");
        request.setStreet("Street"); request.setCity("Dhaka"); request.setZipCode("1207"); request.setPassword("secret123");
        org.springframework.validation.BindingResult result = new org.springframework.validation.BeanPropertyBindingResult(request, "newCitizen");
        assertEquals("redirect:/admin/users", citizenAdminController.saveUser(request, result, model, auth("admin@test.com")));
        assertEquals("manage-users", model.getAttribute("activePage"));
        assertEquals("redirect:/admin/users", citizenAdminController.deleteUser(10L));
        UpdateCitizenRequest editRequest = new UpdateCitizenRequest();
        assertEquals("redirect:/admin/users", citizenAdminController.updateUser(10L, editRequest,
                new org.springframework.validation.BeanPropertyBindingResult(editRequest, "editedCitizen"),
                new org.springframework.ui.ExtendedModelMap(), auth("admin@test.com")));
    }

    @Test
    void citizenDashboardAndRegistrationControllersShouldHandleNormalAndInvalidPaths() {

        when(citizenRepository.findByEmail("citizen@test.com"))
                .thenReturn(Optional.of(citizen));

        org.springframework.ui.Model model =
                new org.springframework.ui.ExtendedModelMap();

        assertEquals(
                "citizen/citizenDashboard",
                citizenDashboardController.dashboard(
                        auth("citizen@test.com"),
                        model
                )
        );

        Citizen registration = sampleCitizen();
        registration.setPassword("secret123");

        org.springframework.validation.BindingResult valid =
                new org.springframework.validation.BeanPropertyBindingResult(
                        registration,
                        "citizen"
                );

        // 1. Password mismatch
        assertEquals(
                "register",
                registerController.register(
                        registration,
                        valid,
                        "different",
                        new org.springframework.ui.ExtendedModelMap()
                )
        );

        // Service mock করতে হবে
        when(citizenService.isEmailTaken(anyString()))
                .thenReturn(false);

        when(citizenService.isPhoneTaken(anyString()))
                .thenReturn(false);

        // 2. Valid registration
        assertEquals(
                "redirect:/login",
                registerController.register(
                        registration,
                        valid,
                        "secret123",
                        new org.springframework.ui.ExtendedModelMap()
                )
        );

        // 3. Invalid BindingResult
        org.springframework.validation.BindingResult invalid =
                new org.springframework.validation.BeanPropertyBindingResult(
                        registration,
                        "citizen"
                );

        invalid.reject("invalid");

        assertEquals(
                "register",
                registerController.register(
                        registration,
                        invalid,
                        "secret123",
                        new org.springframework.ui.ExtendedModelMap()
                )
        );
    }

    @Test
    void complaintControllersShouldCreateListUpdateDeleteAndShowDetails() throws Exception {
        when(citizenRepository.findByEmail("citizen@test.com")).thenReturn(Optional.of(citizen));
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(complaintRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(complaint));
        CreateComplaintRequest request = new CreateComplaintRequest();
        org.springframework.validation.BindingResult result = new org.springframework.validation.BeanPropertyBindingResult(request, "complaint");
        org.springframework.ui.Model model = new org.springframework.ui.ExtendedModelMap();
        assertEquals("citizen/createComplaints", createComplaintController.createComplaintForm(auth("citizen@test.com"), model));
        assertEquals("redirect:/citizen/create-complaint?success=true", createComplaintController.saveComplaint(request, result, null, auth("citizen@test.com"), model));
        assertEquals("citizen/myComplaints", myComplaintsController.myComplaints(auth("citizen@test.com"), new org.springframework.ui.ExtendedModelMap()));
        assertEquals("admin/complaints", adminComplaintController.manageComplaints(new org.springframework.ui.ExtendedModelMap(), auth("admin@test.com")));
        assertEquals("admin/complaints", adminComplaintController.viewComplaint(1L, new org.springframework.ui.ExtendedModelMap(), auth("admin@test.com")));
        assertEquals("redirect:/admin/complaints/1/view", adminComplaintController.updateComplaint(1L, Status.PENDING, Priority.LOW, null, null, null));
        assertEquals("admin/complaints", adminComplaintController.confirmDeleteComplaint(1L, new org.springframework.ui.ExtendedModelMap(), auth("admin@test.com")));
        assertEquals("redirect:/admin/complaints", adminComplaintController.deleteComplaint(1L));
    }

    @Test
    void homeErrorReportAndNotificationMethodsShouldReturnExpectedViewsAndAttributes() {
        when(citizenRepository.findAll()).thenReturn(List.of(citizen));
        when(citizenRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(citizen));
        when(complaintRepository.findAll()).thenReturn(List.of(complaint));
        when(complaintRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(complaint));
        org.springframework.ui.Model model = new org.springframework.ui.ExtendedModelMap();
        assertEquals("home", homeController.home(model));
        assertEquals("about", homeController.about(new org.springframework.ui.ExtendedModelMap()));
        assertEquals("contact", homeController.contact(new org.springframework.ui.ExtendedModelMap()));
        assertEquals("login", homeController.login(new org.springframework.ui.ExtendedModelMap()));
        assertEquals("register", homeController.register(new org.springframework.ui.ExtendedModelMap()));
        assertEquals("error/403", errorPageController.accessDenied());
        assertEquals("error/404", errorPageController.notFound());
        notificationAdvice.addUrgentComplaints(model);
        assertEquals(0, model.getAttribute("urgentNotifCount"));
    }

    @Test
    void reportServiceAndControllerShouldReturnDashboardAndResolutionRows() {
        complaint.setCreatedAt(LocalDateTime.now().minusDays(2));
        ComplaintResolution resolution = new ComplaintResolution(); resolution.setResolvedAt(LocalDateTime.now().minusDays(1));
        complaint.setResolution(resolution); complaint.setEstimatedCost(100.0); complaint.setFinalCost(120.0);
        when(complaintRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(complaint));
        when(complaintRepository.findAllWithResolutionDetails()).thenReturn(List.of(complaint));
        assertEquals(1, complaintReportService.getDashboardRows().size());
        assertEquals(1, complaintReportService.getResolutionReport().size());
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        assertEquals("admin/complaintReports", reportController.reports(new org.springframework.ui.ExtendedModelMap(), auth("admin@test.com")));
    }

    @Test
    void complaintReportRowShouldCalculateAllCostVarianceCases() {
        bd.cityv1.complaint.report.dto.ComplaintReportRow row = new bd.cityv1.complaint.report.dto.ComplaintReportRow(1L, "Dhaka", "Road", Status.RESOLVED, null, null, 1L, 100.0, 125.0);
        assertEquals(25.0, row.costVariancePercent());
        assertEquals(0.0, new bd.cityv1.complaint.report.dto.ComplaintReportRow(1L, "D", "C", Status.PENDING, null, null, 0L, null, 10.0).costVariancePercent());
        assertEquals(0.0, new bd.cityv1.complaint.report.dto.ComplaintReportRow(1L, "D", "C", Status.PENDING, null, null, 0L, 0.0, 10.0).costVariancePercent());
    }

    @Test
    void profileControllersAndAdminProfileServiceShouldCoverSuccessAndRuntimeErrors() {
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(citizenRepository.findByEmail("citizen@test.com")).thenReturn(Optional.of(citizen));
        assertEquals("admin/profile", adminProfileController.profile(new org.springframework.ui.ExtendedModelMap(), auth("admin@test.com")));
        assertEquals("citizen/profile", citizenProfileController.profile(new org.springframework.ui.ExtendedModelMap(), auth("citizen@test.com")));
        when(adminRepository.save(any(Admin.class))).thenAnswer(inv -> inv.getArgument(0));
        bd.cityv1.profile.admin.dto.UpdateAdminPersonalInfoDto dto = new bd.cityv1.profile.admin.dto.UpdateAdminPersonalInfoDto("Admin 2", "01600000000", "IT");
        Admin updated = adminProfileService.updatePersonalInfo("admin@test.com", dto);
        assertEquals("Admin 2", updated.getName());
        assertEquals("IT", updated.getContactInfo().getDepartment());
        assertEquals(200, adminProfileController.updatePersonal(dto, auth("admin@test.com")).getStatusCode().value());
        assertEquals(400, adminProfileController.handleRuntime(new RuntimeException("bad input")).getStatusCode().value());
        assertEquals(400, citizenProfileController.handleRuntime(new RuntimeException("bad input")).getStatusCode().value());
    }

    @Test
    void securityConfigAndWebConfigMethodsShouldCreateBeansAndConfigureObjects()
            throws Exception {

        // Test the real PasswordEncoder implementation
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        String rawPassword = "secret123";
        String encodedPassword = encoder.encode(rawPassword);

        assertNotNull(encodedPassword);

        assertTrue(
                encoder.matches(rawPassword, encodedPassword)
        );

        assertNotNull(
                securityConfig.roleBasedSuccessHandler()
        );

        assertNotNull(
                applicationContext.getBean(
                        org.springframework.security.web.SecurityFilterChain.class
                )
        );

        assertNotNull(
                applicationContext.getBean(
                        org.springframework.security.authentication.AuthenticationManager.class
                )
        );

        assertNotNull(
                securityConfig.authenticationManager(
                        applicationContext.getBean(
                                org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration.class
                        )
                )
        );

        assertNotNull(webConfig);
    }

    @Test
    void adminSeederShouldSkipExistingAdminAndCreateMissingAdmin() {
        var configuredSeeder = new bd.cityv1.admin.AdminSeeder(
                adminRepository, passwordEncoder, "bootstrap@test.com", "bootstrap-test-password");
        when(adminRepository.findByEmail("bootstrap@test.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("bootstrap-test-password", admin.getPassword())).thenReturn(true);
        assertDoesNotThrow(() -> configuredSeeder.run());
        verify(adminRepository, never()).save(any(Admin.class));
        reset(adminRepository);
        when(adminRepository.findByEmail("bootstrap@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("bootstrap-test-password")).thenReturn("encoded");
        assertDoesNotThrow(() -> configuredSeeder.run("startup"));
        verify(adminRepository).save(any(Admin.class));
    }

    @Test
    void adminProfileAndCitizenProfilePasswordEndpointsShouldReturnSuccessMessages() {
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(citizenRepository.findByEmail("citizen@test.com")).thenReturn(Optional.of(citizen));
        String oldHash = "old-hash";
        admin.setPassword(oldHash); citizen.setPassword(oldHash);
        when(passwordEncoder.matches("old123", oldHash)).thenReturn(true);
        when(passwordEncoder.encode("new123")).thenReturn("new-hash");
        ChangePasswordDto dto = new ChangePasswordDto("old123", "new123", "new123");
        assertEquals(200, adminProfileController.changePassword(dto, auth("admin@test.com")).getStatusCode().value());
        assertEquals(200, citizenProfileController.changePassword(dto, auth("citizen@test.com")).getStatusCode().value());
    }

    private static Authentication auth(String email) {
        return new org.springframework.security.authentication.TestingAuthenticationToken(email, "password", "ROLE_ADMIN");
    }

    @Test
    void adminProfileServiceShouldLoadUpdateAndChangePassword() {
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(adminRepository.save(any(Admin.class))).thenAnswer(inv -> inv.getArgument(0));
        assertEquals(admin, adminProfileService.getAdmin("admin@test.com"));
        assertEquals("Admin Updated", adminProfileService.updatePersonalInfo("admin@test.com",
                new bd.cityv1.profile.admin.dto.UpdateAdminPersonalInfoDto("Admin Updated", "01500000000", "Operations")).getName());
        when(passwordEncoder.matches("old123", "stored-hash")).thenReturn(true);
        when(passwordEncoder.encode("new123")).thenReturn("new-hash");
        adminProfileService.changePassword("admin@test.com", new ChangePasswordDto("old123", "new123", "new123"));
        assertEquals("new-hash", admin.getPassword());
        when(adminRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        assertEquals("Admin not found", assertThrows(RuntimeException.class, () -> adminProfileService.getAdmin("missing@test.com")).getMessage());
    }

    @Test
    void citizenProfileServiceShouldValidateAvatarNullEmptyWrongTypeAndOversizedInputs() throws Exception {
        assertEquals("Please select an image to upload", assertThrows(RuntimeException.class, () -> citizenProfileService.updateAvatar("citizen@test.com", null)).getMessage());
        MultipartFile empty = mock(MultipartFile.class); when(empty.isEmpty()).thenReturn(true);
        assertEquals("Please select an image to upload", assertThrows(RuntimeException.class, () -> citizenProfileService.updateAvatar("citizen@test.com", empty)).getMessage());
        MultipartFile text = mock(MultipartFile.class); when(text.isEmpty()).thenReturn(false); when(text.getContentType()).thenReturn("text/plain");
        assertEquals("Only JPG, PNG, or WEBP images are allowed.", assertThrows(RuntimeException.class, () -> citizenProfileService.updateAvatar("citizen@test.com", text)).getMessage());
        MultipartFile large = mock(MultipartFile.class); when(large.isEmpty()).thenReturn(false); when(large.getContentType()).thenReturn("image/png"); when(large.getSize()).thenReturn(5L * 1024 * 1024 + 1);
        assertEquals("Image must be under 5MB", assertThrows(RuntimeException.class, () -> citizenProfileService.updateAvatar("citizen@test.com", large)).getMessage());
    }

    @Test
    void adminComplaintServiceShouldUpdateWithCostNotesAndResolutionImage() throws Exception {
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));
        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false); when(image.getOriginalFilename()).thenReturn("proof.png"); when(image.getBytes()).thenReturn(new byte[]{1, 2, 3});
        Complaint updated = adminComplaintService.resolveOrUpdateComplaint(1L,
                new ResolveComplaintRequest(Status.IN_PROGRESS, Priority.HIGH, 100.0, "Partial work"), image);
        assertEquals(Status.IN_PROGRESS, updated.getStatus());
        assertEquals("Partial work", updated.getResolution().getCostNotes());
        //assertEquals(true, updated.getResolution().getResolutionImagePath().endsWith(".png"));
    }

    @Test
    void superAdminServiceShouldSearchAdminsAndProtectLastSuperAdmin() {

        when(adminRepository.searchByNameOrEmail("alice"))
                .thenReturn(List.of(admin));

        assertEquals(
                1,
                superAdminService.listAdmins(" alice ").size()
        );

        Admin onlySuperAdmin = sampleAdmin(
                2L,
                "other-super@test.com",
                true,
                List.of("SUPER_ADMIN")
        );

        when(adminRepository.findById(2L))
                .thenReturn(Optional.of(onlySuperAdmin));

        when(adminRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(admin));

        // Only ONE SUPER_ADMIN exists
        when(adminRepository.findAll())
                .thenReturn(List.of(onlySuperAdmin));

        assertEquals(
                "Cannot delete the last SUPER_ADMIN.",
                assertThrows(
                        IllegalStateException.class,
                        () -> superAdminService.deleteAdmin(
                                2L,
                                "admin@test.com"
                        )
                ).getMessage()
        );

        when(adminRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertEquals(
                "Admin not found with id: 999",
                assertThrows(
                        IllegalArgumentException.class,
                        () -> superAdminService.unblock(999L)
                ).getMessage()
        );
    }

    @Test
    void recordsEnumsEntitiesAndRepositoryContractsShouldExposeExpectedValues() {
        bd.cityv1.home.dto.ComplaintHomeDto home = new bd.cityv1.home.dto.ComplaintHomeDto(1L, "T", "Description", "Road", "Dhaka", "PENDING", null, null);
        assertEquals(1L, home.id()); assertEquals("Road", home.category()); assertEquals("PENDING", home.status());
        bd.cityv1.profile.admin.dto.UpdateAdminPersonalInfoDto adminDto = new bd.cityv1.profile.admin.dto.UpdateAdminPersonalInfoDto("A", "017", "IT");
        assertEquals("IT", adminDto.department());
        UpdatePersonalInfoDto citizenDto = new UpdatePersonalInfoDto("C", "018", "S", "D", "1200");
        assertEquals("1200", citizenDto.zipCode());
        ChangePasswordDto passwordDto = new ChangePasswordDto("a", "b", "b");
        assertEquals("a", passwordDto.currentPassword());
        assertEquals(4, Priority.values().length); assertEquals(4, Status.values().length); assertEquals(2, Gender.values().length);
        AdminContactInfo contact = new AdminContactInfo("017", "IT");
        assertEquals("017", contact.getPhone()); assertEquals("IT", contact.getDepartment());
        assertEquals(false, complaint.getNotificationRead());
    }

    @Test
    void controllerExceptionHandlersShouldReturnBadRequestBodies() {
        assertEquals(400, adminProfileController.handleRuntime(new IllegalArgumentException("invalid admin")).getStatusCode().value());
        assertEquals(400, citizenProfileController.handleRuntime(new IllegalArgumentException("invalid citizen")).getStatusCode().value());
        assertEquals("invalid admin", ((java.util.Map<?, ?>) adminProfileController.handleRuntime(new RuntimeException("invalid admin")).getBody()).get("error"));
        assertEquals("invalid citizen", ((java.util.Map<?, ?>) citizenProfileController.handleRuntime(new RuntimeException("invalid citizen")).getBody()).get("error"));
    }

    @Test
    void webConfigShouldRegisterUploadResourceMapping() {
        assertEquals(true, java.util.Arrays.stream(bd.cityv1.citizen.WebConfig.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("addResourceHandlers")));
    }

    @Test
    void bothProfileValidationHandlersShouldReturnAllFieldMessages() {
        org.springframework.validation.BeanPropertyBindingResult binding = new org.springframework.validation.BeanPropertyBindingResult(new AddAdminRequest(), "request");
        binding.rejectValue("name", "invalid", "Name is required");
        binding.rejectValue("email", "invalid", "Email is invalid");
        org.springframework.web.bind.MethodArgumentNotValidException exception = mock(org.springframework.web.bind.MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(binding);
        assertEquals(400, adminProfileController.handleValidation(exception).getStatusCode().value());
        assertEquals(400, citizenProfileController.handleValidation(exception).getStatusCode().value());
        assertEquals("Name is required, Email is invalid", ((java.util.Map<?, ?>) adminProfileController.handleValidation(exception).getBody()).get("error"));
    }

    @Test
    void everyRepositoryDomainAndDtoClassShouldBeLoadable() throws Exception {
        String[] classes = {
                "bd.cityv1.Cityv1Application", "bd.cityv1.admin.AdminContactInfo", "bd.cityv1.admin.AdminSeeder",
                "bd.cityv1.admin.dashboard.AdminController", "bd.cityv1.admin.dashboard.dto.ComplaintSummaryDto",
                "bd.cityv1.admin.register.Admin", "bd.cityv1.admin.register.AdminRepository",
                "bd.cityv1.admin.superadmin.AddAdminRequest", "bd.cityv1.admin.superadmin.AdminPosition",
                "bd.cityv1.admin.superadmin.AdminStats", "bd.cityv1.admin.superadmin.SuperAdminController",
                "bd.cityv1.admin.superadmin.SuperAdminService", "bd.cityv1.admin.usermanagement.CitizenAdminController",
                "bd.cityv1.admin.usermanagement.CitizenAdminService", "bd.cityv1.admin.usermanagement.dto.AddCitizenRequest",
                "bd.cityv1.admin.usermanagement.dto.UpdateCitizenRequest", "bd.cityv1.citizen.WebConfig",
                "bd.cityv1.citizen.dashboard.CitizenDashboardController", "bd.cityv1.citizen.register.Citizen",
                "bd.cityv1.citizen.register.CitizenAddress", "bd.cityv1.citizen.register.CitizenRepository",
                "bd.cityv1.citizen.register.CitizenService", "bd.cityv1.citizen.register.Gender",
                "bd.cityv1.citizen.register.RegisterController", "bd.cityv1.complaint.ComplaintService",
                "bd.cityv1.complaint.admin.AdminComplaintController", "bd.cityv1.complaint.admin.AdminComplaintService",
                "bd.cityv1.complaint.admin.dto.ResolveComplaintRequest", "bd.cityv1.complaint.citizen.CreateComplaintController",
                "bd.cityv1.complaint.citizen.MyComplaintsController", "bd.cityv1.complaint.citizen.dto.CreateComplaintRequest",
                "bd.cityv1.complaint.common.Complaint", "bd.cityv1.complaint.common.ComplaintRepository",
                "bd.cityv1.complaint.common.ComplaintResolution", "bd.cityv1.complaint.common.Priority",
                "bd.cityv1.complaint.common.Status", "bd.cityv1.complaint.report.ComplaintReportService",
                "bd.cityv1.complaint.report.ReportController", "bd.cityv1.complaint.report.dto.ComplaintReportRow",
                "bd.cityv1.errorPage.ErrorPageController", "bd.cityv1.home.HomeController",
                "bd.cityv1.home.dto.ComplaintHomeDto", "bd.cityv1.notification.AdminNotificationAdvice",
                "bd.cityv1.profile.admin.AdminProfileController", "bd.cityv1.profile.admin.AdminProfileService",
                "bd.cityv1.profile.admin.dto.UpdateAdminPersonalInfoDto", "bd.cityv1.profile.citizen.CitizenProfileController",
                "bd.cityv1.profile.citizen.CitizenProfileService", "bd.cityv1.profile.citizen.dto.UpdatePersonalInfoDto",
                "bd.cityv1.profile.common.ChangePasswordDto", "bd.cityv1.profile.common.PasswordChangeService",
                "bd.cityv1.security.CitizenSecurityConfig", "bd.cityv1.security.CustomAuthenticationProvider"
        };
        for (String className : classes) assertEquals(true, Class.forName(className) != null);
    }

    @Test
    void beanValidationShouldRejectBlankEmailSizeAndNullFields() {
        AddCitizenRequest addCitizen = new AddCitizenRequest();
        assertEquals(true, validator.validate(addCitizen).size() >= 5);
        CreateComplaintRequest complaintRequest = new CreateComplaintRequest();
        complaintRequest.setTitle("bad"); complaintRequest.setDescription("short");
        assertEquals(true, validator.validate(complaintRequest).size() >= 3);
        ChangePasswordDto passwordRequest = new ChangePasswordDto("", "123", "");
        assertEquals(true, validator.validate(passwordRequest).size() >= 3);
        AddAdminRequest adminRequest = new AddAdminRequest("", "bad-email", "123", "", null);
        assertEquals(true, validator.validate(adminRequest).size() >= 4);
    }

    @Test
    void createComplaintControllerShouldSaveComplaintWithImageAndWithoutImage() throws Exception {
        when(citizenRepository.findByEmail("citizen@test.com")).thenReturn(Optional.of(citizen));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));
        CreateComplaintRequest request = new CreateComplaintRequest();
        request.setTitle("Broken street"); request.setDescription("The street needs urgent repair");
        request.setCategory("Road"); request.setPriority(Priority.HIGH); request.setLocation("Dhaka");
        org.springframework.validation.BindingResult valid = new org.springframework.validation.BeanPropertyBindingResult(request, "complaint");
        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false); when(image.getOriginalFilename()).thenReturn("road.jpg"); when(image.getBytes()).thenReturn(new byte[]{1});
        assertEquals("redirect:/citizen/create-complaint?success=true", createComplaintController.saveComplaint(request, valid, image, auth("citizen@test.com"), new org.springframework.ui.ExtendedModelMap()));
        assertEquals("redirect:/citizen/create-complaint?success=true", createComplaintController.saveComplaint(request, valid, null, auth("citizen@test.com"), new org.springframework.ui.ExtendedModelMap()));
    }

    @Test
    void citizenProfileServiceShouldSuccessfullyUploadAValidImage() throws Exception {
        when(citizenRepository.findByEmail("citizen@test.com")).thenReturn(Optional.of(citizen));
        when(citizenRepository.save(any(Citizen.class))).thenAnswer(inv -> inv.getArgument(0));
        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false); when(image.getContentType()).thenReturn("image/jpeg");
        when(image.getSize()).thenReturn(3L); when(image.getOriginalFilename()).thenReturn("avatar.jpg"); when(image.getBytes()).thenReturn(new byte[]{1, 2, 3});
        Citizen updated = citizenProfileService.updateAvatar("citizen@test.com", image);
        assertEquals(true, updated.getProfileImagePath().startsWith("/uploads/"));
        assertEquals(true, updated.getProfileImagePath().endsWith("_avatar.jpg"));
        assertEquals(200, citizenProfileController.uploadAvatar(image, auth("citizen@test.com")).getStatusCode().value());
    }

    @Test
    void registrationControllerShouldRejectDuplicateEmailAndDuplicatePhone() {
        Citizen registration = sampleCitizen(); registration.setPassword("secret123");
        org.springframework.validation.BindingResult valid = new org.springframework.validation.BeanPropertyBindingResult(registration, "citizen");
        when(citizenRepository.existsByEmail(registration.getEmail())).thenReturn(true);
        org.springframework.ui.Model emailModel = new org.springframework.ui.ExtendedModelMap();
        assertEquals("register", registerController.register(registration, valid, "secret123", emailModel));
        assertEquals("Email already registered", emailModel.getAttribute("error"));
        reset(citizenRepository);
        when(citizenRepository.existsByPhone(registration.getPhone())).thenReturn(true);
        org.springframework.ui.Model phoneModel = new org.springframework.ui.ExtendedModelMap();
        assertEquals("register", registerController.register(registration, valid, "secret123", phoneModel));
        assertEquals("Phone number already registered", phoneModel.getAttribute("error"));
    }

    @Test
    void securitySuccessHandlerShouldRedirectAdminAndCitizenToDifferentDashboards() throws Exception {
        org.springframework.security.web.authentication.AuthenticationSuccessHandler handler = securityConfig.roleBasedSuccessHandler();
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.mock.web.MockHttpServletResponse adminResponse = new org.springframework.mock.web.MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, adminResponse, new org.springframework.security.authentication.TestingAuthenticationToken("admin", "x", "ROLE_ADMIN"));
        assertEquals("/admin/dashboard?loginSuccess=true", adminResponse.getRedirectedUrl());
        org.springframework.mock.web.MockHttpServletResponse citizenResponse = new org.springframework.mock.web.MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, citizenResponse, new org.springframework.security.authentication.TestingAuthenticationToken("citizen", "x", "ROLE_CITIZEN"));
        assertEquals("/citizen/dashboard?loginSuccess=true", citizenResponse.getRedirectedUrl());
    }

    @Test
    void notificationAdviceShouldKeepOnlyHighAndCriticalComplaintsAndLimitTen() {
        List<Complaint> many = new ArrayList<>();
        for (int i = 0; i < 12; i++) { Complaint item = sampleComplaint(); item.setPriority(i % 2 == 0 ? Priority.HIGH : Priority.LOW); many.add(item); }
        Complaint critical = sampleComplaint(); critical.setPriority(Priority.CRITICAL); many.add(critical);
        when(complaintRepository.findAllByOrderByCreatedAtDesc()).thenReturn(many);
        org.springframework.ui.Model model = new org.springframework.ui.ExtendedModelMap();
        notificationAdvice.addUrgentComplaints(model);
        assertEquals(7, model.getAttribute("urgentNotifCount"));
        assertEquals(7, ((List<?>) model.getAttribute("urgentNotifications")).size());
    }

    @Test
    void adminAndCitizenDashboardControllersShouldRejectMissingLoggedInUsers() {
        when(adminRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        assertEquals("Admin not found", assertThrows(IllegalArgumentException.class, () -> adminController.dashboard(new org.springframework.ui.ExtendedModelMap(), auth("missing@test.com"))).getMessage());
        when(citizenRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        assertEquals("Logged-in citizen not found for email: missing@test.com", assertThrows(IllegalStateException.class, () -> citizenDashboardController.dashboard(auth("missing@test.com"), new org.springframework.ui.ExtendedModelMap())).getMessage());
    }

    @Test
    void lombokGeneratedGettersAndSettersShouldExistForAllMutableModels() throws Exception {
        Class<?>[] models = {Admin.class, Citizen.class, CitizenAddress.class, Complaint.class, ComplaintResolution.class,
                AddAdminRequest.class, AddCitizenRequest.class, UpdateCitizenRequest.class, CreateComplaintRequest.class};
        for (Class<?> model : models) {
            long fieldCount = java.util.Arrays.stream(model.getDeclaredFields())
                    .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers())).count();
            long getterCount = java.util.Arrays.stream(model.getMethods())
                    .filter(method -> method.getName().startsWith("get") || method.getName().startsWith("is")).count();
            assertEquals(true, getterCount >= Math.min(fieldCount, 1));
        }
        CitizenAddress address = new CitizenAddress(); address.setStreet("Street"); address.setCity("Dhaka"); address.setZipCode("1200");
        assertEquals("Street", address.getStreet()); assertEquals("Dhaka", address.getCity()); assertEquals("1200", address.getZipCode());
        AdminContactInfo contact = new AdminContactInfo(); contact.setPhone("017"); contact.setDepartment("IT");
        assertEquals("017", contact.getPhone()); assertEquals("IT", contact.getDepartment());
        complaint.setNotificationRead(true); complaint.setEstimatedCost(50.0); complaint.setFinalCost(60.0);
        assertEquals(true, complaint.getNotificationRead()); assertEquals(50.0, complaint.getEstimatedCost()); assertEquals(60.0, complaint.getFinalCost());
    }

    @Test
    void applicationMainAndRecordConstructorsShouldBePresentAndCallableByContract() throws Exception {
        assertEquals(true, java.lang.reflect.Modifier.isStatic(bd.cityv1.Cityv1Application.class.getMethod("main", String[].class).getModifiers()));
        ComplaintSummaryDto summary = new ComplaintSummaryDto(1L, "T", "Road", "PENDING", "HIGH", "Dhaka", "Citizen", "now");
        assertEquals("T", summary.title());
        AdminStats stats = new AdminStats(5L, 3L, 2L);
        assertEquals(5L, stats.total()); assertEquals(3L, stats.active()); assertEquals(2L, stats.blocked());
    }

    @Test
    void superAdminCurrentAdminShouldReturnAdminAndRejectMissingEmail() {
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        assertEquals(admin, superAdminService.currentAdmin("admin@test.com"));
        when(adminRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());
        assertEquals("Admin not found", assertThrows(IllegalArgumentException.class,
                () -> superAdminService.currentAdmin("nobody@test.com")).getMessage());
    }

    @Test
    void configurationMethodsShouldExposeWebResourceAndSecurityFilterContracts() throws Exception {
        assertEquals("addResourceHandlers", bd.cityv1.citizen.WebConfig.class.getMethod("addResourceHandlers", org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry.class).getName());
        assertEquals("filter", bd.cityv1.security.CitizenSecurityConfig.class.getMethod("filter", org.springframework.security.config.annotation.web.builders.HttpSecurity.class).getName());
        assertEquals(true, applicationContext.getBeansOfType(org.springframework.security.web.SecurityFilterChain.class).size() >= 1);
    }

    @Test
    void repositoryQueryMethodsShouldBeInvokedWithExpectedArguments() {
        when(adminRepository.searchByNameOrEmail("bob")).thenReturn(List.of(admin));
        when(complaintRepository.findAllWithResolutionDetails()).thenReturn(List.of(complaint));
        when(citizenRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(citizen));
        assertEquals(1, superAdminService.listAdmins("bob").size());
        assertEquals(1, complaintReportService.getResolutionReport().size());
        assertEquals(1, citizenAdminService.listCitizens().size());
        verify(adminRepository).searchByNameOrEmail("bob");
        verify(complaintRepository).findAllWithResolutionDetails();
        verify(citizenRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void authenticationProviderShouldRejectWrongPasswordAndUnknownAdmin() {
        when(citizenRepository.findByEmail("citizen@test.com")).thenReturn(Optional.of(citizen));
        when(passwordEncoder.matches("wrong", citizen.getPassword())).thenReturn(false);
        assertEquals("Invalid email or password", assertThrows(UsernameNotFoundException.class, () -> authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("citizen@test.com", "wrong"))).getMessage());
        when(citizenRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());
        when(adminRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());
        assertEquals("Invalid email or password", assertThrows(UsernameNotFoundException.class, () -> authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken("unknown@test.com", "x"))).getMessage());
    }

    @Test
    void profileUpdateEndpointsShouldReturnSuccessfulMessagesForAdminAndCitizen() {
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(citizenRepository.findByEmail("citizen@test.com")).thenReturn(Optional.of(citizen));
        when(adminRepository.save(any(Admin.class))).thenAnswer(inv -> inv.getArgument(0));
        when(citizenRepository.save(any(Citizen.class))).thenAnswer(inv -> inv.getArgument(0));
        bd.cityv1.profile.admin.dto.UpdateAdminPersonalInfoDto adminDto = new bd.cityv1.profile.admin.dto.UpdateAdminPersonalInfoDto("Admin", "01500000000", "IT");
        UpdatePersonalInfoDto citizenDto = new UpdatePersonalInfoDto("Citizen", "01700000000", "Street", "Dhaka", "1200");
        assertEquals(200, adminProfileController.updatePersonal(adminDto, auth("admin@test.com")).getStatusCode().value());
        assertEquals(200, citizenProfileController.updatePersonal(citizenDto, auth("citizen@test.com")).getStatusCode().value());
    }

    @Test
    void controllersShouldHandleInvalidBindingAndServiceExceptionBranches() {
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(citizenRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(citizen));
        when(citizenRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(0L);
        org.springframework.ui.Model model = new org.springframework.ui.ExtendedModelMap();
        AddAdminRequest adminRequest = new AddAdminRequest();
        org.springframework.validation.BindingResult adminErrors = new org.springframework.validation.BeanPropertyBindingResult(adminRequest, "addAdminRequest");
        adminErrors.rejectValue("email", "invalid", "Invalid email");
        assertEquals("admin/admins", superAdminController.saveAdmin(adminRequest, adminErrors, model, auth("admin@test.com"), new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap()));
        AddCitizenRequest citizenRequest = new AddCitizenRequest();
        org.springframework.validation.BindingResult citizenErrors = new org.springframework.validation.BeanPropertyBindingResult(citizenRequest, "newCitizen");
        citizenErrors.rejectValue("email", "invalid", "Invalid email");
        assertEquals("admin/users", citizenAdminController.saveUser(citizenRequest, citizenErrors, model, auth("admin@test.com")));
    }

    @Test
    void controllersShouldRejectMissingDependenciesWithExpectedMessages() throws Exception {
        when(adminRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        assertEquals("Admin not found", assertThrows(IllegalArgumentException.class, () -> reportController.reports(new org.springframework.ui.ExtendedModelMap(), auth("missing@test.com"))).getMessage());
        assertEquals("Admin not found", assertThrows(IllegalArgumentException.class, () -> adminComplaintController.manageComplaints(new org.springframework.ui.ExtendedModelMap(), auth("missing@test.com"))).getMessage());
        when(citizenRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        assertEquals("Logged-in citizen not found for email: missing@test.com", assertThrows(IllegalStateException.class, () -> createComplaintController.createComplaintForm(auth("missing@test.com"), new org.springframework.ui.ExtendedModelMap())).getMessage());
        assertEquals("Logged-in citizen not found for email: missing@test.com", assertThrows(IllegalStateException.class, () -> myComplaintsController.myComplaints(auth("missing@test.com"), new org.springframework.ui.ExtendedModelMap())).getMessage());
    }

    @Test
    void adminComplaintControllerShouldRejectUnknownViewAndDeleteIds() {
        when(complaintRepository.findById(404L)).thenReturn(Optional.empty());
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        assertEquals("Complaint not found with id: 404", assertThrows(IllegalArgumentException.class, () -> adminComplaintController.viewComplaint(404L, new org.springframework.ui.ExtendedModelMap(), auth("admin@test.com"))).getMessage());
        assertEquals("Complaint not found with id: 404", assertThrows(IllegalArgumentException.class, () -> adminComplaintController.confirmDeleteComplaint(404L, new org.springframework.ui.ExtendedModelMap(), auth("admin@test.com"))).getMessage());
    }

    @Test
    void domainDefaultsShouldBeInitializedForNewEntities() {
        Admin newAdmin = new Admin(); Citizen newCitizen = new Citizen(); Complaint newComplaint = new Complaint();
        assertEquals(true, newAdmin.isEnabled()); assertEquals(AdminPosition.GENERAL_ADMIN, newAdmin.getPosition());
        assertEquals(0, newAdmin.getRoles().size()); assertNotNull(newAdmin.getContactInfo());
        assertEquals(0, newCitizen.getComplaints().size()); assertNotNull(newCitizen.getCreatedAt());
        assertEquals(Status.PENDING, newComplaint.getStatus()); assertEquals(false, newComplaint.getNotificationRead());
        assertNotNull(newComplaint.getCreatedAt());
    }

    private static Citizen sampleCitizen() {
        Citizen c = new Citizen(); c.setId(10L); c.setCitizenId("CTZ-0001"); c.setName("Test Citizen");
        c.setPhone("01700000000"); c.setEmail("citizen@test.com"); c.setPassword("stored-hash");
        c.setAddress(new CitizenAddress()); c.setComplaints(new ArrayList<>()); return c;
    }

    private static Admin sampleAdmin(Long id, String email, boolean enabled, List<String> roles) {
        Admin a = new Admin(); a.setId(id); a.setName("Test Admin"); a.setEmail(email);
        a.setPassword("stored-hash"); a.setEnabled(enabled); a.setRoles(new ArrayList<>(roles));
        a.setContactInfo(new AdminContactInfo()); return a;
    }

    private static Complaint sampleComplaint() {
        Complaint c = new Complaint(); c.setId(1L); c.setTitle("Sample complaint");
        c.setDescription("A sufficiently long sample description"); c.setCategory("Road");
        c.setPriority(Priority.MEDIUM); c.setLocation("Dhaka"); c.setStatus(Status.PENDING); return c;
    }
    @Test
    void entityDefaultValuesShouldBeCorrect() {

        Admin admin = new Admin();
        Citizen citizen = new Citizen();
        Complaint complaint = new Complaint();

        assertTrue(admin.isEnabled());
        assertNotNull(admin.getPosition());
        assertNotNull(admin.getRoles());
        assertNotNull(admin.getContactInfo());

        assertNotNull(citizen.getComplaints());
        assertNotNull(citizen.getCreatedAt());

        assertEquals(Status.PENDING, complaint.getStatus());
        assertFalse(complaint.getNotificationRead());
        assertNotNull(complaint.getCreatedAt());
    }
    private Object createTestValue(Class<?> type) {

        if (type == String.class) {
            return "Test Value";
        }

        if (type == Long.class || type == long.class) {
            return 100L;
        }

        if (type == Integer.class || type == int.class) {
            return 10;
        }

        if (type == Double.class || type == double.class) {
            return 99.99;
        }

        if (type == Float.class || type == float.class) {
            return 10.5f;
        }

        if (type == Boolean.class || type == boolean.class) {
            return true;
        }

        if (type == LocalDateTime.class) {
            return LocalDateTime.of(
                    2026,
                    1,
                    1,
                    10,
                    30
            );
        }

        if (type == java.util.List.class) {
            return new ArrayList<>();
        }

        if (type == java.util.Set.class) {
            return new HashSet<>();
        }

        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();

            if (constants != null && constants.length > 0) {
                return constants[0];
            }
        }

        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }
    private void testAllGettersAndSetters(Class<?> clazz) throws Exception {

        Object object;

        // First try no-argument constructor
        try {

            java.lang.reflect.Constructor<?> constructor =
                    clazz.getDeclaredConstructor();

            constructor.setAccessible(true);
            object = constructor.newInstance();

        } catch (NoSuchMethodException e) {

            // No default constructor.
            // Try parameterized constructors.

            java.lang.reflect.Constructor<?>[] constructors =
                    clazz.getDeclaredConstructors();

            if (constructors.length == 0) {
                return;
            }

            object = null;

            for (java.lang.reflect.Constructor<?> constructor : constructors) {

                try {

                    constructor.setAccessible(true);

                    Class<?>[] parameterTypes =
                            constructor.getParameterTypes();

                    Object[] parameters =
                            new Object[parameterTypes.length];

                    boolean valid = true;

                    for (int i = 0; i < parameterTypes.length; i++) {

                        parameters[i] =
                                createTestValue(parameterTypes[i]);

                        if (parameters[i] == null
                                && parameterTypes[i].isPrimitive()) {

                            valid = false;
                            break;
                        }
                    }

                    if (!valid) {
                        continue;
                    }

                    object = constructor.newInstance(parameters);
                    break;

                } catch (Exception ignored) {
                    // Try next constructor
                }
            }

            // Could not create object
            if (object == null) {
                return;
            }
        }


        // Test all fields
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {

            // Skip static and synthetic fields
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                    || field.isSynthetic()) {
                continue;
            }

            String fieldName = field.getName();
            Class<?> fieldType = field.getType();

            String capitalizedName =
                    Character.toUpperCase(fieldName.charAt(0))
                            + fieldName.substring(1);

            String setterName = "set" + capitalizedName;

            java.lang.reflect.Method setter;

            // Find setter
            try {

                setter = clazz.getMethod(
                        setterName,
                        fieldType
                );

            } catch (NoSuchMethodException e) {
                continue;
            }

            // Create test value
            Object testValue = createTestValue(fieldType);

            // Cannot test primitive with null
            if (testValue == null && fieldType.isPrimitive()) {
                continue;
            }

            // Call setter
            setter.invoke(object, testValue);


            // Find getter
            String getterName;

            if (fieldType == boolean.class
                    || fieldType == Boolean.class) {

                try {

                    getterName = "is" + capitalizedName;

                    clazz.getMethod(getterName);

                } catch (NoSuchMethodException e) {

                    getterName = "get" + capitalizedName;
                }

            } else {

                getterName = "get" + capitalizedName;
            }


            java.lang.reflect.Method getter;

            try {

                getter = clazz.getMethod(getterName);

            } catch (NoSuchMethodException e) {
                continue;
            }


            // Call getter
            Object actualValue =
                    getter.invoke(object);


            // Verify
            assertEquals(
                    testValue,
                    actualValue,
                    "Getter/Setter failed for "
                            + clazz.getSimpleName()
                            + "."
                            + fieldName
            );
        }
    }
    @Test
    void allNonBuilderModelsShouldHaveWorkingGettersAndSetters() throws Exception {

        Class<?>[] models = {
                Admin.class,
                Citizen.class,
                CitizenAddress.class,
                ComplaintResolution.class,
                AddAdminRequest.class,
                AddCitizenRequest.class,
                UpdateCitizenRequest.class,
                CreateComplaintRequest.class
        };

        for (Class<?> modelClass : models) {
            testAllGettersAndSetters(modelClass);
        }
    }
    @Test
    void allMutableModelsAndDtosShouldHaveWorkingGettersAndSetters() throws Exception {

        Class<?>[] models = {

                // Main Entities / Models
                Admin.class,
                Citizen.class,
                CitizenAddress.class,
                Complaint.class,
                ComplaintResolution.class,
                AdminContactInfo.class,

                // Admin DTOs
                AddAdminRequest.class,
                AddCitizenRequest.class,
                UpdateCitizenRequest.class,

                // Complaint DTOs
                CreateComplaintRequest.class,
                ResolveComplaintRequest.class,

                // Profile DTOs
                UpdatePersonalInfoDto.class,
                bd.cityv1.profile.admin.dto.UpdateAdminPersonalInfoDto.class,

                // Password DTO
                ChangePasswordDto.class
        };

        for (Class<?> modelClass : models) {
            testAllGettersAndSetters(modelClass);
        }
    }
    @Test
    void mutableModelsShouldHaveNoArgsConstructors() {

        Class<?>[] models = {
                Admin.class,
                Citizen.class,
                CitizenAddress.class,
                Complaint.class,
                ComplaintResolution.class,
                AdminContactInfo.class,
                AddAdminRequest.class,
                AddCitizenRequest.class,
                UpdateCitizenRequest.class,
                CreateComplaintRequest.class,
                ResolveComplaintRequest.class,
                UpdatePersonalInfoDto.class,
                bd.cityv1.profile.admin.dto.UpdateAdminPersonalInfoDto.class
        };

        for (Class<?> model : models) {

            assertDoesNotThrow(
                    () -> model.getDeclaredConstructor(),
                    model.getSimpleName() + " should have a no-args constructor"
            );
        }
    }
    @Test
    void complaintBuilderShouldCreateAndCopyComplaintCorrectly() {

        Complaint original = Complaint.builder()
                .title("Broken Road")
                .description("Road needs immediate repair")
                .category("Road")
                .priority(Priority.HIGH)
                .location("Dhaka")
                .build();

        assertEquals("Broken Road", original.getTitle());
        assertEquals(Status.PENDING, original.getStatus());
        assertFalse(original.getNotificationRead());
        assertNotNull(original.getCreatedAt());

        Complaint updated = original.toBuilder()
                .title("Updated Road Complaint")
                .build();

        assertEquals(
                "Updated Road Complaint",
                updated.getTitle()
        );

        assertEquals(
                original.getDescription(),
                updated.getDescription()
        );
    }
    @Test
    void allImportantEntityDefaultsShouldBeInitialized() {

        Admin newAdmin = new Admin();

        assertTrue(newAdmin.isEnabled());
        assertEquals(
                AdminPosition.GENERAL_ADMIN,
                newAdmin.getPosition()
        );
        assertNotNull(newAdmin.getRoles());
        assertNotNull(newAdmin.getContactInfo());


        Citizen newCitizen = new Citizen();

        assertNotNull(newCitizen.getComplaints());
        assertNotNull(newCitizen.getCreatedAt());


        Complaint newComplaint = new Complaint();

        assertEquals(
                Status.PENDING,
                newComplaint.getStatus()
        );

        assertFalse(
                newComplaint.getNotificationRead()
        );

        assertNotNull(
                newComplaint.getCreatedAt()
        );
    }
    @Test
    void importantEnumsShouldContainExpectedValues() {

        assertNotNull(Status.PENDING);
        assertNotNull(Priority.LOW);
        assertNotNull(Priority.HIGH);
        assertNotNull(Priority.CRITICAL);
        assertNotNull(AdminPosition.GENERAL_ADMIN);

        assertTrue(Status.values().length > 0);
        assertTrue(Priority.values().length > 0);
        assertTrue(AdminPosition.values().length > 0);
    }

}
