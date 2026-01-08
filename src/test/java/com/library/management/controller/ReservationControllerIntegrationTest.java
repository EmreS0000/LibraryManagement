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
@DisplayName("Reservation Controller Integration Tests")
class ReservationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private MembershipTypeRepository membershipTypeRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    private Member testMember;
    private Book testBook;
    private Reservation testReservation;

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

        // Create a loaned book copy so reservations are allowed
        BookCopy loanedCopy = new BookCopy();
        loanedCopy.setBook(testBook);
        loanedCopy.setBarcode("COPY-LOANED-123");
        loanedCopy.setStatus(BookCopyStatus.LOANED);
        bookCopyRepository.save(loanedCopy);

        // Create reservation
        testReservation = new Reservation();
        testReservation.setMember(testMember);
        testReservation.setBook(testBook);
        testReservation.setExpiryDate(LocalDateTime.now().plusDays(3));
        testReservation.setStatus(ReservationStatus.PENDING);
        testReservation = reservationRepository.save(testReservation);
    }

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        memberRepository.deleteAll();
        membershipTypeRepository.deleteAll();
        publisherRepository.deleteAll();
    }

    // TODO: Fix this test - requires all book copies to be unavailable for reservation
    // @Test
    // @DisplayName("POST /api/v1/reservations - Place reservation successfully")
    // @WithMockUser
    // @Transactional
    // void testPlaceReservation_Success() throws Exception {
    //     // Create another member for new reservation
    //     Member member2 = new Member();
    //     member2.setFirstName("Jane");
    //     member2.setLastName("Smith");
    //     member2.setEmail("jane.smith@test.com");
    //     member2.setPhone("9876543210");
    //     member2.setMembershipType(testMember.getMembershipType());
    //     member2 = memberRepository.save(member2);

    //     String reservationJson = String.format("""
    //             {
    //                 "member": {"id": %d},
    //                 "book": {"id": %d},
    //                 "reservationDate": "%s",
    //                 "status": "ACTIVE"
    //             }
    //             """, member2.getId(), testBook.getId(), LocalDateTime.now());

    //     mockMvc.perform(post("/api/v1/reservations")
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .content(reservationJson))
    //             .andExpect(status().isCreated())
    //             .andExpect(jsonPath("$.status").value("ACTIVE"));

    //     // Verify database
    //     List<Reservation> reservations = reservationRepository.findAll();
    //     assertThat(reservations).hasSizeGreaterThanOrEqualTo(2);
    // }

    // TODO: Fix this test - validation needs to catch invalid data before service layer
    // @Test
    // @DisplayName("POST /api/v1/reservations - Place reservation with invalid data should return 400")
    // @WithMockUser
    // void testPlaceReservation_InvalidData() throws Exception {
    //     String invalidReservationJson = """
    //             {
    //                 "member": {}
    //             }
    //             """;

    //     mockMvc.perform(post("/api/v1/reservations")
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .content(invalidReservationJson))
    //             .andExpect(status().isBadRequest());
    // }

    @Test
    @DisplayName("DELETE /api/v1/reservations/{id} - Cancel reservation successfully")
    @WithMockUser
    void testCancelReservation_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/reservations/{id}", testReservation.getId()))
                .andExpect(status().isNoContent());

        // Verify database
        assertThat(reservationRepository.findById(testReservation.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/reservations/{id} - Cancel non-existent reservation should return 404")
    @WithMockUser
    void testCancelReservation_NotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/reservations/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/reservations/book/{bookId} - Get reservations for book successfully")
    @WithMockUser
    void testGetReservationsForBook_Success() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/book/{bookId}", testBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].book.id").value(testBook.getId()));
    }

    @Test
    @DisplayName("GET /api/v1/reservations/book/{bookId} - Get reservations for non-existent book should return empty list")
    @WithMockUser
    void testGetReservationsForBook_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/book/{bookId}", 99999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/reservations/queue-position/{bookId}/{memberId} - Get queue position successfully")
    @WithMockUser
    @Transactional
    void testGetQueuePosition_Success() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/queue-position/{bookId}/{memberId}",
                        testBook.getId(), testMember.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    @DisplayName("GET /api/v1/reservations/book/{bookId} - Get multiple reservations for same book")
    @WithMockUser
    @Transactional
    void testGetReservationsForBook_MultipleReservations() throws Exception {
        // Create another member and reservation
        Member member2 = new Member();
        member2.setFirstName("Jane");
        member2.setLastName("Smith");
        member2.setEmail("jane.smith@test.com");
        member2.setPhone("9876543210");
        member2.setMembershipType(testMember.getMembershipType());
        member2 = memberRepository.save(member2);

        Reservation reservation2 = new Reservation();
        reservation2.setMember(member2);
        reservation2.setBook(testBook);
        reservation2.setExpiryDate(LocalDateTime.now().plusDays(3));
        reservation2.setStatus(ReservationStatus.PENDING);
        reservationRepository.save(reservation2);

        mockMvc.perform(get("/api/v1/reservations/book/{bookId}", testBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/reservations/book/{bookId} - Get reservations when none exist")
    @WithMockUser
    void testGetReservationsForBook_Empty() throws Exception {
        reservationRepository.deleteAll();

        mockMvc.perform(get("/api/v1/reservations/book/{bookId}", testBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
