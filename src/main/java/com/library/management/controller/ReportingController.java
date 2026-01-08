package com.library.management.controller;

import com.library.management.model.BookStatusReport;
import com.library.management.model.CategoryReport;
import com.library.management.model.MemberActivity;
import com.library.management.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reporting")
@RequiredArgsConstructor
@Tag(name = "Reporting", description = "Analytics and Reporting APIs")
@PreAuthorize("hasRole('ADMIN')")
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/categories/most-read")
    @Operation(summary = "Get most read categories")
    public ResponseEntity<List<CategoryReport>> getMostReadCategories(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(reportingService.getMostReadCategories(limit));
    }

    @GetMapping("/members/most-active")
    @Operation(summary = "Get most active members")
    public ResponseEntity<List<MemberActivity>> getMostActiveMembers(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(reportingService.getMostActiveMembers(limit));
    }

    @GetMapping("/books/status-distribution")
    @Operation(summary = "Get book status distribution")
    public ResponseEntity<List<BookStatusReport>> getBookStatusDistribution() {
        return ResponseEntity.ok(reportingService.getBookStatusDistribution());
    }
}
