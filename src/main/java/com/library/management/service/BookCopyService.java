package com.library.management.service;

import com.library.management.entity.BookCopy;
import java.util.List;

public interface BookCopyService {
    void addCopy(Long bookId, String barcode);

    void removeCopy(Long copyId);

    List<BookCopy> getCopiesByBookId(Long bookId);
}
