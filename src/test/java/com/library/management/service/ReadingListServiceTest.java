package com.library.management.service;

import com.library.management.entity.Book;
import com.library.management.entity.Member;
import com.library.management.entity.ReadingListItem;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.BookRepository;
import com.library.management.repository.MemberRepository;
import com.library.management.repository.ReadingListItemRepository;
import com.library.management.service.impl.ReadingListServiceImpl;
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
@DisplayName("ReadingListService Unit Tests")
class ReadingListServiceTest {

    @Mock
    private ReadingListItemRepository readingListItemRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private ReadingListServiceImpl readingListService;

    private Book testBook;
    private Member testMember;
    private ReadingListItem testReadingListItem;

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

        testReadingListItem = new ReadingListItem();
        testReadingListItem.setId(1L);
        testReadingListItem.setBook(testBook);
        testReadingListItem.setMember(testMember);
    }

    @Test
    @DisplayName("Should add to reading list successfully")
    void testAddToReadingList_Success() {
        // Given
        when(readingListItemRepository.existsByMemberIdAndBookId(1L, 1L)).thenReturn(false);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(readingListItemRepository.save(any(ReadingListItem.class))).thenReturn(testReadingListItem);

        // When
        readingListService.addToReadingList(1L, 1L);

        // Then
        verify(readingListItemRepository, times(1)).save(any(ReadingListItem.class));
    }

    @Test
    @DisplayName("Should throw exception when book already in reading list")
    void testAddToReadingList_AlreadyExists() {
        // Given
        when(readingListItemRepository.existsByMemberIdAndBookId(1L, 1L)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> readingListService.addToReadingList(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already in reading list");
        verify(readingListItemRepository, never()).save(any(ReadingListItem.class));
    }

    @Test
    @DisplayName("Should throw exception when member not found")
    void testAddToReadingList_MemberNotFound() {
        // Given
        when(readingListItemRepository.existsByMemberIdAndBookId(99L, 1L)).thenReturn(false);
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> readingListService.addToReadingList(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Member not found");
    }

    @Test
    @DisplayName("Should throw exception when book not found")
    void testAddToReadingList_BookNotFound() {
        // Given
        when(readingListItemRepository.existsByMemberIdAndBookId(1L, 99L)).thenReturn(false);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> readingListService.addToReadingList(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    @DisplayName("Should remove from reading list successfully")
    void testRemoveFromReadingList_Success() {
        // Given
        when(readingListItemRepository.findByMemberIdAndBookId(1L, 1L)).thenReturn(Optional.of(testReadingListItem));
        doNothing().when(readingListItemRepository).delete(testReadingListItem);

        // When
        readingListService.removeFromReadingList(1L, 1L);

        // Then
        verify(readingListItemRepository, times(1)).delete(testReadingListItem);
    }

    @Test
    @DisplayName("Should throw exception when reading list item not found for removal")
    void testRemoveFromReadingList_NotFound() {
        // Given
        when(readingListItemRepository.findByMemberIdAndBookId(1L, 1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> readingListService.removeFromReadingList(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Reading list item not found");
        verify(readingListItemRepository, never()).delete(any(ReadingListItem.class));
    }

    @Test
    @DisplayName("Should get reading list successfully")
    void testGetReadingList_Success() {
        // Given
        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Another Book");

        ReadingListItem item2 = new ReadingListItem();
        item2.setId(2L);
        item2.setBook(book2);
        item2.setMember(testMember);

        List<ReadingListItem> readingList = Arrays.asList(testReadingListItem, item2);
        when(readingListItemRepository.findByMemberId(1L)).thenReturn(readingList);

        // When
        List<ReadingListItem> result = readingListService.getReadingList(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(testReadingListItem, item2);
        verify(readingListItemRepository, times(1)).findByMemberId(1L);
    }

    @Test
    @DisplayName("Should return empty list when no reading list items found")
    void testGetReadingList_EmptyList() {
        // Given
        when(readingListItemRepository.findByMemberId(1L)).thenReturn(Arrays.asList());

        // When
        List<ReadingListItem> result = readingListService.getReadingList(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return true when book is in reading list")
    void testIsInReadingList_True() {
        // Given
        when(readingListItemRepository.existsByMemberIdAndBookId(1L, 1L)).thenReturn(true);

        // When
        boolean result = readingListService.isInReadingList(1L, 1L);

        // Then
        assertThat(result).isTrue();
        verify(readingListItemRepository, times(1)).existsByMemberIdAndBookId(1L, 1L);
    }

    @Test
    @DisplayName("Should return false when book is not in reading list")
    void testIsInReadingList_False() {
        // Given
        when(readingListItemRepository.existsByMemberIdAndBookId(1L, 1L)).thenReturn(false);

        // When
        boolean result = readingListService.isInReadingList(1L, 1L);

        // Then
        assertThat(result).isFalse();
        verify(readingListItemRepository, times(1)).existsByMemberIdAndBookId(1L, 1L);
    }

    @Test
    @DisplayName("Should handle multiple items in reading list")
    void testGetReadingList_MultipleItems() {
        // Given
        Book book2 = new Book();
        book2.setId(2L);
        Book book3 = new Book();
        book3.setId(3L);
        Book book4 = new Book();
        book4.setId(4L);

        ReadingListItem item2 = new ReadingListItem();
        item2.setBook(book2);
        ReadingListItem item3 = new ReadingListItem();
        item3.setBook(book3);
        ReadingListItem item4 = new ReadingListItem();
        item4.setBook(book4);

        List<ReadingListItem> readingList = Arrays.asList(testReadingListItem, item2, item3, item4);
        when(readingListItemRepository.findByMemberId(1L)).thenReturn(readingList);

        // When
        List<ReadingListItem> result = readingListService.getReadingList(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(4);
    }

    @Test
    @DisplayName("Should handle adding and checking reading list status")
    void testAddAndCheckReadingList() {
        // Given
        when(readingListItemRepository.existsByMemberIdAndBookId(1L, 1L)).thenReturn(false).thenReturn(true);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(readingListItemRepository.save(any(ReadingListItem.class))).thenReturn(testReadingListItem);

        // When
        readingListService.addToReadingList(1L, 1L);
        boolean isInList = readingListService.isInReadingList(1L, 1L);

        // Then
        assertThat(isInList).isTrue();
        verify(readingListItemRepository, times(1)).save(any(ReadingListItem.class));
        verify(readingListItemRepository, times(2)).existsByMemberIdAndBookId(1L, 1L);
    }

    @Test
    @DisplayName("Should handle adding multiple books to same member's reading list")
    void testAddMultipleBooksToReadingList() {
        // Given
        Book book2 = new Book();
        book2.setId(2L);

        when(readingListItemRepository.existsByMemberIdAndBookId(1L, 1L)).thenReturn(false);
        when(readingListItemRepository.existsByMemberIdAndBookId(1L, 2L)).thenReturn(false);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRepository.findById(2L)).thenReturn(Optional.of(book2));
        when(readingListItemRepository.save(any(ReadingListItem.class))).thenReturn(testReadingListItem);

        // When
        readingListService.addToReadingList(1L, 1L);
        readingListService.addToReadingList(1L, 2L);

        // Then
        verify(readingListItemRepository, times(2)).save(any(ReadingListItem.class));
    }
}
