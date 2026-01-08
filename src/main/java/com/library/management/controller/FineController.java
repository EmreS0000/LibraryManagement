package com.library.management.controller;

import com.library.management.entity.Fine;
import com.library.management.service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Fine>> getAllFines() {
        return ResponseEntity.ok(fineService.getAllFines());
    }

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#memberId)")
    public ResponseEntity<List<Fine>> getFinesByMember(@PathVariable Long memberId) {
        return ResponseEntity.ok(fineService.getFinesByMember(memberId));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> payFine(@PathVariable Long id) {
        fineService.payFine(id);
        return ResponseEntity.ok().build();
    }
}
