package com.library.management.repository;

import com.library.management.entity.BookRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRatingRepository extends JpaRepository<BookRating, Long> {

    List<BookRating> findByBookId(Long bookId);

    boolean existsByBookIdAndMemberId(Long bookId, Long memberId);

    @Query("SELECT AVG(r.score) FROM BookRating r WHERE r.book.id = :bookId")
    Double getAverageRating(@Param("bookId") Long bookId);
}
