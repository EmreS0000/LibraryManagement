package com.library.management.service;

import com.library.management.entity.*;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.BookRepository;
import com.library.management.repository.CategoryRepository;
import com.library.management.repository.PublisherRepository;
import com.library.management.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService Unit Tests")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PublisherRepository publisherRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book testBook;
    private Publisher testPublisher;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testPublisher = new Publisher();
        testPublisher.setId(1L);
        testPublisher.setName("Test Publisher");
        testPublisher.setCountry("USA");

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Fiction");
        testCategory.setStatus(Category.Status.ACTIVE);

        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setIsbn("978-1234567890");
        testBook.setPublisher(testPublisher);
        testBook.setCategories(new HashSet<>(Arrays.asList(testCategory)));
        testBook.setPublishYear(2023);
        testBook.setPageCount(300);
        testBook.setCopies(new HashSet<>());
    }

    @Test
    @DisplayName("Should create book successfully")
    void testCreateBook_Success() {
        // Given
        when(bookRepository.existsByIsbn(testBook.getIsbn())).thenReturn(false);
        when(publisherRepository.findById(testPublisher.getId())).thenReturn(Optional.of(testPublisher));
        when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        // When
        Book result = bookService.createBook(testBook);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Book");
        assertThat(result.getIsbn()).isEqualTo("978-1234567890");
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    @DisplayName("Should throw exception when creating book with duplicate ISBN")
    void testCreateBook_DuplicateIsbn() {
        // Given
        when(bookRepository.existsByIsbn(testBook.getIsbn())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> bookService.createBook(testBook))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    @DisplayName("Should throw exception when publisher not found during book creation")
    void testCreateBook_PublisherNotFound() {
        // Given
        when(bookRepository.existsByIsbn(testBook.getIsbn())).thenReturn(false);
        when(publisherRepository.findById(testPublisher.getId())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> bookService.createBook(testBook))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Publisher not found");
    }

    @Test
    @DisplayName("Should throw exception when category not found during book creation")
    void testCreateBook_CategoryNotFound() {
        // Given
        when(bookRepository.existsByIsbn(testBook.getIsbn())).thenReturn(false);
        when(publisherRepository.findById(testPublisher.getId())).thenReturn(Optional.of(testPublisher));
        when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> bookService.createBook(testBook))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    @DisplayName("Should get book by ID successfully")
    void testGetBookById_Success() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        // When
        Book result = bookService.getBookById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Book");
        verify(bookRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when book not found by ID")
    void testGetBookById_NotFound() {
        // Given
        when(bookRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> bookService.getBookById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    @DisplayName("Should get book by ISBN successfully")
    void testGetBookByIsbn_Success() {
        // Given
        when(bookRepository.findByIsbn("978-1234567890")).thenReturn(Optional.of(testBook));

        // When
        Book result = bookService.getBookByIsbn("978-1234567890");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsbn()).isEqualTo("978-1234567890");
        verify(bookRepository, times(1)).findByIsbn("978-1234567890");
    }

    @Test
    @DisplayName("Should throw exception when book not found by ISBN")
    void testGetBookByIsbn_NotFound() {
        // Given
        when(bookRepository.findByIsbn(anyString())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> bookService.getBookByIsbn("invalid-isbn"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ISBN");
    }

    @Test
    @DisplayName("Should get all books with pagination")
    void testGetAllBooks_Success() {
        // Given
        List<Book> bookList = Arrays.asList(testBook);
        Page<Book> bookPage = new PageImpl<>(bookList);
        Pageable pageable = PageRequest.of(0, 10);
        when(bookRepository.findAll(pageable)).thenReturn(bookPage);

        // When
        Page<Book> result = bookService.getAllBooks(null, null, null, null, null, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Book");
        verify(bookRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Should get books by category ID")
    void testGetAllBooks_ByCategory() {
        // Given
        List<Book> bookList = Arrays.asList(testBook);
        Page<Book> bookPage = new PageImpl<>(bookList);
        Pageable pageable = PageRequest.of(0, 10);
        when(bookRepository.findByCategoriesId(1L, pageable)).thenReturn(bookPage);

        // When
        Page<Book> result = bookService.getAllBooks(null, null, null, null, 1L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(bookRepository, times(1)).findByCategoriesId(1L, pageable);
        verify(bookRepository, never()).findAll(pageable);
    }

    @Test
    @DisplayName("Should update book successfully")
    void testUpdateBook_Success() {
        // Given
        Book updatedBook = new Book();
        updatedBook.setTitle("Updated Title");
        updatedBook.setAuthor("Updated Author");
        updatedBook.setIsbn("978-9876543210");
        updatedBook.setPublisher(testPublisher);
        updatedBook.setCategories(new HashSet<>(Arrays.asList(testCategory)));
        
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(publisherRepository.findById(testPublisher.getId())).thenReturn(Optional.of(testPublisher));
        when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        // When
        Book result = bookService.updateBook(1L, updatedBook);

        // Then
        assertThat(result).isNotNull();
        verify(bookRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent book")
    void testUpdateBook_NotFound() {
        // Given
        when(bookRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> bookService.updateBook(999L, testBook))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    @DisplayName("Should delete book successfully")
    void testDeleteBook_Success() {
        // Given
        when(bookRepository.existsById(1L)).thenReturn(true);
        doNothing().when(bookRepository).deleteById(1L);

        // When
        bookService.deleteBook(1L);

        // Then
        verify(bookRepository, times(1)).existsById(1L);
        verify(bookRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent book")
    void testDeleteBook_NotFound() {
        // Given
        when(bookRepository.existsById(anyLong())).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> bookService.deleteBook(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found");
        verify(bookRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Should handle book creation without publisher")
    void testCreateBook_WithoutPublisher() {
        // Given
        testBook.setPublisher(null);
        when(bookRepository.existsByIsbn(testBook.getIsbn())).thenReturn(false);
        when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        // When
        Book result = bookService.createBook(testBook);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPublisher()).isNull();
        verify(publisherRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should handle book creation without categories")
    void testCreateBook_WithoutCategories() {
        // Given
        testBook.setCategories(new HashSet<>());
        when(bookRepository.existsByIsbn(testBook.getIsbn())).thenReturn(false);
        when(publisherRepository.findById(testPublisher.getId())).thenReturn(Optional.of(testPublisher));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        // When
        Book result = bookService.createBook(testBook);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCategories()).isEmpty();
        verify(categoryRepository, never()).findById(anyLong());
    }
}
