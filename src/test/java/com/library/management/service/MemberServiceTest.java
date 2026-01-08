package com.library.management.service;

import com.library.management.entity.Member;
import com.library.management.entity.MembershipType;
import com.library.management.entity.Role;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.MemberRepository;
import com.library.management.service.impl.MemberServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService Unit Tests")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberServiceImpl memberService;

    private Member testMember;
    private MembershipType testMembershipType;

    @BeforeEach
    void setUp() {
        testMembershipType = new MembershipType();
        testMembershipType.setId(1L);
        testMembershipType.setName("Standard");
        testMembershipType.setMaxBooks(5);
        testMembershipType.setMaxLoanDays(14);

        testMember = new Member();
        testMember.setId(1L);
        testMember.setFirstName("John");
        testMember.setLastName("Doe");
        testMember.setEmail("john.doe@example.com");
        testMember.setPhone("1234567890");
        testMember.setPassword("password123");
        testMember.setRole(Role.USER);
        testMember.setBalance(BigDecimal.ZERO);
        testMember.setMembershipType(testMembershipType);
    }

    @Test
    @DisplayName("Should create member successfully")
    void testCreateMember_Success() {
        // Given
        when(memberRepository.existsByEmail(testMember.getEmail())).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // When
        Member result = memberService.createMember(testMember);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("Should throw exception when creating member with duplicate email")
    void testCreateMember_DuplicateEmail() {
        // Given
        when(memberRepository.existsByEmail(testMember.getEmail())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> memberService.createMember(testMember))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("Should get member by ID successfully")
    void testGetMemberById_Success() {
        // Given
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));

        // When
        Member result = memberService.getMemberById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        verify(memberRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when member not found by ID")
    void testGetMemberById_NotFound() {
        // Given
        when(memberRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> memberService.getMemberById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Member not found");
    }

    @Test
    @DisplayName("Should get all members successfully")
    void testGetAllMembers_Success() {
        // Given
        Member member2 = new Member();
        member2.setId(2L);
        member2.setFirstName("Jane");
        member2.setLastName("Smith");
        member2.setEmail("jane.smith@example.com");
        member2.setPhone("0987654321");
        member2.setRole(Role.USER);

        List<Member> members = Arrays.asList(testMember, member2);
        when(memberRepository.findAll()).thenReturn(members);

        // When
        List<Member> result = memberService.getAllMembers();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(testMember, member2);
        verify(memberRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should update member successfully")
    void testUpdateMember_Success() {
        // Given
        Member updatedMember = new Member();
        updatedMember.setFirstName("John Updated");
        updatedMember.setLastName("Doe Updated");
        updatedMember.setEmail("john.updated@example.com");
        updatedMember.setPhone("5555555555");
        updatedMember.setMembershipType(testMembershipType);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // When
        Member result = memberService.updateMember(1L, updatedMember);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("John Updated");
        assertThat(result.getLastName()).isEqualTo("Doe Updated");
        assertThat(result.getEmail()).isEqualTo("john.updated@example.com");
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent member")
    void testUpdateMember_NotFound() {
        // Given
        Member updatedMember = new Member();
        updatedMember.setFirstName("Updated");

        when(memberRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> memberService.updateMember(99L, updatedMember))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Member not found");
    }

    @Test
    @DisplayName("Should delete member successfully")
    void testDeleteMember_Success() {
        // Given
        when(memberRepository.existsById(1L)).thenReturn(true);
        doNothing().when(memberRepository).deleteById(1L);

        // When
        memberService.deleteMember(1L);

        // Then
        verify(memberRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent member")
    void testDeleteMember_NotFound() {
        // Given
        when(memberRepository.existsById(anyLong())).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> memberService.deleteMember(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Member not found");
        verify(memberRepository, never()).deleteById(anyLong());
    }
}
