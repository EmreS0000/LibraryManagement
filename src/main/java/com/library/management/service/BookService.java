package com.library.management.service;

import com.library.management.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookService {
    Book createBook(Book book);

    Book getBookById(Long id);

    Book getBookByIsbn(String isbn);

    Page<Book> getAllBooks(String search, String title, String author, String isbn,
            Long categoryId, Pageable pageable);

    Book updateBook(Long id, Book book);

    void deleteBook(Long id);
}
