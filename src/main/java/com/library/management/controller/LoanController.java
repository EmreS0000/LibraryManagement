package com.library.management.controller;

import com.library.management.entity.Loan;
import com.library.management.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Loan Management APIs")
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/borrow")
    @Operation(summary = "Borrow a book")
    public ResponseEntity<Loan> borrowBook(@RequestBody Loan loan) {
        if (loan.getMember() == null || loan.getMember().getId() == null ||
            loan.getBookCopy() == null || (loan.getBookCopy().getId() == null && 
            (loan.getBookCopy().getBarcode() == null || loan.getBookCopy().getBarcode().isEmpty()))) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(loanService.borrowBook(loan), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/return")
    @Operation(summary = "Return a borrowed book")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Loan> returnBook(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.returnBook(id));
    }

    @GetMapping("/admin/all")
    @Operation(summary = "Get all loans (Admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Loan>> getAllLoansAdmin() {
        // Assuming LoanService has getAllLoans or similar
        // Using existing findAll but mapping to DTO list?
        // Service method is needed if only Page based findAll exists.
        // For now, let's implement a simple findAll in Service or Repository call here
        return ResponseEntity.ok(loanService.getAllLoans());
    }

    @GetMapping
    @Operation(summary = "Get all loans")
    public ResponseEntity<List<Loan>> getAllLoans() {
        return ResponseEntity.ok(loanService.getAllLoans());
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "Get loans by member")
    public ResponseEntity<List<Loan>> getLoansByMember(@PathVariable Long memberId) {
        return ResponseEntity.ok(loanService.getLoansByMember(memberId));
    }
}
