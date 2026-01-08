package com.library.management.service;

import com.library.management.entity.MembershipType;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.MembershipTypeRepository;
import com.library.management.service.impl.MembershipTypeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MembershipTypeService Unit Tests")
class MembershipTypeServiceTest {

    @Mock
    private MembershipTypeRepository membershipTypeRepository;

    @InjectMocks
    private MembershipTypeServiceImpl membershipTypeService;

    private MembershipType testMembershipType;

    @BeforeEach
    void setUp() {
        testMembershipType = new MembershipType();
        testMembershipType.setId(1L);
        testMembershipType.setName("Standard");
        testMembershipType.setMaxBooks(5);
        testMembershipType.setMaxLoanDays(14);
    }

    @Test
    @DisplayName("Should create membership type successfully")
    void testCreateMembershipType_Success() {
        // Given
        when(membershipTypeRepository.save(any(MembershipType.class))).thenReturn(testMembershipType);

        // When
        MembershipType result = membershipTypeService.createMembershipType(testMembershipType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Standard");
        assertThat(result.getMaxBooks()).isEqualTo(5);
        assertThat(result.getMaxLoanDays()).isEqualTo(14);
        verify(membershipTypeRepository, times(1)).save(any(MembershipType.class));
    }

    @Test
    @DisplayName("Should get membership type by ID successfully")
    void testGetMembershipType_Success() {
        // Given
        when(membershipTypeRepository.findById(1L)).thenReturn(Optional.of(testMembershipType));

        // When
        MembershipType result = membershipTypeService.getMembershipType(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Standard");
        verify(membershipTypeRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when membership type not found by ID")
    void testGetMembershipType_NotFound() {
        // Given
        when(membershipTypeRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> membershipTypeService.getMembershipType(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Membership Type not found");
    }

    @Test
    @DisplayName("Should get all membership types successfully")
    void testGetAllMembershipTypes_Success() {
        // Given
        MembershipType premiumType = new MembershipType();
        premiumType.setId(2L);
        premiumType.setName("Premium");
        premiumType.setMaxBooks(10);
        premiumType.setMaxLoanDays(30);

        List<MembershipType> types = Arrays.asList(testMembershipType, premiumType);
        when(membershipTypeRepository.findAll()).thenReturn(types);

        // When
        List<MembershipType> result = membershipTypeService.getAllMembershipTypes();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(testMembershipType, premiumType);
        verify(membershipTypeRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should update membership type successfully")
    void testUpdateMembershipType_Success() {
        // Given
        MembershipType updatedType = new MembershipType();
        updatedType.setName("Premium");
        updatedType.setMaxBooks(10);
        updatedType.setMaxLoanDays(30);

        when(membershipTypeRepository.findById(1L)).thenReturn(Optional.of(testMembershipType));
        when(membershipTypeRepository.save(any(MembershipType.class))).thenReturn(testMembershipType);

        // When
        MembershipType result = membershipTypeService.updateMembershipType(1L, updatedType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Premium");
        assertThat(result.getMaxBooks()).isEqualTo(10);
        assertThat(result.getMaxLoanDays()).isEqualTo(30);
        verify(membershipTypeRepository, times(1)).save(any(MembershipType.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent membership type")
    void testUpdateMembershipType_NotFound() {
        // Given
        MembershipType updatedType = new MembershipType();
        updatedType.setName("Updated");

        when(membershipTypeRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> membershipTypeService.updateMembershipType(99L, updatedType))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Membership Type not found");
    }

    @Test
    @DisplayName("Should delete membership type successfully")
    void testDeleteMembershipType_Success() {
        // Given
        when(membershipTypeRepository.existsById(1L)).thenReturn(true);
        doNothing().when(membershipTypeRepository).deleteById(1L);

        // When
        membershipTypeService.deleteMembershipType(1L);

        // Then
        verify(membershipTypeRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent membership type")
    void testDeleteMembershipType_NotFound() {
        // Given
        when(membershipTypeRepository.existsById(anyLong())).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> membershipTypeService.deleteMembershipType(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Membership Type not found");
        verify(membershipTypeRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Should handle membership type with zero max books")
    void testCreateMembershipType_ZeroMaxBooks() {
        // Given
        MembershipType limitedType = new MembershipType();
        limitedType.setName("Limited");
        limitedType.setMaxBooks(0);
        limitedType.setMaxLoanDays(7);

        when(membershipTypeRepository.save(any(MembershipType.class))).thenReturn(limitedType);

        // When
        MembershipType result = membershipTypeService.createMembershipType(limitedType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMaxBooks()).isEqualTo(0);
        verify(membershipTypeRepository, times(1)).save(any(MembershipType.class));
    }

    @Test
    @DisplayName("Should handle membership type with large max books value")
    void testCreateMembershipType_LargeMaxBooks() {
        // Given
        MembershipType unlimitedType = new MembershipType();
        unlimitedType.setName("Unlimited");
        unlimitedType.setMaxBooks(999);
        unlimitedType.setMaxLoanDays(90);

        when(membershipTypeRepository.save(any(MembershipType.class))).thenReturn(unlimitedType);

        // When
        MembershipType result = membershipTypeService.createMembershipType(unlimitedType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMaxBooks()).isEqualTo(999);
        assertThat(result.getMaxLoanDays()).isEqualTo(90);
        verify(membershipTypeRepository, times(1)).save(any(MembershipType.class));
    }
}
