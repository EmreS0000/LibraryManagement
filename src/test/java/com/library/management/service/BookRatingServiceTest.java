package com.library.management.service;

import com.library.management.entity.Book;
import com.library.management.entity.BookRating;
import com.library.management.entity.Member;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.BookRatingRepository;
import com.library.management.repository.BookRepository;
import com.library.management.repository.LoanRepository;
import com.library.management.repository.MemberRepository;
import com.library.management.service.impl.BookRatingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookRatingService Unit Tests")
class BookRatingServiceTest {

    @Mock
    private BookRatingRepository bookRatingRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private BookRatingServiceImpl bookRatingService;

    private Book testBook;
    private Member testMember;
    private BookRating testRating;

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("Test Book");
        testBook.setIsbn("978-1234567890");

        testMember = new Member();
        testMember.setId(1L);
        testMember.setFirstName("John");
        testMember.setLastName("Doe");
        testMember.setEmail("john.doe@example.com");

        testRating = new BookRating();
        testRating.setId(1L);
        testRating.setBook(testBook);
        testRating.setMember(testMember);
        testRating.setScore(5);
        testRating.setComment("Excellent book!");
    }

    @Test
    @DisplayName("Should add rating successfully")
    void testAddRating_Success() {
        // Given
        when(bookRatingRepository.existsByBookIdAndMemberId(1L, 1L)).thenReturn(false);
        when(loanRepository.existsByMemberIdAndBookCopyBookId(1L, 1L)).thenReturn(true);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRatingRepository.save(any(BookRating.class))).thenReturn(testRating);

        // When
        BookRating result = bookRatingService.addRating(1L, 1L, 5, "Excellent book!");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getScore()).isEqualTo(5);
        assertThat(result.getComment()).isEqualTo("Excellent book!");
        verify(bookRatingRepository, times(1)).save(any(BookRating.class));
    }

    @Test
    @DisplayName("Should throw exception when member already rated the book")
    void testAddRating_AlreadyRated() {
        // Given
        when(bookRatingRepository.existsByBookIdAndMemberId(1L, 1L)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> bookRatingService.addRating(1L, 1L, 5, "Great!"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already rated");
    }

    @Test
    @DisplayName("Should throw exception when member has not borrowed the book")
    void testAddRating_NotBorrowed() {
        // Given
        when(bookRatingRepository.existsByBookIdAndMemberId(1L, 1L)).thenReturn(false);
        when(loanRepository.existsByMemberIdAndBookCopyBookId(1L, 1L)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> bookRatingService.addRating(1L, 1L, 5, "Great!"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("can only rate books you have borrowed");
    }

    @Test
    @DisplayName("Should throw exception when member not found")
    void testAddRating_MemberNotFound() {
        // Given
        when(bookRatingRepository.existsByBookIdAndMemberId(1L, 99L)).thenReturn(false);
        when(loanRepository.existsByMemberIdAndBookCopyBookId(99L, 1L)).thenReturn(true);
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> bookRatingService.addRating(99L, 1L, 5, "Great!"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Member not found");
    }

    @Test
    @DisplayName("Should throw exception when book not found")
    void testAddRating_BookNotFound() {
        // Given
        when(bookRatingRepository.existsByBookIdAndMemberId(99L, 1L)).thenReturn(false);
        when(loanRepository.existsByMemberIdAndBookCopyBookId(1L, 99L)).thenReturn(true);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> bookRatingService.addRating(1L, 99L, 5, "Great!"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    @DisplayName("Should get ratings for book successfully")
    void testGetRatingsForBook_Success() {
        // Given
        Member member2 = new Member();
        member2.setId(2L);

        BookRating rating2 = new BookRating();
        rating2.setId(2L);
        rating2.setBook(testBook);
        rating2.setMember(member2);
        rating2.setScore(4);
        rating2.setComment("Good book");

        List<BookRating> ratings = Arrays.asList(testRating, rating2);
        when(bookRatingRepository.findByBookId(1L)).thenReturn(ratings);

        // When
        List<BookRating> result = bookRatingService.getRatingsForBook(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(testRating, rating2);
        verify(bookRatingRepository, times(1)).findByBookId(1L);
    }

    @Test
    @DisplayName("Should return empty list when no ratings found")
    void testGetRatingsForBook_EmptyList() {
        // Given
        when(bookRatingRepository.findByBookId(1L)).thenReturn(Arrays.asList());

        // When
        List<BookRating> result = bookRatingService.getRatingsForBook(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should delete rating successfully")
    void testDeleteRating_Success() {
        // Given
        when(bookRatingRepository.existsById(1L)).thenReturn(true);
        doNothing().when(bookRatingRepository).deleteById(1L);

        // When
        bookRatingService.deleteRating(1L);

        // Then
        verify(bookRatingRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent rating")
    void testDeleteRating_NotFound() {
        // Given
        when(bookRatingRepository.existsById(anyLong())).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> bookRatingService.deleteRating(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Rating not found");
        verify(bookRatingRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Should get average rating successfully")
    void testGetAverageRating_Success() {
        // Given
        when(bookRatingRepository.getAverageRating(1L)).thenReturn(4.5);

        // When
        Double result = bookRatingService.getAverageRating(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(4.5);
        verify(bookRatingRepository, times(1)).getAverageRating(1L);
    }

    @Test
    @DisplayName("Should return 0.0 when no ratings exist for average")
    void testGetAverageRating_NoRatings() {
        // Given
        when(bookRatingRepository.getAverageRating(1L)).thenReturn(null);

        // When
        Double result = bookRatingService.getAverageRating(1L);

        // Then
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should add rating with minimum score")
    void testAddRating_MinimumScore() {
        // Given
        when(bookRatingRepository.existsByBookIdAndMemberId(1L, 1L)).thenReturn(false);
        when(loanRepository.existsByMemberIdAndBookCopyBookId(1L, 1L)).thenReturn(true);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRatingRepository.save(any(BookRating.class))).thenReturn(testRating);

        // When
        BookRating result = bookRatingService.addRating(1L, 1L, 1, "Not good");

        // Then
        assertThat(result).isNotNull();
        verify(bookRatingRepository, times(1)).save(any(BookRating.class));
    }

    @Test
    @DisplayName("Should add rating without comment")
    void testAddRating_NoComment() {
        // Given
        when(bookRatingRepository.existsByBookIdAndMemberId(1L, 1L)).thenReturn(false);
        when(loanRepository.existsByMemberIdAndBookCopyBookId(1L, 1L)).thenReturn(true);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRatingRepository.save(any(BookRating.class))).thenReturn(testRating);

        // When
        BookRating result = bookRatingService.addRating(1L, 1L, 4, null);

        // Then
        assertThat(result).isNotNull();
        verify(bookRatingRepository, times(1)).save(any(BookRating.class));
    }
}
