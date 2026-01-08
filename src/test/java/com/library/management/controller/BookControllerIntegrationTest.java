package com.library.management.controller;

import com.library.management.entity.Book;
import com.library.management.entity.BookCopy;
import com.library.management.entity.Category;
import com.library.management.entity.Publisher;
import com.library.management.repository.BookCopyRepository;
import com.library.management.repository.BookRepository;
import com.library.management.repository.CategoryRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Book Controller Integration Tests")
class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    private Publisher testPublisher;
    private Category testCategory;
    private Book testBook;

    @BeforeEach
    void setUp() {
        // Create test publisher
        testPublisher = new Publisher();
        testPublisher.setName("Test Publisher");
        testPublisher.setCountry("USA");
        testPublisher.setFoundedYear(1990);
        testPublisher = publisherRepository.save(testPublisher);

        // Create test category
        testCategory = new Category();
        testCategory.setName("Test Category");
        testCategory.setDescription("Test Description");
        testCategory = categoryRepository.save(testCategory);

        // Create test book
        testBook = new Book();
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setIsbn("TEST-ISBN-123");
        testBook.setPublisher(testPublisher);
        testBook.setPublishYear(2023);
        testBook.setPageCount(300);
        testBook.setCategories(new HashSet<>());
        testBook.getCategories().add(testCategory);
        testBook = bookRepository.save(testBook);
    }

    @AfterEach
    void tearDown() {
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
        publisherRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/books - Create book successfully")
    @WithMockUser(roles = "ADMIN")
    void testCreateBook_Success() throws Exception {
        String bookJson = String.format("""
                {
                    "title": "New Book",
                    "author": "New Author",
                    "isbn": "NEW-ISBN-456",
                    "publishYear": 2024,
                    "pageCount": 250,
                    "publisher": {"id": %d},
                    "categories": []
                }
                """, testPublisher.getId());

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Book"))
                .andExpect(jsonPath("$.author").value("New Author"))
                .andExpect(jsonPath("$.isbn").value("NEW-ISBN-456"));

        // Verify database
        List<Book> books = bookRepository.findAll();
        assertThat(books).hasSize(2);
        assertThat(books).anyMatch(b -> b.getIsbn().equals("NEW-ISBN-456"));
    }

    @Test
    @DisplayName("POST /api/v1/books - Create book without admin role should fail")
    @WithMockUser(roles = "USER")
    void testCreateBook_Forbidden() throws Exception {
        String bookJson = """
                {
                    "title": "New Book",
                    "author": "New Author",
                    "isbn": "NEW-ISBN-456"
                }
                """;

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/books/{id} - Get book by ID successfully")
    @WithMockUser
    void testGetBookById_Success() throws Exception {
        mockMvc.perform(get("/api/v1/books/{id}", testBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testBook.getId()))
                .andExpect(jsonPath("$.title").value("Test Book"))
                .andExpect(jsonPath("$.author").value("Test Author"))
                .andExpect(jsonPath("$.isbn").value("TEST-ISBN-123"));
    }

    @Test
    @DisplayName("GET /api/v1/books/{id} - Get non-existent book should return 404")
    @WithMockUser
    void testGetBookById_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/books/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/books - Get all books successfully")
    @WithMockUser
    void testGetAllBooks_Success() throws Exception {
        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("Test Book"));
    }

    @Test
    @DisplayName("GET /api/v1/books - Search books by title")
    @WithMockUser
    void testGetAllBooks_WithTitleSearch() throws Exception {
        mockMvc.perform(get("/api/v1/books")
                        .param("title", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("Test Book"));
    }

    @Test
    @DisplayName("GET /api/v1/books - Search books by author")
    @WithMockUser
    void testGetAllBooks_WithAuthorSearch() throws Exception {
        mockMvc.perform(get("/api/v1/books")
                        .param("author", "Test Author"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("PUT /api/v1/books/{id} - Update book successfully")
    @WithMockUser(roles = "ADMIN")
    void testUpdateBook_Success() throws Exception {
        String updateJson = String.format("""
                {
                    "title": "Updated Book",
                    "author": "Updated Author",
                    "isbn": "TEST-ISBN-123",
                    "publishYear": 2024,
                    "pageCount": 350,
                    "publisher": {"id": %d},
                    "categories": []
                }
                """, testPublisher.getId());

        mockMvc.perform(put("/api/v1/books/{id}", testBook.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Book"))
                .andExpect(jsonPath("$.author").value("Updated Author"));

        // Verify database
        Book updatedBook = bookRepository.findById(testBook.getId()).orElseThrow();
        assertThat(updatedBook.getTitle()).isEqualTo("Updated Book");
    }

    @Test
    @DisplayName("PUT /api/v1/books/{id} - Update non-existent book should return 404")
    @WithMockUser(roles = "ADMIN")
    void testUpdateBook_NotFound() throws Exception {
        String updateJson = """
                {
                    "title": "Updated Book",
                    "author": "Updated Author",
                    "isbn": "TEST-ISBN-999"
                }
                """;

        mockMvc.perform(put("/api/v1/books/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/books/{id} - Delete book successfully")
    @WithMockUser(roles = "ADMIN")
    void testDeleteBook_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/books/{id}", testBook.getId()))
                .andExpect(status().isNoContent());

        // Verify database
        assertThat(bookRepository.findById(testBook.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/books/{id} - Delete without admin role should fail")
    @WithMockUser(roles = "USER")
    void testDeleteBook_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/books/{id}", testBook.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/books/{id}/copies - Get book copies successfully")
    @WithMockUser(roles = "ADMIN")
    void testGetBookCopies_Success() throws Exception {
        // Create book copy
        BookCopy copy = new BookCopy();
        copy.setBook(testBook);
        copy.setBarcode("COPY-123");
        copy.setStatus(com.library.management.entity.BookCopyStatus.AVAILABLE);
        bookCopyRepository.save(copy);

        mockMvc.perform(get("/api/v1/books/{id}/copies", testBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].barcode").value("COPY-123"));
    }

    @Test
    @DisplayName("POST /api/v1/books/{id}/copies - Add book copy successfully")
    @WithMockUser(roles = "ADMIN")
    void testAddBookCopy_Success() throws Exception {
        mockMvc.perform(post("/api/v1/books/{id}/copies", testBook.getId())
                        .param("barcode", "NEW-COPY-123"))
                .andExpect(status().isCreated());

        // Verify database
        List<BookCopy> copies = bookCopyRepository.findAll();
        assertThat(copies).hasSize(1);
        assertThat(copies.get(0).getBarcode()).isEqualTo("NEW-COPY-123");
    }

    @Test
    @DisplayName("POST /api/v1/books/{id}/copies - Add copy without barcode")
    @WithMockUser(roles = "ADMIN")
    void testAddBookCopy_WithoutBarcode() throws Exception {
        mockMvc.perform(post("/api/v1/books/{id}/copies", testBook.getId()))
                .andExpect(status().isCreated());

        // Verify database - barcode should be auto-generated
        List<BookCopy> copies = bookCopyRepository.findAll();
        assertThat(copies).hasSize(1);
        assertThat(copies.get(0).getBarcode()).isNotNull();
    }

    @Test
    @DisplayName("DELETE /api/v1/books/copies/{copyId} - Delete book copy successfully")
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testDeleteBookCopy_Success() throws Exception {
        // Create book copy
        BookCopy copy = new BookCopy();
        copy.setBook(testBook);
        copy.setBarcode("COPY-TO-DELETE");
        copy.setStatus(com.library.management.entity.BookCopyStatus.AVAILABLE);
        copy = bookCopyRepository.save(copy);

        mockMvc.perform(delete("/api/v1/books/copies/{copyId}", copy.getId()))
                .andExpect(status().isNoContent());

        // Verify database
        assertThat(bookCopyRepository.findById(copy.getId())).isEmpty();
    }

    @Test
    @DisplayName("POST /api/v1/books - Create book with invalid data should return 400")
    @WithMockUser(roles = "ADMIN")
    void testCreateBook_InvalidData() throws Exception {
        String invalidBookJson = """
                {
                    "author": "Author Only"
                }
                """;

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBookJson))
                .andExpect(status().isBadRequest());
    }
}
