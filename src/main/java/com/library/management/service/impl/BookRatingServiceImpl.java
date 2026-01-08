package com.library.management.service.impl;

import com.library.management.entity.Book;
import com.library.management.entity.BookRating;
import com.library.management.entity.Member;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.BookRatingRepository;
import com.library.management.repository.BookRepository;
import com.library.management.repository.LoanRepository;
import com.library.management.repository.MemberRepository;
import com.library.management.service.BookRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookRatingServiceImpl implements BookRatingService {

    private final BookRatingRepository bookRatingRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    @Override
    public BookRating addRating(Long memberId, Long bookId, Integer score, String comment) {
        if (bookRatingRepository.existsByBookIdAndMemberId(bookId, memberId)) {
            throw new BusinessException("You have already rated this book.");
        }

        // Verify borrowing history
        if (!loanRepository.existsByMemberIdAndBookCopyBookId(memberId, bookId)) {
            throw new BusinessException("You can only rate books you have borrowed.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        BookRating rating = new BookRating();
        rating.setMember(member);
        rating.setBook(book);
        rating.setScore(score);
        rating.setComment(comment);

        return bookRatingRepository.save(rating);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookRating> getRatingsForBook(Long bookId) {
        return bookRatingRepository.findByBookId(bookId);
    }

    @Override
    public void deleteRating(Long ratingId) {
        if (!bookRatingRepository.existsById(ratingId)) {
            throw new ResourceNotFoundException("Rating not found");
        }
        bookRatingRepository.deleteById(ratingId);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRating(Long bookId) {
        Double average = bookRatingRepository.getAverageRating(bookId);
        return average != null ? average : 0.0;
    }
}
