package com.library.management.controller;

import com.library.management.entity.Member;
import com.library.management.entity.MembershipType;
import com.library.management.repository.MemberRepository;
import com.library.management.repository.MembershipTypeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Auth Controller Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MembershipTypeRepository membershipTypeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MembershipType testMembershipType;

    @BeforeEach
    void setUp() {
        // Create membership type
        testMembershipType = new MembershipType();
        testMembershipType.setName("Standard");
        testMembershipType.setMaxBooks(5);
        testMembershipType.setMaxLoanDays(14);
        testMembershipType = membershipTypeRepository.save(testMembershipType);
    }

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
        membershipTypeRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Register new user successfully")
    @Transactional
    void testRegister_Success() throws Exception {
        String registerJson = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john.doe@test.com",
                    "phone": "1234567890",
                    "password": "SecurePassword123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.doe@test.com"))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").exists())
                .andExpect(jsonPath("$.id").exists());

        // Verify database
        Member savedMember = memberRepository.findByEmail("john.doe@test.com").orElseThrow();
        assertThat(savedMember.getFirstName()).isEqualTo("John");
        assertThat(savedMember.getLastName()).isEqualTo("Doe");
        assertThat(savedMember.getEmail()).isEqualTo("john.doe@test.com");
        assertThat(savedMember.getPassword()).isNotEqualTo("SecurePassword123!"); // Should be encoded
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Register with invalid email should return 400")
    void testRegister_InvalidEmail() throws Exception {
        String registerJson = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "invalid-email",
                    "phone": "1234567890",
                    "password": "SecurePassword123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Register with missing fields should return 400")
    void testRegister_MissingFields() throws Exception {
        String registerJson = """
                {
                    "firstName": "John",
                    "email": "john.doe@test.com"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Register with duplicate email should return error")
    @Transactional
    void testRegister_DuplicateEmail() throws Exception {
        // First registration
        String registerJson1 = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john.doe@test.com",
                    "phone": "1234567890",
                    "password": "SecurePassword123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson1))
                .andExpect(status().isOk());

        // Try to register again with same email
        String registerJson2 = """
                {
                    "firstName": "Jane",
                    "lastName": "Smith",
                    "email": "john.doe@test.com",
                    "phone": "9876543210",
                    "password": "AnotherPassword456!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson2))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Register with weak password should handle appropriately")
    void testRegister_WeakPassword() throws Exception {
        String registerJson = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john.doe@test.com",
                    "phone": "1234567890",
                    "password": "weak"
                }
                """;

        // Depending on password validation rules, this might return 400
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Login with valid credentials successfully")
    @Transactional
    void testLogin_Success() throws Exception {
        // First register a user
        Member member = new Member();
        member.setFirstName("John");
        member.setLastName("Doe");
        member.setEmail("john.doe@test.com");
        member.setPhone("1234567890");
        member.setPassword(passwordEncoder.encode("SecurePassword123!"));
        member.setRole(com.library.management.entity.Role.USER);
        member.setMembershipType(testMembershipType);
        memberRepository.save(member);

        // Now login
        String loginJson = """
                {
                    "email": "john.doe@test.com",
                    "password": "SecurePassword123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.doe@test.com"))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").exists())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Login with invalid password should return 401")
    @Transactional
    void testLogin_InvalidPassword() throws Exception {
        // First register a user
        Member member = new Member();
        member.setFirstName("John");
        member.setLastName("Doe");
        member.setEmail("john.doe@test.com");
        member.setPhone("1234567890");
        member.setPassword(passwordEncoder.encode("SecurePassword123!"));
        member.setRole(com.library.management.entity.Role.USER);
        member.setMembershipType(testMembershipType);
        memberRepository.save(member);

        // Try to login with wrong password
        String loginJson = """
                {
                    "email": "john.doe@test.com",
                    "password": "WrongPassword!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Login with non-existent email should return 401")
    void testLogin_NonExistentEmail() throws Exception {
        String loginJson = """
                {
                    "email": "nonexistent@test.com",
                    "password": "SomePassword123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Login with missing fields should return 400")
    void testLogin_MissingFields() throws Exception {
        String loginJson = """
                {
                    "email": "john.doe@test.com"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Login with invalid email format should return 400")
    void testLogin_InvalidEmailFormat() throws Exception {
        String loginJson = """
                {
                    "email": "invalid-email",
                    "password": "SomePassword123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Register multiple users successfully")
    @Transactional
    void testRegister_MultipleUsers() throws Exception {
        String[] emails = {"user1@test.com", "user2@test.com", "user3@test.com"};

        for (int i = 0; i < emails.length; i++) {
            String registerJson = String.format("""
                    {
                        "firstName": "User%d",
                        "lastName": "Test",
                        "email": "%s",
                        "phone": "123456789%d",
                        "password": "Password%d!"
                    }
                    """, i + 1, emails[i], i, i + 1);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(emails[i]));
        }

        // Verify all users were created
        assertThat(memberRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("POST /api/v1/auth/register then login - Full authentication flow")
    @Transactional
    void testRegisterThenLogin_FullFlow() throws Exception {
        // Register
        String registerJson = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john.doe@test.com",
                    "phone": "1234567890",
                    "password": "SecurePassword123!"
                }
                """;

        String registerResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Login with same credentials
        String loginJson = """
                {
                    "email": "john.doe@test.com",
                    "password": "SecurePassword123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.doe@test.com"))
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Register with empty password should return 400")
    void testRegister_EmptyPassword() throws Exception {
        String registerJson = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john.doe@test.com",
                    "phone": "1234567890",
                    "password": ""
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Login with empty password should return 400")
    void testLogin_EmptyPassword() throws Exception {
        String loginJson = """
                {
                    "email": "john.doe@test.com",
                    "password": ""
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isBadRequest());
    }
}
