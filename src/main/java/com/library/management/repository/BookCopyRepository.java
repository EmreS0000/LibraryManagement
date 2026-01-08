package com.library.management.repository;

import com.library.management.entity.BookCopy;
import com.library.management.entity.BookCopyStatus;
import com.library.management.model.BookStatusReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {
    Optional<BookCopy> findByBarcode(String barcode);

    List<BookCopy> findByBookId(Long bookId);

    long countByBookId(Long bookId);

    long countByBookIdAndStatus(Long bookId, BookCopyStatus status);

    @Query("SELECT new com.library.management.model.BookStatusReport(CAST(bc.status AS string), COUNT(bc)) " +
            "FROM BookCopy bc GROUP BY bc.status")
    List<BookStatusReport> countByStatusGrouped();
}
