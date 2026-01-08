package com.library.management.controller;

import com.library.management.entity.MembershipType;
import com.library.management.service.MembershipTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/membership-types")
@RequiredArgsConstructor
public class MembershipTypeController {

    private final MembershipTypeService membershipTypeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MembershipType> createMembershipType(@Valid @RequestBody MembershipType membershipType) {
        return ResponseEntity.ok(membershipTypeService.createMembershipType(membershipType));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MembershipType> updateMembershipType(@PathVariable Long id,
            @Valid @RequestBody MembershipType membershipType) {
        return ResponseEntity.ok(membershipTypeService.updateMembershipType(id, membershipType));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMembershipType(@PathVariable Long id) {
        membershipTypeService.deleteMembershipType(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MembershipType> getMembershipType(@PathVariable Long id) {
        return ResponseEntity.ok(membershipTypeService.getMembershipType(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MembershipType>> getAllMembershipTypes() {
        return ResponseEntity.ok(membershipTypeService.getAllMembershipTypes());
    }
}
