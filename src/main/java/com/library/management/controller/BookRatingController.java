package com.library.management.controller;

import com.library.management.entity.BookRating;
import com.library.management.service.BookRatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class BookRatingController {

    private final BookRatingService bookRatingService;
    private final com.library.management.repository.MemberRepository memberRepository;

    @PostMapping
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<BookRating> addRating(@RequestBody BookRating bookRating) {
        if (bookRating.getBook() == null || bookRating.getBook().getId() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (bookRating.getScore() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Long memberId = getCurrentUserId();
        return ResponseEntity.ok(bookRatingService.addRating(memberId, bookRating.getBook().getId(), 
                                                               bookRating.getScore(), bookRating.getComment()));
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<BookRating>> getRatingsForBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(bookRatingService.getRatingsForBook(bookId));
    }

    @GetMapping("/average/{bookId}")
    public ResponseEntity<Double> getAverageRating(@PathVariable Long bookId) {
        return ResponseEntity.ok(bookRatingService.getAverageRating(bookId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRating(@PathVariable Long id) {
        bookRatingService.deleteRating(id);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new com.library.management.exception.BusinessException("User not authenticated");
        }
        String email = authentication.getName();
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new com.library.management.exception.ResourceNotFoundException(
                        "Member not found for email: " + email))
                .getId();
    }
}
