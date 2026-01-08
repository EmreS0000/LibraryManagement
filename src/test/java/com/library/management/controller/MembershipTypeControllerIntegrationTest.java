package com.library.management.controller;

import com.library.management.entity.MembershipType;
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
@DisplayName("MembershipType Controller Integration Tests")
class MembershipTypeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MembershipTypeRepository membershipTypeRepository;

    private MembershipType testMembershipType;

    @BeforeEach
    void setUp() {
        testMembershipType = new MembershipType();
        testMembershipType.setName("Standard");
        testMembershipType.setMaxBooks(5);
        testMembershipType.setMaxLoanDays(14);
        testMembershipType = membershipTypeRepository.save(testMembershipType);
    }

    @AfterEach
    void tearDown() {
        membershipTypeRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/membership-types - Create membership type successfully")
    @WithMockUser(roles = "ADMIN")
    void testCreateMembershipType_Success() throws Exception {
        String membershipTypeJson = """
                {
                    "name": "Premium",
                    "maxBorrowLimit": 10,
                    "loanDurationDays": 30
                }
                """;

        mockMvc.perform(post("/api/v1/membership-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(membershipTypeJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Premium"))
                .andExpect(jsonPath("$.maxBorrowLimit").value(10))
                .andExpect(jsonPath("$.loanDurationDays").value(30));

        // Verify database
        List<MembershipType> types = membershipTypeRepository.findAll();
        assertThat(types).hasSize(2);
        assertThat(types).anyMatch(t -> t.getName().equals("Premium"));
    }

    @Test
    @DisplayName("POST /api/v1/membership-types - Create without admin role should fail")
    @WithMockUser(roles = "USER")
    void testCreateMembershipType_Forbidden() throws Exception {
        String membershipTypeJson = """
                {
                    "name": "Premium",
                    "maxBorrowLimit": 10,
                    "loanDurationDays": 30
                }
                """;

        mockMvc.perform(post("/api/v1/membership-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(membershipTypeJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/membership-types - Create with invalid data should return 400")
    @WithMockUser(roles = "ADMIN")
    void testCreateMembershipType_InvalidData() throws Exception {
        String invalidJson = """
                {
                    "maxBorrowLimit": 10
                }
                """;

        mockMvc.perform(post("/api/v1/membership-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/membership-types/{id} - Get membership type by ID successfully")
    @WithMockUser(roles = "ADMIN")
    void testGetMembershipType_Success() throws Exception {
        mockMvc.perform(get("/api/v1/membership-types/{id}", testMembershipType.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testMembershipType.getId()))
                .andExpect(jsonPath("$.name").value("Standard"))
                .andExpect(jsonPath("$.maxBorrowLimit").value(5))
                .andExpect(jsonPath("$.loanDurationDays").value(14));
    }

    @Test
    @DisplayName("GET /api/v1/membership-types/{id} - Get without admin role should fail")
    @WithMockUser(roles = "USER")
    void testGetMembershipType_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/membership-types/{id}", testMembershipType.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/membership-types/{id} - Get non-existent type should return 404")
    @WithMockUser(roles = "ADMIN")
    void testGetMembershipType_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/membership-types/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/membership-types - Get all membership types successfully")
    @WithMockUser(roles = "ADMIN")
    void testGetAllMembershipTypes_Success() throws Exception {
        // Create additional type
        MembershipType type2 = new MembershipType();
        type2.setName("VIP");
        type2.setMaxBooks(15);
        type2.setMaxLoanDays(60);
        membershipTypeRepository.save(type2);

        mockMvc.perform(get("/api/v1/membership-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/membership-types - Get without admin role should fail")
    @WithMockUser(roles = "USER")
    void testGetAllMembershipTypes_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/membership-types"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/membership-types - Get all when empty")
    @WithMockUser(roles = "ADMIN")
    void testGetAllMembershipTypes_Empty() throws Exception {
        membershipTypeRepository.deleteAll();

        mockMvc.perform(get("/api/v1/membership-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("PUT /api/v1/membership-types/{id} - Update membership type successfully")
    @WithMockUser(roles = "ADMIN")
    void testUpdateMembershipType_Success() throws Exception {
        String updateJson = """
                {
                    "name": "Standard Plus",
                    "maxBorrowLimit": 7,
                    "loanDurationDays": 21
                }
                """;

        mockMvc.perform(put("/api/v1/membership-types/{id}", testMembershipType.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Standard Plus"))
                .andExpect(jsonPath("$.maxBorrowLimit").value(7))
                .andExpect(jsonPath("$.loanDurationDays").value(21));

        // Verify database
        MembershipType updated = membershipTypeRepository.findById(testMembershipType.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Standard Plus");
        assertThat(updated.getMaxBooks()).isEqualTo(7);
    }

    @Test
    @DisplayName("PUT /api/v1/membership-types/{id} - Update without admin role should fail")
    @WithMockUser(roles = "USER")
    void testUpdateMembershipType_Forbidden() throws Exception {
        String updateJson = """
                {
                    "name": "Updated",
                    "maxBorrowLimit": 10,
                    "loanDurationDays": 30
                }
                """;

        mockMvc.perform(put("/api/v1/membership-types/{id}", testMembershipType.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/membership-types/{id} - Update non-existent type should return 404")
    @WithMockUser(roles = "ADMIN")
    void testUpdateMembershipType_NotFound() throws Exception {
        String updateJson = """
                {
                    "name": "Updated",
                    "maxBorrowLimit": 10,
                    "loanDurationDays": 30
                }
                """;

        mockMvc.perform(put("/api/v1/membership-types/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/membership-types/{id} - Delete membership type successfully")
    @WithMockUser(roles = "ADMIN")
    void testDeleteMembershipType_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/membership-types/{id}", testMembershipType.getId()))
                .andExpect(status().isOk());

        // Verify database
        assertThat(membershipTypeRepository.findById(testMembershipType.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/membership-types/{id} - Delete without admin role should fail")
    @WithMockUser(roles = "USER")
    void testDeleteMembershipType_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/membership-types/{id}", testMembershipType.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/membership-types/{id} - Delete non-existent type should return 404")
    @WithMockUser(roles = "ADMIN")
    void testDeleteMembershipType_NotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/membership-types/{id}", 99999L))
                .andExpect(status().isNotFound());
    }
}
