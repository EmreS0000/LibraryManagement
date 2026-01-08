package com.library.management.controller;

import com.library.management.entity.ReadingListItem;
import com.library.management.service.ReadingListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reading-list")
@RequiredArgsConstructor
public class ReadingListController {

    private final ReadingListService readingListService;
    private final com.library.management.repository.MemberRepository memberRepository;

    @PostMapping("/{bookId}")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    public ResponseEntity<Void> addToReadingList(@PathVariable Long bookId) {
        Long memberId = getCurrentUserId();
        readingListService.addToReadingList(memberId, bookId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{bookId}")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    public ResponseEntity<Void> removeFromReadingList(@PathVariable Long bookId) {
        Long memberId = getCurrentUserId();
        readingListService.removeFromReadingList(memberId, bookId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    public ResponseEntity<List<ReadingListItem>> getReadingList() {
        Long memberId = getCurrentUserId();
        return ResponseEntity.ok(readingListService.getReadingList(memberId));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        String email = authentication.getName();
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found"))
                .getId();
    }
}
