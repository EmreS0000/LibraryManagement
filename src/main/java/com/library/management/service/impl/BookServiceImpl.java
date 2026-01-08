package com.library.management.service.impl;

import com.library.management.entity.Book;
import com.library.management.entity.BookCopy;
import com.library.management.entity.BookCopyStatus;
import com.library.management.entity.Category;
import com.library.management.entity.Publisher;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.BookRepository;
import com.library.management.repository.CategoryRepository;
import com.library.management.repository.PublisherRepository;
import com.library.management.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;

    @Override
    public Book createBook(Book book) {
        if (book.getIsbn() != null && bookRepository.existsByIsbn(book.getIsbn())) {
            throw new BusinessException("Book with ISBN " + book.getIsbn() + " already exists");
        }

        if (book.getPublisher() != null && book.getPublisher().getId() != null) {
            Publisher publisher = publisherRepository.findById(book.getPublisher().getId())
                    .orElseThrow(() -> new BusinessException("Publisher not found"));
            book.setPublisher(publisher);
        }

        if (book.getCategories() != null && !book.getCategories().isEmpty()) {
            Set<Category> categories = new HashSet<>();
            for (Category cat : book.getCategories()) {
                if (cat.getId() != null) {
                    Category category = categoryRepository.findById(cat.getId())
                            .orElseThrow(() -> new BusinessException("Category not found"));
                    categories.add(category);
                }
            }
            book.setCategories(categories);
        }

        return bookRepository.save(book);
    }

    @Override
    @Transactional(readOnly = true)
    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Book getBookByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ISBN: " + isbn));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Book> getAllBooks(String search, String title, String author, String isbn,
            Long categoryId, Pageable pageable) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasTitle = title != null && !title.isBlank();
        boolean hasAuthor = author != null && !author.isBlank();
        boolean hasIsbn = isbn != null && !isbn.isBlank();

        if (categoryId != null && hasSearch) {
            return bookRepository.searchBooksByCategory(categoryId, search, pageable);
        }

        if (categoryId != null) {
            return bookRepository.findByCategoriesId(categoryId, pageable);
        }

        if (hasSearch) {
            return bookRepository.searchBooks(search, pageable);
        }

        if (hasTitle) {
            return bookRepository.findByTitleContainingIgnoreCase(title, pageable);
        }

        if (hasAuthor) {
            return bookRepository.findByAuthorContainingIgnoreCase(author, pageable);
        }

        if (hasIsbn) {
                return bookRepository.findByIsbn(isbn)
                    .map(book -> new PageImpl<>(List.of(book), pageable, 1))
                    .orElseGet(() -> new PageImpl<>(List.of(), pageable, 0));
        }

        return bookRepository.findAll(pageable);
    }

    @Override
    public Book updateBook(Long id, Book book) {
        Book existingBook = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));

        existingBook.setTitle(book.getTitle());
        existingBook.setAuthor(book.getAuthor());
        existingBook.setIsbn(book.getIsbn());
        existingBook.setPublishYear(book.getPublishYear());
        existingBook.setPageCount(book.getPageCount());

        if (book.getPublisher() != null && book.getPublisher().getId() != null) {
            Publisher publisher = publisherRepository.findById(book.getPublisher().getId())
                    .orElseThrow(() -> new BusinessException("Publisher not found"));
            existingBook.setPublisher(publisher);
        } else {
            existingBook.setPublisher(null);
        }

        if (book.getCategories() != null) {
            Set<Category> categories = new HashSet<>();
            for (Category cat : book.getCategories()) {
                if (cat.getId() != null) {
                    Category category = categoryRepository.findById(cat.getId())
                            .orElseThrow(() -> new BusinessException("Category not found"));
                    categories.add(category);
                }
            }
            existingBook.setCategories(categories);
        }

        return bookRepository.save(existingBook);
    }

    @Override
    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book not found with id: " + id);
        }
        bookRepository.deleteById(id);
    }
}
