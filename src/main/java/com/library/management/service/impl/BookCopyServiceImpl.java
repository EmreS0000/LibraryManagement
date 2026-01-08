package com.library.management.service.impl;

import com.library.management.entity.Book;
import com.library.management.entity.BookCopy;
import com.library.management.entity.BookCopyStatus;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.BookCopyRepository;
import com.library.management.repository.BookRepository;
import com.library.management.service.BookCopyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BookCopyServiceImpl implements BookCopyService {

    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;

    @Override
    public void addCopy(Long bookId, String barcode) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        if (barcode == null || barcode.trim().isEmpty()) {
            barcode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        if (bookCopyRepository.findByBarcode(barcode).isPresent()) {
            throw new BusinessException("Barcode already exists");
        }

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setBarcode(barcode);
        copy.setStatus(BookCopyStatus.AVAILABLE);

        bookCopyRepository.save(copy);
    }

    @Override
    public void removeCopy(Long copyId) {
        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new ResourceNotFoundException("Copy not found"));
        if (copy.getStatus() == BookCopyStatus.LOANED) {
            throw new BusinessException("Cannot delete loaned copy");
        }
        bookCopyRepository.delete(copy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookCopy> getCopiesByBookId(Long bookId) {
        return bookCopyRepository.findByBookId(bookId);
    }
}
