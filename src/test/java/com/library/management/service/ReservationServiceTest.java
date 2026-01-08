package com.library.management.service;

import com.library.management.entity.*;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.BookRepository;
import com.library.management.repository.MemberRepository;
import com.library.management.repository.ReservationRepository;
import com.library.management.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService Unit Tests")
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Book testBook;
    private Member testMember;
    private Reservation testReservation;
    private BookCopy testBookCopy;

    @BeforeEach
    void setUp() {
        testMember = new Member();
        testMember.setId(1L);
        testMember.setFirstName("John");
        testMember.setLastName("Doe");
        testMember.setEmail("john.doe@example.com");

        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("Test Book");
        testBook.setIsbn("978-1234567890");
        testBook.setCopies(new HashSet<>());

        testBookCopy = new BookCopy();
        testBookCopy.setId(1L);
        testBookCopy.setBook(testBook);
        testBookCopy.setBarcode("BC001");
        testBookCopy.setStatus(BookCopyStatus.LOANED);

        testBook.getCopies().add(testBookCopy);

        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setBook(testBook);
        testReservation.setMember(testMember);
        testReservation.setStatus(ReservationStatus.PENDING);
    }

    @Test
    @DisplayName("Should place reservation successfully")
    void testPlaceReservation_Success() {
        // Given
        Reservation newReservation = new Reservation();
        newReservation.setBook(testBook);
        newReservation.setMember(testMember);

        when(bookRepository.findById(testBook.getId())).thenReturn(Optional.of(testBook));
        when(memberRepository.findById(testMember.getId())).thenReturn(Optional.of(testMember));
        when(reservationRepository.existsByBookIdAndMemberIdAndStatus(
                testBook.getId(), testMember.getId(), ReservationStatus.PENDING)).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        // When
        Reservation result = reservationService.placeReservation(newReservation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(result.getBook()).isEqualTo(testBook);
        assertThat(result.getMember()).isEqualTo(testMember);
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should throw exception when book not found for reservation")
    void testPlaceReservation_BookNotFound() {
        // Given
        Reservation newReservation = new Reservation();
        Book book = new Book();
        book.setId(99L);
        newReservation.setBook(book);

        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> reservationService.placeReservation(newReservation))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    @DisplayName("Should throw exception when book is available")
    void testPlaceReservation_BookAvailable() {
        // Given
        testBookCopy.setStatus(BookCopyStatus.AVAILABLE);
        Reservation newReservation = new Reservation();
        newReservation.setBook(testBook);
        newReservation.setMember(testMember);

        when(bookRepository.findById(testBook.getId())).thenReturn(Optional.of(testBook));

        // When/Then
        assertThatThrownBy(() -> reservationService.placeReservation(newReservation))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("available");
    }

    @Test
    @DisplayName("Should throw exception when member already has pending reservation")
    void testPlaceReservation_DuplicateReservation() {
        // Given
        Reservation newReservation = new Reservation();
        newReservation.setBook(testBook);
        newReservation.setMember(testMember);

        when(bookRepository.findById(testBook.getId())).thenReturn(Optional.of(testBook));
        when(reservationRepository.existsByBookIdAndMemberIdAndStatus(
                testBook.getId(), testMember.getId(), ReservationStatus.PENDING)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> reservationService.placeReservation(newReservation))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already has a pending reservation");
    }

    @Test
    @DisplayName("Should throw exception when member not found for reservation")
    void testPlaceReservation_MemberNotFound() {
        // Given
        Reservation newReservation = new Reservation();
        newReservation.setBook(testBook);
        Member member = new Member();
        member.setId(99L);
        newReservation.setMember(member);

        when(bookRepository.findById(testBook.getId())).thenReturn(Optional.of(testBook));
        when(reservationRepository.existsByBookIdAndMemberIdAndStatus(
                testBook.getId(), 99L, ReservationStatus.PENDING)).thenReturn(false);
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> reservationService.placeReservation(newReservation))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Member not found");
    }

    @Test
    @DisplayName("Should cancel reservation successfully")
    void testCancelReservation_Success() {
        // Given
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        // When
        reservationService.cancelReservation(1L);

        // Then
        verify(reservationRepository, times(1)).delete(testReservation);
    }

    @Test
    @DisplayName("Should throw exception when reservation not found for cancellation")
    void testCancelReservation_NotFound() {
        // Given
        when(reservationRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> reservationService.cancelReservation(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Reservation not found");
    }

    @Test
    @DisplayName("Should return true when book is reserved by other member")
    void testIsBookReservedByOther_True() {
        // Given
        Member otherMember = new Member();
        otherMember.setId(2L);
        testReservation.setMember(otherMember);

        when(reservationRepository.findFirstPendingReservation(testBook.getId()))
                .thenReturn(Optional.of(testReservation));

        // When
        boolean result = reservationService.isBookReservedByOther(testBook.getId(), testMember.getId());

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when book is reserved by same member")
    void testIsBookReservedByOther_False() {
        // Given
        when(reservationRepository.findFirstPendingReservation(testBook.getId()))
                .thenReturn(Optional.of(testReservation));

        // When
        boolean result = reservationService.isBookReservedByOther(testBook.getId(), testMember.getId());

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when no pending reservation")
    void testIsBookReservedByOther_NoReservation() {
        // Given
        when(reservationRepository.findFirstPendingReservation(testBook.getId()))
                .thenReturn(Optional.empty());

        // When
        boolean result = reservationService.isBookReservedByOther(testBook.getId(), testMember.getId());

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should get reservations for book successfully")
    void testGetReservationsForBook_Success() {
        // Given
        Reservation reservation2 = new Reservation();
        reservation2.setId(2L);
        reservation2.setBook(testBook);
        reservation2.setStatus(ReservationStatus.PENDING);

        List<Reservation> reservations = Arrays.asList(testReservation, reservation2);
        when(reservationRepository.findByBookIdAndStatusOrderByCreatedAtAsc(
                testBook.getId(), ReservationStatus.PENDING)).thenReturn(reservations);

        // When
        List<Reservation> result = reservationService.getReservationsForBook(testBook.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(testReservation, reservation2);
    }

    @Test
    @DisplayName("Should get queue position successfully")
    void testGetQueuePosition_Success() {
        // Given
        Member member2 = new Member();
        member2.setId(2L);
        Member member3 = new Member();
        member3.setId(3L);

        Reservation res1 = new Reservation();
        res1.setMember(member2);
        Reservation res2 = new Reservation();
        res2.setMember(testMember);
        Reservation res3 = new Reservation();
        res3.setMember(member3);

        List<Reservation> queue = Arrays.asList(res1, res2, res3);
        when(reservationRepository.findByBookIdAndStatusOrderByCreatedAtAsc(
                testBook.getId(), ReservationStatus.PENDING)).thenReturn(queue);

        // When
        int position = reservationService.getQueuePosition(testBook.getId(), testMember.getId());

        // Then
        assertThat(position).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return 0 when member not in queue")
    void testGetQueuePosition_NotInQueue() {
        // Given
        when(reservationRepository.findByBookIdAndStatusOrderByCreatedAtAsc(
                testBook.getId(), ReservationStatus.PENDING)).thenReturn(Arrays.asList());

        // When
        int position = reservationService.getQueuePosition(testBook.getId(), testMember.getId());

        // Then
        assertThat(position).isEqualTo(0);
    }

    @Test
    @DisplayName("Should check and notify next reservation")
    void testCheckAndNotifyNextReservation_Success() {
        // When
        reservationService.checkAndNotifyNextReservation(testBook.getId());

        // Then - method completes without exception
        verifyNoInteractions(reservationRepository);
    }
}
