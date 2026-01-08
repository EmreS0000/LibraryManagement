package com.library.management.service;

import com.library.management.entity.Book;
import com.library.management.entity.Favorite;
import com.library.management.entity.Member;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.BookRepository;
import com.library.management.repository.FavoriteRepository;
import com.library.management.repository.MemberRepository;
import com.library.management.service.impl.FavoriteServiceImpl;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoriteService Unit Tests")
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private FavoriteServiceImpl favoriteService;

    private Book testBook;
    private Member testMember;
    private Favorite testFavorite;

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

        testFavorite = new Favorite();
        testFavorite.setId(1L);
        testFavorite.setBook(testBook);
        testFavorite.setMember(testMember);
    }

    @Test
    @DisplayName("Should add favorite successfully")
    void testAddFavorite_Success() {
        // Given
        when(favoriteRepository.existsByMemberIdAndBookId(1L, 1L)).thenReturn(false);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(favoriteRepository.save(any(Favorite.class))).thenReturn(testFavorite);

        // When
        favoriteService.addFavorite(1L, 1L);

        // Then
        verify(favoriteRepository, times(1)).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Should throw exception when book already in favorites")
    void testAddFavorite_AlreadyExists() {
        // Given
        when(favoriteRepository.existsByMemberIdAndBookId(1L, 1L)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> favoriteService.addFavorite(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already in favorites");
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Should throw exception when member not found")
    void testAddFavorite_MemberNotFound() {
        // Given
        when(favoriteRepository.existsByMemberIdAndBookId(99L, 1L)).thenReturn(false);
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> favoriteService.addFavorite(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Member not found");
    }

    @Test
    @DisplayName("Should throw exception when book not found")
    void testAddFavorite_BookNotFound() {
        // Given
        when(favoriteRepository.existsByMemberIdAndBookId(1L, 99L)).thenReturn(false);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> favoriteService.addFavorite(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    @DisplayName("Should remove favorite successfully")
    void testRemoveFavorite_Success() {
        // Given
        when(favoriteRepository.findByMemberIdAndBookId(1L, 1L)).thenReturn(Optional.of(testFavorite));
        doNothing().when(favoriteRepository).delete(testFavorite);

        // When
        favoriteService.removeFavorite(1L, 1L);

        // Then
        verify(favoriteRepository, times(1)).delete(testFavorite);
    }

    @Test
    @DisplayName("Should throw exception when favorite not found for removal")
    void testRemoveFavorite_NotFound() {
        // Given
        when(favoriteRepository.findByMemberIdAndBookId(1L, 1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> favoriteService.removeFavorite(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Favorite not found");
        verify(favoriteRepository, never()).delete(any(Favorite.class));
    }

    @Test
    @DisplayName("Should get favorites successfully")
    void testGetFavorites_Success() {
        // Given
        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Another Book");

        Favorite favorite2 = new Favorite();
        favorite2.setId(2L);
        favorite2.setBook(book2);
        favorite2.setMember(testMember);

        List<Favorite> favorites = Arrays.asList(testFavorite, favorite2);
        when(favoriteRepository.findByMemberId(1L)).thenReturn(favorites);

        // When
        List<Favorite> result = favoriteService.getFavorites(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(testFavorite, favorite2);
        verify(favoriteRepository, times(1)).findByMemberId(1L);
    }

    @Test
    @DisplayName("Should return empty list when no favorites found")
    void testGetFavorites_EmptyList() {
        // Given
        when(favoriteRepository.findByMemberId(1L)).thenReturn(Arrays.asList());

        // When
        List<Favorite> result = favoriteService.getFavorites(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return true when book is favorite")
    void testIsFavorite_True() {
        // Given
        when(favoriteRepository.existsByMemberIdAndBookId(1L, 1L)).thenReturn(true);

        // When
        boolean result = favoriteService.isFavorite(1L, 1L);

        // Then
        assertThat(result).isTrue();
        verify(favoriteRepository, times(1)).existsByMemberIdAndBookId(1L, 1L);
    }

    @Test
    @DisplayName("Should return false when book is not favorite")
    void testIsFavorite_False() {
        // Given
        when(favoriteRepository.existsByMemberIdAndBookId(1L, 1L)).thenReturn(false);

        // When
        boolean result = favoriteService.isFavorite(1L, 1L);

        // Then
        assertThat(result).isFalse();
        verify(favoriteRepository, times(1)).existsByMemberIdAndBookId(1L, 1L);
    }

    @Test
    @DisplayName("Should handle multiple favorites for same member")
    void testGetFavorites_MultipleFavorites() {
        // Given
        Book book2 = new Book();
        book2.setId(2L);
        Book book3 = new Book();
        book3.setId(3L);

        Favorite favorite2 = new Favorite();
        favorite2.setBook(book2);
        Favorite favorite3 = new Favorite();
        favorite3.setBook(book3);

        List<Favorite> favorites = Arrays.asList(testFavorite, favorite2, favorite3);
        when(favoriteRepository.findByMemberId(1L)).thenReturn(favorites);

        // When
        List<Favorite> result = favoriteService.getFavorites(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("Should handle adding and checking favorite status")
    void testAddAndCheckFavorite() {
        // Given
        when(favoriteRepository.existsByMemberIdAndBookId(1L, 1L)).thenReturn(false).thenReturn(true);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(favoriteRepository.save(any(Favorite.class))).thenReturn(testFavorite);

        // When
        favoriteService.addFavorite(1L, 1L);
        boolean isFavorite = favoriteService.isFavorite(1L, 1L);

        // Then
        assertThat(isFavorite).isTrue();
        verify(favoriteRepository, times(1)).save(any(Favorite.class));
        verify(favoriteRepository, times(2)).existsByMemberIdAndBookId(1L, 1L);
    }
}
