package com.library.management.service;

import com.library.management.entity.Member;
import com.library.management.entity.Role;
import com.library.management.exception.BusinessException;
import com.library.management.model.AuthModel;
import com.library.management.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.library.management.repository.MembershipTypeRepository membershipTypeRepository;

    @InjectMocks
    private AuthService authService;

    private Member testMember;
    private com.library.management.entity.MembershipType defaultMembershipType;

    @BeforeEach
    void setUp() {
        testMember = new Member();
        testMember.setId(1L);
        testMember.setFirstName("John");
        testMember.setLastName("Doe");
        testMember.setEmail("john.doe@example.com");
        testMember.setPhone("1234567890");
        testMember.setPassword("encodedPassword");
        testMember.setRole(Role.USER);
        testMember.setBalance(BigDecimal.ZERO);
        
        defaultMembershipType = new com.library.management.entity.MembershipType();
        defaultMembershipType.setId(1L);
        defaultMembershipType.setName("Standard");
        defaultMembershipType.setMaxBooks(5);
        defaultMembershipType.setMaxLoanDays(14);
    }

    @Test
    @DisplayName("Should register user successfully")
    void testRegister_Success() {
        // Given
        AuthModel.RegisterRequest request = new AuthModel.RegisterRequest(
                "John",
                "Doe",
                "john.doe@example.com",
                "1234567890",
                "password123"
        );

        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(membershipTypeRepository.findAll()).thenReturn(java.util.List.of(defaultMembershipType));
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // When
        AuthModel.AuthResponse response = authService.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("john.doe@example.com");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(response.token()).isNotNull();
        assertThat(response.token()).startsWith("Basic ");
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("Should register admin user with admin email")
    void testRegister_AdminUser() {
        // Given
        AuthModel.RegisterRequest request = new AuthModel.RegisterRequest(
                "Admin",
                "User",
                "admin@example.com",
                "1234567890",
                "adminPass"
        );

        Member adminMember = new Member();
        adminMember.setId(2L);
        adminMember.setEmail("admin@example.com");
        adminMember.setRole(Role.ADMIN);

        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(membershipTypeRepository.findAll()).thenReturn(java.util.List.of(defaultMembershipType));
        when(memberRepository.save(any(Member.class))).thenReturn(adminMember);

        // When
        AuthModel.AuthResponse response = authService.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.role()).isEqualTo("ADMIN");
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void testRegister_EmailExists() {
        // Given
        AuthModel.RegisterRequest request = new AuthModel.RegisterRequest(
                "John",
                "Doe",
                "john.doe@example.com",
                "1234567890",
                "password123"
        );

        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.of(testMember));

        // When/Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email already exists");
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLogin_Success() {
        // Given
        AuthModel.LoginRequest request = new AuthModel.LoginRequest(
                "john.doe@example.com",
                "password123"
        );

        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.of(testMember));
        when(passwordEncoder.matches(request.password(), testMember.getPassword())).thenReturn(true);

        // When
        AuthModel.AuthResponse response = authService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("john.doe@example.com");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(response.token()).isNotNull();
        assertThat(response.token()).startsWith("Basic ");
    }

    @Test
    @DisplayName("Should throw exception when email not found")
    void testLogin_EmailNotFound() {
        // Given
        AuthModel.LoginRequest request = new AuthModel.LoginRequest(
                "nonexistent@example.com",
                "password123"
        );

        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Should throw exception when password is incorrect")
    void testLogin_InvalidPassword() {
        // Given
        AuthModel.LoginRequest request = new AuthModel.LoginRequest(
                "john.doe@example.com",
                "wrongPassword"
        );

        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.of(testMember));
        when(passwordEncoder.matches(request.password(), testMember.getPassword())).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Should generate valid Basic authentication token")
    void testGenerateToken_ValidFormat() {
        // Given
        AuthModel.RegisterRequest request = new AuthModel.RegisterRequest(
                "John",
                "Doe",
                "john.doe@example.com",
                "1234567890",
                "password123"
        );

        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(membershipTypeRepository.findAll()).thenReturn(java.util.List.of(defaultMembershipType));
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // When
        AuthModel.AuthResponse response = authService.register(request);

        // Then
        assertThat(response.token()).matches("^Basic [A-Za-z0-9+/]+=*$");
    }

    @Test
    @DisplayName("Should handle registration with special characters in name")
    void testRegister_SpecialCharactersInName() {
        // Given
        AuthModel.RegisterRequest request = new AuthModel.RegisterRequest(
                "O'Brien",
                "Müller",
                "obrien@example.com",
                "1234567890",
                "password123"
        );

        Member member = new Member();
        member.setId(3L);
        member.setEmail("obrien@example.com");
        member.setRole(Role.USER);

        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(membershipTypeRepository.findAll()).thenReturn(java.util.List.of(defaultMembershipType));
        when(memberRepository.save(any(Member.class))).thenReturn(member);

        // When
        AuthModel.AuthResponse response = authService.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("obrien@example.com");
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("Should handle login with different email case")
    void testLogin_CaseInsensitiveEmail() {
        // Given
        AuthModel.LoginRequest request = new AuthModel.LoginRequest(
                "JOHN.DOE@EXAMPLE.COM",
                "password123"
        );

        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.of(testMember));
        when(passwordEncoder.matches(request.password(), testMember.getPassword())).thenReturn(true);

        // When
        AuthModel.AuthResponse response = authService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should handle registration with minimum valid data")
    void testRegister_MinimumData() {
        // Given
        AuthModel.RegisterRequest request = new AuthModel.RegisterRequest(
                "A",
                "B",
                "a@b.c",
                "1",
                "pass"
        );

        Member member = new Member();
        member.setId(4L);
        member.setEmail("a@b.c");
        member.setRole(Role.USER);

        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(membershipTypeRepository.findAll()).thenReturn(java.util.List.of(defaultMembershipType));
        when(memberRepository.save(any(Member.class))).thenReturn(member);

        // When
        AuthModel.AuthResponse response = authService.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("a@b.c");
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("Should encode password during registration")
    void testRegister_PasswordEncoded() {
        // Given
        AuthModel.RegisterRequest request = new AuthModel.RegisterRequest(
                "John",
                "Doe",
                "john.doe@example.com",
                "1234567890",
                "plainPassword"
        );

        when(memberRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");        when(membershipTypeRepository.findAll()).thenReturn(java.util.List.of(defaultMembershipType));        when(membershipTypeRepository.findAll()).thenReturn(java.util.List.of(defaultMembershipType));
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            assertThat(member.getPassword()).isEqualTo("encodedPassword");
            return testMember;
        });

        // When
        authService.register(request);

        // Then
        verify(passwordEncoder, times(1)).encode("plainPassword");
    }
}
