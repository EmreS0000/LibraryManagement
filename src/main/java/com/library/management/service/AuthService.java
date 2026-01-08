package com.library.management.service;

import com.library.management.model.AuthModel;
import com.library.management.entity.Member;
import com.library.management.entity.Role;
import com.library.management.entity.MembershipType;
import com.library.management.exception.BusinessException;
import com.library.management.repository.MemberRepository;
import com.library.management.repository.MembershipTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MembershipTypeRepository membershipTypeRepository;

    @Transactional
    public AuthModel.AuthResponse register(AuthModel.RegisterRequest request) {
        // Check if email already exists
        if (memberRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException("Email already exists");
        }

        Member member = new Member();
        member.setFirstName(request.firstName());
        member.setLastName(request.lastName());
        member.setEmail(request.email());
        member.setPhone(request.phone());
        member.setPassword(passwordEncoder.encode(request.password()));
        member.setRole(request.email().contains("admin") ? Role.ADMIN : Role.USER);
        member.setBalance(java.math.BigDecimal.ZERO);
        member.setCreatedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        
        // Assign default membership type to new members
        MembershipType defaultMembership = membershipTypeRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("No membership types available. Please contact administrator."));
        member.setMembershipType(defaultMembership);

        try {
            Member savedMember = memberRepository.save(member);
            String token = generateBasicToken(request.email(), request.password());
            return new AuthModel.AuthResponse(savedMember.getId(), savedMember.getEmail(), savedMember.getRole().name(), token);
        } catch (Exception e) {
            // Log the actual error for debugging
            System.err.println("Registration error: " + e.getMessage());
            e.printStackTrace();
            throw new BusinessException("Registration failed: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public AuthModel.AuthResponse login(AuthModel.LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = generateBasicToken(request.email(), request.password());
        return new AuthModel.AuthResponse(member.getId(), member.getEmail(), member.getRole().name(), token);
    }

    private String generateBasicToken(String email, String password) {
        String auth = email + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }
}
