package com.library.management.controller;

import com.library.management.entity.Category;
import com.library.management.repository.CategoryRepository;
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
@DisplayName("Category Controller Integration Tests")
class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setName("Fiction");
        testCategory.setDescription("Fictional books and novels");
        testCategory = categoryRepository.save(testCategory);
    }

    @AfterEach
    void tearDown() {
        categoryRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/categories - Create category successfully")
    @WithMockUser(roles = "ADMIN")
    void testCreateCategory_Success() throws Exception {
        String categoryJson = """
                {
                    "name": "Science Fiction",
                    "description": "Sci-fi books and novels"
                }
                """;

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Science Fiction"))
                .andExpect(jsonPath("$.description").value("Sci-fi books and novels"));

        // Verify database
        List<Category> categories = categoryRepository.findAll();
        assertThat(categories).hasSize(2);
        assertThat(categories).anyMatch(c -> c.getName().equals("Science Fiction"));
    }

    @Test
    @DisplayName("POST /api/v1/categories - Create category without admin role should fail")
    @WithMockUser(roles = "USER")
    void testCreateCategory_Forbidden() throws Exception {
        String categoryJson = """
                {
                    "name": "Science Fiction",
                    "description": "Sci-fi books"
                }
                """;

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/categories - Create category with invalid data should return 400")
    @WithMockUser(roles = "ADMIN")
    void testCreateCategory_InvalidData() throws Exception {
        String invalidCategoryJson = """
                {
                    "description": "Description without name"
                }
                """;

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidCategoryJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/categories/{id} - Get category by ID successfully")
    @WithMockUser
    void testGetCategoryById_Success() throws Exception {
        mockMvc.perform(get("/api/v1/categories/{id}", testCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCategory.getId()))
                .andExpect(jsonPath("$.name").value("Fiction"))
                .andExpect(jsonPath("$.description").value("Fictional books and novels"));
    }

    @Test
    @DisplayName("GET /api/v1/categories/{id} - Get non-existent category should return 404")
    @WithMockUser
    void testGetCategoryById_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/categories/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/categories - Get all categories successfully")
    @WithMockUser
    void testGetAllCategories_Success() throws Exception {
        // Create additional category
        Category category2 = new Category();
        category2.setName("Non-Fiction");
        category2.setDescription("Non-fictional books");
        categoryRepository.save(category2);

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[1].name").exists());
    }

    @Test
    @DisplayName("GET /api/v1/categories - Get all categories when empty")
    @WithMockUser
    void testGetAllCategories_Empty() throws Exception {
        categoryRepository.deleteAll();

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("PUT /api/v1/categories/{id} - Update category successfully")
    @WithMockUser(roles = "ADMIN")
    void testUpdateCategory_Success() throws Exception {
        String updateJson = """
                {
                    "name": "Updated Fiction",
                    "description": "Updated description for fiction"
                }
                """;

        mockMvc.perform(put("/api/v1/categories/{id}", testCategory.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Fiction"))
                .andExpect(jsonPath("$.description").value("Updated description for fiction"));

        // Verify database
        Category updatedCategory = categoryRepository.findById(testCategory.getId()).orElseThrow();
        assertThat(updatedCategory.getName()).isEqualTo("Updated Fiction");
        assertThat(updatedCategory.getDescription()).isEqualTo("Updated description for fiction");
    }

    @Test
    @DisplayName("PUT /api/v1/categories/{id} - Update category without admin role should fail")
    @WithMockUser(roles = "USER")
    void testUpdateCategory_Forbidden() throws Exception {
        String updateJson = """
                {
                    "name": "Updated Fiction",
                    "description": "Updated description"
                }
                """;

        mockMvc.perform(put("/api/v1/categories/{id}", testCategory.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/categories/{id} - Update non-existent category should return 404")
    @WithMockUser(roles = "ADMIN")
    void testUpdateCategory_NotFound() throws Exception {
        String updateJson = """
                {
                    "name": "Updated Fiction",
                    "description": "Updated description"
                }
                """;

        mockMvc.perform(put("/api/v1/categories/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{id} - Delete category successfully")
    @WithMockUser(roles = "ADMIN")
    void testDeleteCategory_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{id}", testCategory.getId()))
                .andExpect(status().isNoContent());

        // Verify database
        assertThat(categoryRepository.findById(testCategory.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{id} - Delete category without admin role should fail")
    @WithMockUser(roles = "USER")
    void testDeleteCategory_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{id}", testCategory.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{id} - Delete non-existent category should return 404")
    @WithMockUser(roles = "ADMIN")
    void testDeleteCategory_NotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{id}", 99999L))
                .andExpect(status().isNotFound());
    }
}
