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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("BookRating Controller Integration Tests")
class BookRatingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRatingRepository bookRatingRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private MembershipTypeRepository membershipTypeRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    private Member testMember;
    private Book testBook;
    private BookRating testRating;

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

        // Create rating
        testRating = new BookRating();
        testRating.setBook(testBook);
        testRating.setMember(testMember);
        testRating.setScore(5);
        testRating.setComment("Excellent book!");
        testRating = bookRatingRepository.save(testRating);
    }

    @AfterEach
    void tearDown() {
        bookRatingRepository.deleteAll();
        loanRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        memberRepository.deleteAll();
        membershipTypeRepository.deleteAll();
        publisherRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/ratings - Add rating successfully")
    @WithMockUser(username = "john.doe@test.com", roles = "MEMBER")
    @Transactional
    void testAddRating_Success() throws Exception {
        // Create another book for rating
        Book book2 = new Book();
        book2.setTitle("Another Book");
        book2.setAuthor("Another Author");
        book2.setIsbn("ANOTHER-ISBN-456");
        book2.setPublisher(testBook.getPublisher());
        book2 = bookRepository.save(book2);

        // Create book copy and loan history so user can rate this book
        BookCopy copy2 = new BookCopy();
        copy2.setBook(book2);
        copy2.setBarcode("COPY-ANOTHER-456");
        copy2.setStatus(BookCopyStatus.AVAILABLE);
        copy2 = bookCopyRepository.save(copy2);

        Loan pastLoan = new Loan();
        pastLoan.setMember(testMember);
        pastLoan.setBookCopy(copy2);
        pastLoan.setLoanDate(LocalDateTime.now().minusDays(30));
        pastLoan.setDueDate(LocalDateTime.now().minusDays(16));
        pastLoan.setReturnDate(LocalDateTime.now().minusDays(15));
        pastLoan.setStatus(LoanStatus.RETURNED);
        loanRepository.save(pastLoan);

        String ratingJson = String.format("""
                {
                    "book": {"id": %d},
                    "rating": 4,
                    "review": "Great book!"
                }
                """, book2.getId());

        mockMvc.perform(post("/api/v1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ratingJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.review").value("Great book!"));

        // Verify database
        List<BookRating> ratings = bookRatingRepository.findAll();
        assertThat(ratings).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("POST /api/v1/ratings - Add rating without MEMBER role should fail")
    @WithMockUser(roles = "USER")
    void testAddRating_Forbidden() throws Exception {
        String ratingJson = String.format("""
                {
                    "book": {"id": %d},
                    "rating": 4,
                    "review": "Great book!"
                }
                """, testBook.getId());

        mockMvc.perform(post("/api/v1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ratingJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/ratings - Add rating with invalid data should return 400")
    @WithMockUser(username = "john.doe@test.com", roles = "MEMBER")
    void testAddRating_InvalidData() throws Exception {
        String invalidRatingJson = """
                {
                    "rating": 10,
                    "review": "Invalid rating value"
                }
                """;

        mockMvc.perform(post("/api/v1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRatingJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/ratings/book/{bookId} - Get ratings for book successfully")
    @WithMockUser
    void testGetRatingsForBook_Success() throws Exception {
        mockMvc.perform(get("/api/v1/ratings/book/{bookId}", testBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[0].review").value("Excellent book!"));
    }

    @Test
    @DisplayName("GET /api/v1/ratings/book/{bookId} - Get ratings for non-existent book should return empty list")
    @WithMockUser
    void testGetRatingsForBook_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/ratings/book/{bookId}", 99999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/ratings/book/{bookId} - Get multiple ratings for same book")
    @WithMockUser
    @Transactional
    void testGetRatingsForBook_MultipleRatings() throws Exception {
        // Create another member and rating
        Member member2 = new Member();
        member2.setFirstName("Jane");
        member2.setLastName("Smith");
        member2.setEmail("jane.smith@test.com");
        member2.setPhone("9876543210");
        member2.setMembershipType(testMember.getMembershipType());
        member2 = memberRepository.save(member2);

        BookRating rating2 = new BookRating();
        rating2.setBook(testBook);
        rating2.setMember(member2);
        rating2.setScore(4);
        rating2.setComment("Good book");
        bookRatingRepository.save(rating2);

        mockMvc.perform(get("/api/v1/ratings/book/{bookId}", testBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/ratings/average/{bookId} - Get average rating successfully")
    @WithMockUser
    @Transactional
    void testGetAverageRating_Success() throws Exception {
        // Create another member and rating
        Member member2 = new Member();
        member2.setFirstName("Jane");
        member2.setLastName("Smith");
        member2.setEmail("jane.smith@test.com");
        member2.setPhone("9876543210");
        member2.setMembershipType(testMember.getMembershipType());
        member2 = memberRepository.save(member2);

        BookRating rating2 = new BookRating();
        rating2.setBook(testBook);
        rating2.setMember(member2);
        rating2.setScore(3);
        rating2.setComment("Decent book");
        bookRatingRepository.save(rating2);

        mockMvc.perform(get("/api/v1/ratings/average/{bookId}", testBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(4.0)); // Average of 5 and 3
    }

    @Test
    @DisplayName("GET /api/v1/ratings/average/{bookId} - Get average rating for book with no ratings")
    @WithMockUser
    void testGetAverageRating_NoRatings() throws Exception {
        bookRatingRepository.deleteAll();

        mockMvc.perform(get("/api/v1/ratings/average/{bookId}", testBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0.0));
    }

    @Test
    @DisplayName("DELETE /api/v1/ratings/{id} - Delete rating successfully")
    @WithMockUser(roles = "ADMIN")
    void testDeleteRating_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/ratings/{id}", testRating.getId()))
                .andExpect(status().isNoContent());

        // Verify database
        assertThat(bookRatingRepository.findById(testRating.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/ratings/{id} - Delete rating without admin role should fail")
    @WithMockUser(roles = "USER")
    void testDeleteRating_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/ratings/{id}", testRating.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/ratings/{id} - Delete non-existent rating should return 404")
    @WithMockUser(roles = "ADMIN")
    void testDeleteRating_NotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/ratings/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/ratings - Add rating with out of range value should return 400")
    @WithMockUser(username = "john.doe@test.com", roles = "MEMBER")
    void testAddRating_OutOfRangeValue() throws Exception {
        String ratingJson = String.format("""
                {
                    "book": {"id": %d},
                    "rating": 6,
                    "review": "Rating too high"
                }
                """, testBook.getId());

        mockMvc.perform(post("/api/v1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ratingJson))
                .andExpect(status().isBadRequest());
    }
}
