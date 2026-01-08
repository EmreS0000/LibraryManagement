package com.library.management.controller;

import com.library.management.entity.Member;
import com.library.management.entity.MembershipType;
import com.library.management.entity.Role;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Member Controller Integration Tests")
class MemberControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MembershipTypeRepository membershipTypeRepository;

    private Member testMember;
    private MembershipType testMembershipType;

    @BeforeEach
    void setUp() {
        // Create membership type
        testMembershipType = new MembershipType();
        testMembershipType.setName("Standard");
        testMembershipType.setMaxBooks(5);
        testMembershipType.setMaxLoanDays(14);
        testMembershipType = membershipTypeRepository.save(testMembershipType);

        // Create test member
        testMember = new Member();
        testMember.setFirstName("John");
        testMember.setLastName("Doe");
        testMember.setEmail("john.doe@test.com");
        testMember.setPhone("1234567890");
        testMember.setPassword("encodedPassword"); // avoid NOT NULL constraint
        testMember.setRole(Role.USER);
        testMember.setMembershipType(testMembershipType);
        testMember = memberRepository.save(testMember);
    }

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
        membershipTypeRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/members - Create member successfully")
    @WithMockUser
    void testCreateMember_Success() throws Exception {
        String memberJson = String.format("""
                {
                    "firstName": "Jane",
                    "lastName": "Smith",
                    "email": "jane.smith@test.com",
                    "phone": "9876543210",
                    "address": "456 Test Ave",
                    "membershipType": {"id": %d}
                }
                """, testMembershipType.getId());

        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.email").value("jane.smith@test.com"));

        // Verify database
        List<Member> members = memberRepository.findAll();
        assertThat(members).hasSize(2);
        assertThat(members).anyMatch(m -> m.getEmail().equals("jane.smith@test.com"));
    }

    @Test
    @DisplayName("POST /api/v1/members - Create member with invalid email should return 400")
    @WithMockUser
    void testCreateMember_InvalidEmail() throws Exception {
        String memberJson = """
                {
                    "firstName": "Jane",
                    "lastName": "Smith",
                    "email": "invalid-email",
                    "phone": "9876543210"
                }
                """;

        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/members - Create member with missing required fields should return 400")
    @WithMockUser
    void testCreateMember_MissingFields() throws Exception {
        String memberJson = """
                {
                    "firstName": "Jane"
                }
                """;

        mockMvc.perform(post("/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/members/{id} - Get member by ID successfully")
    @WithMockUser
    void testGetMemberById_Success() throws Exception {
        mockMvc.perform(get("/api/v1/members/{id}", testMember.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testMember.getId()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@test.com"))
                .andExpect(jsonPath("$.phone").value("1234567890"));
    }

    @Test
    @DisplayName("GET /api/v1/members/{id} - Get non-existent member should return 404")
    @WithMockUser
    void testGetMemberById_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/members/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/members - Get all members successfully")
    @WithMockUser
    void testGetAllMembers_Success() throws Exception {
        // Create additional member
        Member member2 = new Member();
        member2.setFirstName("Alice");
        member2.setLastName("Johnson");
        member2.setEmail("alice.johnson@test.com");
        member2.setPhone("5555555555");
        member2.setMembershipType(testMembershipType);
        memberRepository.save(member2);

        mockMvc.perform(get("/api/v1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/members - Get all members when empty")
    @WithMockUser
    void testGetAllMembers_Empty() throws Exception {
        memberRepository.deleteAll();

        mockMvc.perform(get("/api/v1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("PUT /api/v1/members/{id} - Update member successfully")
    @WithMockUser
    void testUpdateMember_Success() throws Exception {
        String updateJson = String.format("""
                {
                    "firstName": "John Updated",
                    "lastName": "Doe Updated",
                    "email": "john.updated@test.com",
                    "phone": "1111111111",
                    "address": "999 Updated St",
                    "membershipType": {"id": %d}
                }
                """, testMembershipType.getId());

        mockMvc.perform(put("/api/v1/members/{id}", testMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John Updated"))
                .andExpect(jsonPath("$.lastName").value("Doe Updated"))
                .andExpect(jsonPath("$.email").value("john.updated@test.com"));

        // Verify database
        Member updatedMember = memberRepository.findById(testMember.getId()).orElseThrow();
        assertThat(updatedMember.getFirstName()).isEqualTo("John Updated");
        assertThat(updatedMember.getPhone()).isEqualTo("1111111111");
    }

    @Test
    @DisplayName("PUT /api/v1/members/{id} - Update non-existent member should return 404")
    @WithMockUser
    void testUpdateMember_NotFound() throws Exception {
        String updateJson = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john@test.com",
                    "phone": "1234567890"
                }
                """;

        mockMvc.perform(put("/api/v1/members/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/members/{id} - Delete member successfully")
    @WithMockUser
    void testDeleteMember_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/members/{id}", testMember.getId()))
                .andExpect(status().isNoContent());

        // Verify database
        assertThat(memberRepository.findById(testMember.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/members/{id} - Delete non-existent member should return 404")
    @WithMockUser
    void testDeleteMember_NotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/members/{id}", 99999L))
                .andExpect(status().isNotFound());
    }
}
