package com.library.management.controller;

import com.library.management.entity.Book;
import com.library.management.entity.BookCopy;
import com.library.management.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "Book management APIs")
public class BookController {

    private final BookService bookService;
    private final com.library.management.service.BookCopyService bookCopyService;

    @PostMapping
    @Operation(summary = "Create a new book")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Book> createBook(@Valid @RequestBody Book book) {
        return new ResponseEntity<>(bookService.createBook(book), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @GetMapping
    @Operation(summary = "Get all books with pagination and search")
    public ResponseEntity<Page<Book>> getAllBooks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) Long categoryId,
            Pageable pageable) {
        return ResponseEntity.ok(bookService.getAllBooks(search, title, author, isbn, categoryId, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update book")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Book> updateBook(@PathVariable Long id, @Valid @RequestBody Book book) {
        return ResponseEntity.ok(bookService.updateBook(id, book));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete book")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    // Book Copy Endpoints

    @GetMapping("/{id}/copies")
    @Operation(summary = "Get copies of a book")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookCopy>> getBookCopies(@PathVariable Long id) {
        return ResponseEntity.ok(bookCopyService.getCopiesByBookId(id));
    }

    @PostMapping("/{id}/copies")
    @Operation(summary = "Add a copy to a book")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addBookCopy(@PathVariable Long id, @RequestParam(required = false) String barcode) {
        bookCopyService.addCopy(id, barcode);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/copies/{copyId}")
    @Operation(summary = "Delete a book copy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBookCopy(@PathVariable Long copyId) {
        bookCopyService.removeCopy(copyId);
        return ResponseEntity.noContent().build();
    }
}
