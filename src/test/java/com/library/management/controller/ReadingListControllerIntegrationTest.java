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
@DisplayName("ReadingList Controller Integration Tests")
class ReadingListControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReadingListItemRepository readingListItemRepository;

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
    private ReadingListItem testReadingListItem;

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

        // Create reading list item
        testReadingListItem = new ReadingListItem();
        testReadingListItem.setMember(testMember);
        testReadingListItem.setBook(testBook);
        testReadingListItem = readingListItemRepository.save(testReadingListItem);
    }

    @AfterEach
    void tearDown() {
        readingListItemRepository.deleteAll();
        bookRepository.deleteAll();
        memberRepository.deleteAll();
        membershipTypeRepository.deleteAll();
        publisherRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/reading-list/{bookId} - Add book to reading list successfully")
    @WithMockUser(username = "john.doe@test.com", roles = "MEMBER")
    @Transactional
    void testAddToReadingList_Success() throws Exception {
        // Create another book to add to reading list
        Book book2 = new Book();
        book2.setTitle("Another Book");
        book2.setAuthor("Another Author");
        book2.setIsbn("ANOTHER-ISBN-456");
        book2.setPublisher(testBook.getPublisher());
        Book savedBook2 = bookRepository.save(book2);

        mockMvc.perform(post("/api/v1/reading-list/{bookId}", savedBook2.getId()))
                .andExpect(status().isOk());

        // Verify database
        List<ReadingListItem> items = readingListItemRepository.findAll();
        assertThat(items).hasSizeGreaterThanOrEqualTo(2);
        assertThat(items).anyMatch(item -> item.getBook().getId().equals(savedBook2.getId()));
    }

    @Test
    @DisplayName("POST /api/v1/reading-list/{bookId} - Add to reading list without proper role should fail")
    @WithMockUser(roles = "USER")
    void testAddToReadingList_Forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/reading-list/{bookId}", testBook.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/reading-list/{bookId} - Add non-existent book should return 404")
    @WithMockUser(username = "john.doe@test.com", roles = "MEMBER")
    void testAddToReadingList_BookNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/reading-list/{bookId}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/reading-list/{bookId} - Add duplicate book should handle appropriately")
    @WithMockUser(username = "john.doe@test.com", roles = "MEMBER")
    @Transactional
    void testAddToReadingList_Duplicate() throws Exception {
        // Try to add the same book again
        mockMvc.perform(post("/api/v1/reading-list/{bookId}", testBook.getId()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/v1/reading-list/{bookId} - Admin can add to reading list")
    @WithMockUser(username = "john.doe@test.com", roles = "ADMIN")
    @Transactional
    void testAddToReadingList_AsAdmin() throws Exception {
        // Create another book
        Book book2 = new Book();
        book2.setTitle("Admin's Book");
        book2.setAuthor("Admin Author");
        book2.setIsbn("ADMIN-ISBN-789");
        book2.setPublisher(testBook.getPublisher());
        book2 = bookRepository.save(book2);

        mockMvc.perform(post("/api/v1/reading-list/{bookId}", book2.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/reading-list/{bookId} - Remove book from reading list successfully")
    @WithMockUser(username = "john.doe@test.com", roles = "MEMBER")
    void testRemoveFromReadingList_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/reading-list/{bookId}", testBook.getId()))
                .andExpect(status().isOk());

        // Verify database
        assertThat(readingListItemRepository.findById(testReadingListItem.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/reading-list/{bookId} - Remove without proper role should fail")
    @WithMockUser(roles = "USER")
    void testRemoveFromReadingList_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/reading-list/{bookId}", testBook.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/reading-list/{bookId} - Remove non-existent book should return 404")
    @WithMockUser(username = "john.doe@test.com", roles = "MEMBER")
    void testRemoveFromReadingList_NotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/reading-list/{bookId}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/reading-list - Get reading list successfully")
    @WithMockUser(username = "john.doe@test.com", roles = "MEMBER")
    void testGetReadingList_Success() throws Exception {
        mockMvc.perform(get("/api/v1/reading-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].book.id").value(testBook.getId()));
    }

    @Test
    @DisplayName("GET /api/v1/reading-list - Get reading list without proper role should fail")
    @WithMockUser(roles = "USER")
    void testGetReadingList_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reading-list"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/reading-list - Get reading list when empty")
    @WithMockUser(username = "jane.smith@test.com", roles = "MEMBER")
    @Transactional
    void testGetReadingList_Empty() throws Exception {
        // Create a new member with no reading list
        Member member2 = new Member();
        member2.setFirstName("Jane");
        member2.setLastName("Smith");
        member2.setEmail("jane.smith@test.com");
        member2.setPhone("9876543210");
        member2.setMembershipType(testMember.getMembershipType());
        memberRepository.save(member2);

        mockMvc.perform(get("/api/v1/reading-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/reading-list - Get multiple books in reading list")
    @WithMockUser(username = "john.doe@test.com", roles = "MEMBER")
    @Transactional
    void testGetReadingList_Multiple() throws Exception {
        // Create another book and add it to reading list
        Book book2 = new Book();
        book2.setTitle("Second Book");
        book2.setAuthor("Second Author");
        book2.setIsbn("SECOND-ISBN-456");
        book2.setPublisher(testBook.getPublisher());
        book2 = bookRepository.save(book2);

        ReadingListItem item2 = new ReadingListItem();
        item2.setMember(testMember);
        item2.setBook(book2);
        readingListItemRepository.save(item2);

        mockMvc.perform(get("/api/v1/reading-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/reading-list - Admin can get reading list")
    @WithMockUser(username = "john.doe@test.com", roles = "ADMIN")
    void testGetReadingList_AsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/reading-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("DELETE /api/v1/reading-list/{bookId} - Admin can remove from reading list")
    @WithMockUser(username = "john.doe@test.com", roles = "ADMIN")
    void testRemoveFromReadingList_AsAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/reading-list/{bookId}", testBook.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/reading-list/{bookId} - Add multiple books to reading list")
    @WithMockUser(username = "john.doe@test.com", roles = "MEMBER")
    @Transactional
    void testAddToReadingList_MultipleBooksSequentially() throws Exception {
        // Create and add three books
        for (int i = 1; i <= 3; i++) {
            Book book = new Book();
            book.setTitle("Book " + i);
            book.setAuthor("Author " + i);
            book.setIsbn("ISBN-" + i);
            book.setPublisher(testBook.getPublisher());
            book = bookRepository.save(book);

            mockMvc.perform(post("/api/v1/reading-list/{bookId}", book.getId()))
                    .andExpect(status().isOk());
        }

        // Verify all books were added
        List<ReadingListItem> items = readingListItemRepository.findAll();
        assertThat(items).hasSize(4); // 1 from setup + 3 newly added
    }
}
