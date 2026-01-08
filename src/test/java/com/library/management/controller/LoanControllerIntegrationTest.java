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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Loan Controller Integration Tests")
class LoanControllerIntegrationTest {

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
    private MembershipTypeRepository membershipTypeRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    private Member testMember;
    private Book testBook;
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
        testLoan.setLoanDate(LocalDateTime.now());
        testLoan.setDueDate(LocalDateTime.now().plusDays(14));
        testLoan.setStatus(LoanStatus.ACTIVE);
        testLoan = loanRepository.save(testLoan);
    }

    @AfterEach
    void tearDown() {
        loanRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        memberRepository.deleteAll();
        membershipTypeRepository.deleteAll();
        publisherRepository.deleteAll();
    }

    // TODO: Fix this test - requires member balance = 0 and proper business rules
    // @Test
    // @DisplayName("POST /api/v1/loans/borrow - Borrow book successfully")
    // @WithMockUser
    // @Transactional
    // void testBorrowBook_Success() throws Exception {
    //     // Create another available book copy
    //     BookCopy availableCopy = new BookCopy();
    //     availableCopy.setBook(testBook);
    //     availableCopy.setBarcode("COPY-456");
    //     availableCopy.setStatus(BookCopyStatus.AVAILABLE);
    //     availableCopy = bookCopyRepository.save(availableCopy);

    //     String loanJson = String.format("""
    //             {
    //                 "member": {"id": %d},
    //                 "bookCopy": {"id": %d},
    //                 "borrowDate": "%s",
    //                 "dueDate": "%s"
    //             }
    //             """, testMember.getId(), availableCopy.getId(),
    //             LocalDate.now(), LocalDate.now().plusDays(14));

    //     mockMvc.perform(post("/api/v1/loans/borrow")
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .content(loanJson))
    //             .andExpect(status().isCreated())
    //             .andExpect(jsonPath("$.status").value("ACTIVE"));

    //     // Verify database
    //     List<Loan> loans = loanRepository.findAll();
    //     assertThat(loans).hasSizeGreaterThanOrEqualTo(2);
    // }

    @Test
    @DisplayName("POST /api/v1/loans/borrow - Borrow with invalid data should return 400")
    @WithMockUser
    void testBorrowBook_InvalidData() throws Exception {
        String invalidLoanJson = """
                {
                    "member": {}
                }
                """;

        mockMvc.perform(post("/api/v1/loans/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidLoanJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/loans/{id}/return - Return borrowed book successfully")
    @WithMockUser(roles = "ADMIN")
    void testReturnBook_Success() throws Exception {
        mockMvc.perform(post("/api/v1/loans/{id}/return", testLoan.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnDate").exists());

        // Verify database
        Loan returnedLoan = loanRepository.findById(testLoan.getId()).orElseThrow();
        assertThat(returnedLoan.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(returnedLoan.getReturnDate()).isNotNull();
    }

    @Test
    @DisplayName("POST /api/v1/loans/{id}/return - Return with USER role should succeed")
    @WithMockUser(roles = "USER")
    void testReturnBook_WithUserRole() throws Exception {
        mockMvc.perform(post("/api/v1/loans/{id}/return", testLoan.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/loans/{id}/return - Return non-existent loan should return 404")
    @WithMockUser(roles = "ADMIN")
    void testReturnBook_NotFound() throws Exception {
        mockMvc.perform(post("/api/v1/loans/{id}/return", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/loans - Get all loans successfully")
    @WithMockUser
    void testGetAllLoans_Success() throws Exception {
        mockMvc.perform(get("/api/v1/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").exists());
    }

    @Test
    @DisplayName("GET /api/v1/loans/admin/all - Get all loans as admin successfully")
    @WithMockUser(roles = "ADMIN")
    void testGetAllLoansAdmin_Success() throws Exception {
        mockMvc.perform(get("/api/v1/loans/admin/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/loans/admin/all - Get all loans without admin role should fail")
    @WithMockUser(roles = "USER")
    void testGetAllLoansAdmin_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/loans/admin/all"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/loans/member/{memberId} - Get loans by member successfully")
    @WithMockUser
    void testGetLoansByMember_Success() throws Exception {
        mockMvc.perform(get("/api/v1/loans/member/{memberId}", testMember.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].member.id").value(testMember.getId()));
    }

    @Test
    @DisplayName("GET /api/v1/loans/member/{memberId} - Get loans for non-existent member should return empty list")
    @WithMockUser
    void testGetLoansByMember_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/loans/member/{memberId}", 99999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/loans - Get all loans when empty")
    @WithMockUser
    void testGetAllLoans_Empty() throws Exception {
        loanRepository.deleteAll();

        mockMvc.perform(get("/api/v1/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("POST /api/v1/loans/{id}/return - Return already returned loan should handle appropriately")
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void testReturnBook_AlreadyReturned() throws Exception {
        // First return
        mockMvc.perform(post("/api/v1/loans/{id}/return", testLoan.getId()))
                .andExpect(status().isOk());

        // Try to return again - this might throw exception or handle gracefully
        // Depending on business logic implementation
        mockMvc.perform(post("/api/v1/loans/{id}/return", testLoan.getId()))
                .andExpect(status().is4xxClientError());
    }
}
