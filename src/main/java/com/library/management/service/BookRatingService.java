package com.library.management.service;

import com.library.management.entity.BookRating;
import java.util.List;

public interface BookRatingService {
    BookRating addRating(Long memberId, Long bookId, Integer score, String comment);

    List<BookRating> getRatingsForBook(Long bookId);

    void deleteRating(Long ratingId);

    Double getAverageRating(Long bookId);
}
