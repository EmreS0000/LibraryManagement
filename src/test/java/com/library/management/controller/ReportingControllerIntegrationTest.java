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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Reporting Controller Integration Tests")
class ReportingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MembershipTypeRepository membershipTypeRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    private Member testMember;
    private Book testBook;
    private Category testCategory;
    private BookCopy testBookCopy;
    private Loan testLoan;

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

        // Create category
        testCategory = new Category();
        testCategory.setName("Fiction");
        testCategory.setDescription("Fiction books");
        testCategory = categoryRepository.save(testCategory);

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
        testBook.getCategories().add(testCategory);
        testBook = bookRepository.save(testBook);

        // Create book copy
        testBookCopy = new BookCopy();
        testBookCopy.setBook(testBook);
        testBookCopy.setBarcode("COPY-123");
        testBookCopy.setStatus(BookCopyStatus.AVAILABLE);
        testBookCopy = bookCopyRepository.save(testBookCopy);

        // Create loan
        testLoan = new Loan();
        testLoan.setMember(testMember);
        testLoan.setBookCopy(testBookCopy);
        testLoan.setLoanDate(LocalDateTime.now().minusDays(7));
        testLoan.setDueDate(LocalDateTime.now().plusDays(7));
        testLoan.setStatus(LoanStatus.ACTIVE);
        testLoan = loanRepository.save(testLoan);
    }

    @AfterEach
    void tearDown() {
        loanRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();
        membershipTypeRepository.deleteAll();
        publisherRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/v1/reporting/categories/most-read - Get most read categories successfully")
    @WithMockUser(roles = "ADMIN")
    void testGetMostReadCategories_Success() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/categories/most-read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/categories/most-read - Get with custom limit")
    @WithMockUser(roles = "ADMIN")
    void testGetMostReadCategories_WithLimit() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/categories/most-read")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/categories/most-read - Without admin role should fail")
    @WithMockUser(roles = "USER")
    void testGetMostReadCategories_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/categories/most-read"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/members/most-active - Get most active members successfully")
    @WithMockUser(roles = "ADMIN")
    void testGetMostActiveMembers_Success() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/members/most-active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/members/most-active - Get with custom limit")
    @WithMockUser(roles = "ADMIN")
    void testGetMostActiveMembers_WithLimit() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/members/most-active")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/members/most-active - Without admin role should fail")
    @WithMockUser(roles = "USER")
    void testGetMostActiveMembers_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/members/most-active"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/books/status-distribution - Get book status distribution successfully")
    @WithMockUser(roles = "ADMIN")
    void testGetBookStatusDistribution_Success() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/books/status-distribution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/books/status-distribution - Without admin role should fail")
    @WithMockUser(roles = "USER")
    void testGetBookStatusDistribution_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reporting/books/status-distribution"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/categories/most-read - Verify data with multiple categories")
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testGetMostReadCategories_MultipleCategories() throws Exception {
        // Create additional categories and books
        Category category2 = new Category();
        category2.setName("Non-Fiction");
        category2.setDescription("Non-fiction books");
        category2 = categoryRepository.save(category2);

        Book book2 = new Book();
        book2.setTitle("Another Book");
        book2.setAuthor("Another Author");
        book2.setIsbn("ANOTHER-ISBN-456");
        book2.setPublisher(testBook.getPublisher());
        book2.getCategories().add(category2);
        book2 = bookRepository.save(book2);

        BookCopy copy2 = new BookCopy();
        copy2.setBook(book2);
        copy2.setBarcode("COPY-456");
        copy2.setStatus(BookCopyStatus.LOANED);
        copy2 = bookCopyRepository.save(copy2);

        Loan loan2 = new Loan();
        loan2.setMember(testMember);
        loan2.setBookCopy(copy2);
        loan2.setLoanDate(LocalDateTime.now().minusDays(5));
        loan2.setDueDate(LocalDateTime.now().plusDays(9));
        loan2.setStatus(LoanStatus.ACTIVE);
        loanRepository.save(loan2);

        mockMvc.perform(get("/api/v1/reporting/categories/most-read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/members/most-active - Verify data with multiple members")
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testGetMostActiveMembers_MultipleMembers() throws Exception {
        // Create additional member
        Member member2 = new Member();
        member2.setFirstName("Jane");
        member2.setLastName("Smith");
        member2.setEmail("jane.smith@test.com");
        member2.setPhone("9876543210");
        member2.setMembershipType(testMember.getMembershipType());
        member2 = memberRepository.save(member2);

        // Create loan for second member
        BookCopy copy2 = new BookCopy();
        copy2.setBook(testBook);
        copy2.setBarcode("COPY-789");
        copy2.setStatus(BookCopyStatus.LOANED);
        copy2 = bookCopyRepository.save(copy2);

        Loan loan2 = new Loan();
        loan2.setMember(member2);
        loan2.setBookCopy(copy2);
        loan2.setLoanDate(LocalDateTime.now().minusDays(3));
        loan2.setDueDate(LocalDateTime.now().plusDays(11));
        loan2.setStatus(LoanStatus.ACTIVE);
        loanRepository.save(loan2);

        mockMvc.perform(get("/api/v1/reporting/members/most-active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/books/status-distribution - Verify data with different statuses")
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testGetBookStatusDistribution_DifferentStatuses() throws Exception {
        // Create book copies with different statuses
        BookCopy availableCopy = new BookCopy();
        availableCopy.setBook(testBook);
        availableCopy.setBarcode("AVAILABLE-COPY");
        availableCopy.setStatus(BookCopyStatus.AVAILABLE);
        bookCopyRepository.save(availableCopy);

        BookCopy borrowedCopy = new BookCopy();
        borrowedCopy.setBook(testBook);
        borrowedCopy.setBarcode("BORROWED-COPY");
        borrowedCopy.setStatus(BookCopyStatus.LOANED);
        bookCopyRepository.save(borrowedCopy);

        mockMvc.perform(get("/api/v1/reporting/books/status-distribution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/categories/most-read - When no data exists")
    @WithMockUser(roles = "ADMIN")
    void testGetMostReadCategories_NoData() throws Exception {
        loanRepository.deleteAll();

        mockMvc.perform(get("/api/v1/reporting/categories/most-read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/members/most-active - When no data exists")
    @WithMockUser(roles = "ADMIN")
    void testGetMostActiveMembers_NoData() throws Exception {
        loanRepository.deleteAll();

        mockMvc.perform(get("/api/v1/reporting/members/most-active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/reporting/books/status-distribution - When no data exists")
    @WithMockUser(roles = "ADMIN")
    void testGetBookStatusDistribution_NoData() throws Exception {
        loanRepository.deleteAll();
        bookCopyRepository.deleteAll();

        mockMvc.perform(get("/api/v1/reporting/books/status-distribution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
