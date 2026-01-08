package com.library.management.controller;

import com.library.management.entity.Publisher;
import com.library.management.repository.PublisherRepository;
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
@DisplayName("Publisher Controller Integration Tests")
class PublisherControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PublisherRepository publisherRepository;

    private Publisher testPublisher;

    @BeforeEach
    void setUp() {
        testPublisher = new Publisher();
        testPublisher.setName("Penguin Books");
        testPublisher.setCountry("USA");
        testPublisher.setAddress("123 Publishing St, New York");
        testPublisher.setPhone("555-1234");
        testPublisher.setFoundedYear(1935);
        testPublisher = publisherRepository.save(testPublisher);
    }

    @AfterEach
    void tearDown() {
        publisherRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/publishers - Create publisher successfully")
    @WithMockUser(roles = "ADMIN")
    void testCreatePublisher_Success() throws Exception {
        String publisherJson = """
                {
                    "name": "HarperCollins",
                    "address": "456 Book Ave, London",
                    "phone": "555-5678",
                    "email": "info@harpercollins.com"
                }
                """;

        mockMvc.perform(post("/api/v1/publishers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publisherJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("HarperCollins"))
                .andExpect(jsonPath("$.address").value("456 Book Ave, London"))
                .andExpect(jsonPath("$.phone").value("555-5678"));

        // Verify database
        List<Publisher> publishers = publisherRepository.findAll();
        assertThat(publishers).hasSize(2);
        assertThat(publishers).anyMatch(p -> p.getName().equals("HarperCollins"));
    }

    @Test
    @DisplayName("POST /api/v1/publishers - Create publisher without admin role should fail")
    @WithMockUser(roles = "USER")
    void testCreatePublisher_Forbidden() throws Exception {
        String publisherJson = """
                {
                    "name": "HarperCollins",
                    "address": "456 Book Ave",
                    "phone": "555-5678"
                }
                """;

        mockMvc.perform(post("/api/v1/publishers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publisherJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/publishers - Create publisher with invalid data should return 400")
    @WithMockUser(roles = "ADMIN")
    void testCreatePublisher_InvalidData() throws Exception {
        String invalidPublisherJson = """
                {
                    "address": "Address without name"
                }
                """;

        mockMvc.perform(post("/api/v1/publishers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPublisherJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/publishers/{id} - Get publisher by ID successfully")
    @WithMockUser
    void testGetPublisherById_Success() throws Exception {
        mockMvc.perform(get("/api/v1/publishers/{id}", testPublisher.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPublisher.getId()))
                .andExpect(jsonPath("$.name").value("Penguin Books"))
                .andExpect(jsonPath("$.address").value("123 Publishing St, New York"))
                .andExpect(jsonPath("$.phone").value("555-1234"));
    }

    @Test
    @DisplayName("GET /api/v1/publishers/{id} - Get non-existent publisher should return 404")
    @WithMockUser
    void testGetPublisherById_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/publishers/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/publishers - Get all publishers successfully")
    @WithMockUser
    void testGetAllPublishers_Success() throws Exception {
        // Create additional publisher
        Publisher publisher2 = new Publisher();
        publisher2.setName("Random House");
        publisher2.setCountry("UK");
        publisher2.setFoundedYear(1927);
        publisherRepository.save(publisher2);

        mockMvc.perform(get("/api/v1/publishers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[1].name").exists());
    }

    @Test
    @DisplayName("GET /api/v1/publishers - Get all publishers when empty")
    @WithMockUser
    void testGetAllPublishers_Empty() throws Exception {
        publisherRepository.deleteAll();

        mockMvc.perform(get("/api/v1/publishers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("PUT /api/v1/publishers/{id} - Update publisher successfully")
    @WithMockUser(roles = "ADMIN")
    void testUpdatePublisher_Success() throws Exception {
        String updateJson = """
                {
                    "name": "Penguin Random House",
                    "address": "999 Updated Publishing St",
                    "phone": "555-0000",
                    "email": "updated@penguinrandomhouse.com"
                }
                """;

        mockMvc.perform(put("/api/v1/publishers/{id}", testPublisher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Penguin Random House"))
                .andExpect(jsonPath("$.address").value("999 Updated Publishing St"))
                .andExpect(jsonPath("$.phone").value("555-0000"));

        // Verify database
        Publisher updatedPublisher = publisherRepository.findById(testPublisher.getId()).orElseThrow();
        assertThat(updatedPublisher.getName()).isEqualTo("Penguin Random House");
        assertThat(updatedPublisher.getCountry()).isEqualTo("USA");
    }

    @Test
    @DisplayName("PUT /api/v1/publishers/{id} - Update publisher without admin role should fail")
    @WithMockUser(roles = "USER")
    void testUpdatePublisher_Forbidden() throws Exception {
        String updateJson = """
                {
                    "name": "Updated Publisher",
                    "address": "Updated Address",
                    "phone": "555-0000"
                }
                """;

        mockMvc.perform(put("/api/v1/publishers/{id}", testPublisher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/publishers/{id} - Update non-existent publisher should return 404")
    @WithMockUser(roles = "ADMIN")
    void testUpdatePublisher_NotFound() throws Exception {
        String updateJson = """
                {
                    "name": "Updated Publisher",
                    "address": "Updated Address",
                    "phone": "555-0000"
                }
                """;

        mockMvc.perform(put("/api/v1/publishers/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/publishers/{id} - Delete publisher successfully")
    @WithMockUser(roles = "ADMIN")
    void testDeletePublisher_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/publishers/{id}", testPublisher.getId()))
                .andExpect(status().isNoContent());

        // Verify database
        assertThat(publisherRepository.findById(testPublisher.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/publishers/{id} - Delete publisher without admin role should fail")
    @WithMockUser(roles = "USER")
    void testDeletePublisher_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/publishers/{id}", testPublisher.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/publishers/{id} - Delete non-existent publisher should return 404")
    @WithMockUser(roles = "ADMIN")
    void testDeletePublisher_NotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/publishers/{id}", 99999L))
                .andExpect(status().isNotFound());
    }
}
