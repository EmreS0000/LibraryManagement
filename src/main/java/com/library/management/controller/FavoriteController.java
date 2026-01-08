package com.library.management.controller;

import com.library.management.entity.Favorite;
import com.library.management.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final com.library.management.repository.MemberRepository memberRepository;

    @PostMapping("/{bookId}")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    public ResponseEntity<Void> addFavorite(@PathVariable Long bookId) {
        Long memberId = getCurrentUserId();
        favoriteService.addFavorite(memberId, bookId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{bookId}")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long bookId) {
        Long memberId = getCurrentUserId();
        favoriteService.removeFavorite(memberId, bookId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    public ResponseEntity<List<Favorite>> getFavorites() {
        Long memberId = getCurrentUserId();
        return ResponseEntity.ok(favoriteService.getFavorites(memberId));
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
