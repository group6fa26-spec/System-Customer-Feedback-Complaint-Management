package com.bql.feedback.security;

import com.bql.feedback.model.AccountStatus;
import com.bql.feedback.model.Customer;
import com.bql.feedback.model.Department;
import com.bql.feedback.model.Feedback;
import com.bql.feedback.model.FeedbackType;
import com.bql.feedback.model.RentalContract;
import com.bql.feedback.model.RentalContractStatus;
import com.bql.feedback.model.ResidentType;
import com.bql.feedback.model.Role;
import com.bql.feedback.model.Staff;
import com.bql.feedback.model.UserAccount;
import com.bql.feedback.repository.CustomerRepository;
import com.bql.feedback.repository.FeedbackRepository;
import com.bql.feedback.repository.RentalContractRepository;
import com.bql.feedback.repository.StaffRepository;
import com.bql.feedback.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SecurityIntegrationTests {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"token\":\"([^\"]+)\"");
    private static final Pattern ATTACHMENT_ID_PATTERN = Pattern.compile("\"id\":(\\d+)");

    @Autowired
    private WebApplicationContext ctx;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserAccountRepository accounts;
    @Autowired
    private CustomerRepository customers;
    @Autowired
    private RentalContractRepository rentalContracts;
    @Autowired
    private StaffRepository staffRepo;
    @Autowired
    private FeedbackRepository feedbacks;
    @Autowired
    private PasswordEncoder encoder;

    private MockMvc mvc;
    private Long feedbackIdOfB;
    private String codeOfB;
    private Long managerStaffId;
    private Long pendingTenantAccountId;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
        cleanDatabase();

        Customer a = newCustomer("Cu dan A", "0900000001", "a@test.com", ResidentType.OWNER, true);
        Customer b = newCustomer("Cu dan B", "0900000002", "b@test.com", ResidentType.OWNER, true);
        Customer c = newCustomer("Cu dan C", "0900000003", "c@test.com", ResidentType.OWNER, true);
        Customer pendingTenant = newCustomer("Nguoi thue D", "0900000004", "d@test.com", ResidentType.TENANT, false);

        Staff manager = staffRepo.save(
                new Staff(null, "Quan ly BQL", Role.CS_MANAGER, Department.CUSTOMER_SERVICE, true));
        managerStaffId = manager.getId();

        newAccount("usera", Role.CUSTOMER, a.getId(), null, AccountStatus.APPROVED, true);
        newAccount("userb", Role.CUSTOMER, b.getId(), null, AccountStatus.APPROVED, true);
        newAccount("locktest", Role.CUSTOMER, c.getId(), null, AccountStatus.APPROVED, true);
        newAccount("manager01", Role.CS_MANAGER, null, manager.getId(), AccountStatus.APPROVED, true);

        rentalContracts.save(new RentalContract(
                null,
                pendingTenant.getId(),
                "A1-0501",
                LocalDate.now(),
                LocalDate.now().plusMonths(12),
                RentalContractStatus.PENDING,
                LocalDateTime.now()
        ));
        UserAccount pendingAcc = newAccount(
                "pendingtenant", Role.CUSTOMER, pendingTenant.getId(), null, AccountStatus.PENDING, false);
        pendingTenantAccountId = pendingAcc.getId();

        Feedback fb = feedbacks.save(new Feedback(
                "FB-TEST-0001", b.getId(), FeedbackType.COMPLAINT,
                "Tieu de", "Noi dung", LocalDateTime.now()));
        feedbackIdOfB = fb.getId();
        codeOfB = fb.getCode();
    }

    @AfterAll
    static void cleanupTestUploads() {
        Path uploadRoot = Paths.get("target", "test-uploads").toAbsolutePath().normalize();
        if (!Files.exists(uploadRoot) || !uploadRoot.endsWith(Paths.get("target", "test-uploads"))) {
            return;
        }
        try (Stream<Path> walk = Files.walk(uploadRoot)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM feedback_attachments");
        jdbcTemplate.execute("DELETE FROM feedback_responses");
        jdbcTemplate.execute("DELETE FROM status_histories");
        jdbcTemplate.execute("DELETE FROM escalation_histories");
        jdbcTemplate.execute("DELETE FROM satisfaction_ratings");
        jdbcTemplate.execute("DELETE FROM feedback_assignments");
        jdbcTemplate.execute("DELETE FROM feedbacks");
        jdbcTemplate.execute("DELETE FROM rental_contracts");
        jdbcTemplate.execute("DELETE FROM user_accounts");
        jdbcTemplate.execute("DELETE FROM customers");
        jdbcTemplate.execute("DELETE FROM staff");
    }

    private Customer newCustomer(String name, String phone, String email,
                                 ResidentType residentType, boolean verified) {
        Customer c = new Customer(null, name, phone, email, "A1-0501", verified, LocalDateTime.now());
        c.setResidentType(residentType);
        return customers.save(c);
    }

    private UserAccount newAccount(String user, Role role, Long customerId, Long staffId,
                                   AccountStatus status, boolean active) {
        UserAccount ua = new UserAccount(null, user, encoder.encode("Pass@123"), role,
                customerId, staffId, active, LocalDateTime.now());
        ua.setAccountStatus(status);
        return accounts.save(ua);
    }

    private String loginToken(String user) throws Exception {
        String json = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user + "\",\"password\":\"Pass@123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Matcher m = TOKEN_PATTERN.matcher(json);
        if (!m.find()) {
            throw new IllegalStateException("Không tìm thấy token trong phản hồi: " + json);
        }
        return m.group(1);
    }

    @Test
    void noTokenIs401() throws Exception {
        mvc.perform(get("/api/feedbacks/mine"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void customerCannotReadOthersFeedbackByCode() throws Exception {
        mvc.perform(get("/api/feedbacks/code/" + codeOfB)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginToken("usera")))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanReadOwnFeedbackByCode() throws Exception {
        mvc.perform(get("/api/feedbacks/code/" + codeOfB)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginToken("userb")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(codeOfB));
    }

    @Test
    void customerCannotListAllFeedbacks() throws Exception {
        mvc.perform(get("/api/feedbacks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginToken("usera")))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotReadOthersViaMineId() throws Exception {
        mvc.perform(get("/api/feedbacks/mine/" + feedbackIdOfB)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginToken("usera")))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerCanReadStaffListAndDetail() throws Exception {
        String tokenA = loginToken("usera");
        mvc.perform(get("/api/staff")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(managerStaffId));

        mvc.perform(get("/api/staff/" + managerStaffId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(managerStaffId));
    }

    @Test
    void customerCannotAccessOthersAttachments() throws Exception {
        String tokenB = loginToken("userb");
        String tokenA = loginToken("usera");

        MockMultipartFile file = new MockMultipartFile(
                "file", "proof.png", "image/png", new byte[]{1, 2, 3, 4});
        String uploadJson = mvc.perform(multipart("/api/feedbacks/" + feedbackIdOfB + "/attachments")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Matcher m = ATTACHMENT_ID_PATTERN.matcher(uploadJson);
        if (!m.find()) {
            throw new IllegalStateException("Không tìm thấy attachment id trong phản hồi: " + uploadJson);
        }
        String attachmentId = m.group(1);

        mvc.perform(get("/api/feedbacks/" + feedbackIdOfB + "/attachments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/feedbacks/" + feedbackIdOfB + "/attachments/" + attachmentId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotCallManagerEndpoint() throws Exception {
        mvc.perform(post("/api/accounts/" + pendingTenantAccountId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginToken("usera")))
                .andExpect(status().isForbidden());
    }

    @Test
    void csManagerCanApprovePendingTenantAndResidentCanLogin() throws Exception {
        String managerToken = loginToken("manager01");

        mvc.perform(post("/api/accounts/" + pendingTenantAccountId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("APPROVED"))
                .andExpect(jsonPath("$.verifiedResident").value(true))
                .andExpect(jsonPath("$.rentalContracts[0].status").value("ACTIVE"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"pendingtenant\",\"password\":\"Pass@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void lockedAfterFiveWrongPasswords() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"locktest\",\"password\":\"sai\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"locktest\",\"password\":\"Pass@123\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }
}
