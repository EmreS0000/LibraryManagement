package com.library.management.controller;

import com.library.management.entity.*;
import com.library.management.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Favorite Controller Integration Tests")
class FavoriteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private MembershipTypeRepository membershipTypeRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    private Member testMember;
    private Book testBook;
    private Favorite testFavorite;

    @BeforeEach
    void setUp() {
        // Create membership type
        MembershipType membershipType = new MembershipType();
        membershipType.setName("Standard");
        membershipType.setMaxBooks(5);
        membershipType.setMaxLoanDays(14);
        membershipType = membershipTypeRepository.save(membershipType);

        // Create member
        testMember = new Member();
        testMember.setFirstName("John");
        testMember.setLastName("Doe");
        testMember.setEmail("john.doe@test.com");
        testMember.setPhone("1234567890");
        testMember.setMembershipType(membershipType);
        testMember = memberRepository.save(testMember);

        // Create publisher
        Publisher publisher = new Publisher();
        publisher.setName("Test Publisher");
        publisher.setCountry("USA");
        publisher.setFoundedYear(2000);
        publisher = publisherRepository.save(publisher);

        // Create book
        testBook = new Book();
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setIsbn("TEST-ISBN-123");
        testBook.setPublisher(publisher);
        testBook = bookRepository.save(testBook);

        // Create favorite
        testFavorite = new Favorite();
        testFavorite.setMember(testMember);
        testFavorite.setBook(testBook);
        testFavorite = favoriteRepository.save(testFavorite);
    }

    @AfterEach
    void tearDown() {
        favoriteRepository.deleteAll();
        bookRepository.deleteAll();
        memberRepository.deleteAll();
        membershipTypeRepository.deleteAll();
        publisherRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/favorites/{bookId} - Add favorite successfully")
    @WithMockUser(username = "john.doe@test.com", roles = "MEMBER")
    @Transactional
    void testAddFavorite_Success() throws Exception {
        // Create another book to add as favorite
        Book book2 = new Book();
        book2.setTitle("Another Book");
        book2.setAuthor("Another Author");
        book2.setIsbn("ANOTHER-ISBN-456");
        book2.setPublisher(testBook.getPublisher());
        Book savedBook2 = bookRepository.save(book2);

        mockMvc.perform(post("/api/v1/favorites/{bookId}", savedBook2.getId()))
                .andExpect(status().isOk());

        // Verify database
        List<Favorite> favorites = favoriteRepository.findAll();
        assertThat(favorites).hasSizeGreaterThanOrEqualTo(2);
        assertThat(favorites).anyMatch(f -> f.getBook().getId().equals(savedBook2.getId()));
    }

    @Test
    @DisplayName("POST /api/v1/favorites/{bookId} - Add favorite without proper role should fail")
    @WithMockUser(roles = "USER")
    void testAddFavorite_Forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/favorites/{bookId}", testBook.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/favorites/{bookId} - Add favorite for non-existent book should return 404")
    @WithMockUser(username = "john.doe@test.com", roles = "MEMBER")
    void testAddFavorite_BookNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/favorites/{bookId}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/favorites/{bookId} - Add duplicate favorite should handle appropriately")
    @WithMockUser(username = "john.doe@test.com", roles = "MEMBER")
    @Transactional
    void testAddFavorite_Duplicate() throws Exception {
        // Try to add the same favorite again
        mockMvc.perform(post("/api/v1/favorites/{bookId}", testBook.getId()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/v1/favorites/{bookId} - Admin can add favorite")
    @WithMockUser(username = "john.doe@test.com", roles = "ADMIN")
    @Transactional
    void testAddFavorite_AsAdmin() throws Exception {
        // Create another book
        Book book2 = new Book();
        book2.setTitle("Admin's Book");
        book2.setAuthor("Admin Author");
        book2.setIsbn("ADMIN-ISBN-789");
        book2.setPublisher(testBook.getPublisher());
        book2 = bookRepository.save(book2);

        mockMvc.perform(post("/api/v1/favorites/{bookId}", book2.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/favorites/{bookId} - Remove favorite successfully")
    @WithMockUser(username = "john.doe@test.com", roles = "MEMBER")
    void testRemoveFavorite_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/favorites/{bookId}", testBook.getId()))
                .andExpect(status().isOk());

        // Verify database
        assertThat(favoriteRepository.findById(testFavorite.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/favorites/{bookId} - Remove favorite without proper role should fail")
    @WithMockUser(roles = "USER")
    void testRemoveFavorite_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/favorites/{bookId}", testBook.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/favorites/{bookId} - Remove non-existent favorite should return 404")
    @WithMockUser(username = "john.doe@test.com", roles = "MEMBER")
    void testRemoveFavorite_NotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/favorites/{bookId}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/favorites - Get user favorites successfully")
    @WithMockUser(username = "john.doe@test.com", roles = "MEMBER")
    void testGetFavorites_Success() throws Exception {
        mockMvc.perform(get("/api/v1/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].book.id").value(testBook.getId()));
    }

    @Test
    @DisplayName("GET /api/v1/favorites - Get favorites without proper role should fail")
    @WithMockUser(roles = "USER")
    void testGetFavorites_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/favorites"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/favorites - Get favorites when none exist")
    @WithMockUser(username = "jane.smith@test.com", roles = "MEMBER")
    @Transactional
    void testGetFavorites_Empty() throws Exception {
        // Create a new member with no favorites
        Member member2 = new Member();
        member2.setFirstName("Jane");
        member2.setLastName("Smith");
        member2.setEmail("jane.smith@test.com");
        member2.setPhone("9876543210");
        member2.setMembershipType(testMember.getMembershipType());
        memberRepository.save(member2);

        mockMvc.perform(get("/api/v1/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/favorites - Get multiple favorites")
    @WithMockUser(username = "john.doe@test.com", roles = "MEMBER")
    @Transactional
    void testGetFavorites_Multiple() throws Exception {
        // Create another book and add it as favorite
        Book book2 = new Book();
        book2.setTitle("Second Favorite");
        book2.setAuthor("Favorite Author");
        book2.setIsbn("FAVORITE-ISBN-456");
        book2.setPublisher(testBook.getPublisher());
        book2 = bookRepository.save(book2);

        Favorite favorite2 = new Favorite();
        favorite2.setMember(testMember);
        favorite2.setBook(book2);
        favoriteRepository.save(favorite2);

        mockMvc.perform(get("/api/v1/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/favorites - Admin can get favorites")
    @WithMockUser(username = "john.doe@test.com", roles = "ADMIN")
    void testGetFavorites_AsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("DELETE /api/v1/favorites/{bookId} - Admin can remove favorite")
    @WithMockUser(username = "john.doe@test.com", roles = "ADMIN")
    void testRemoveFavorite_AsAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/favorites/{bookId}", testBook.getId()))
                .andExpect(status().isOk());
    }
}
