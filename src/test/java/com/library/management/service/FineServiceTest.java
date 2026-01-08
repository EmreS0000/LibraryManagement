package com.library.management.service;

import com.library.management.entity.*;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.FineRepository;
import com.library.management.repository.LoanRepository;
import com.library.management.repository.MemberRepository;
import com.library.management.service.impl.FineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FineService Unit Tests")
class FineServiceTest {

    @Mock
    private FineRepository fineRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private FineServiceImpl fineService;

    private Member testMember;
    private Loan testLoan;
    private Fine testFine;
    private BookCopy testBookCopy;

    @BeforeEach
    void setUp() {
        testMember = new Member();
        testMember.setId(1L);
        testMember.setFirstName("John");
        testMember.setLastName("Doe");
        testMember.setEmail("john.doe@example.com");
        testMember.setBalance(BigDecimal.ZERO);

        testBookCopy = new BookCopy();
        testBookCopy.setId(1L);
        testBookCopy.setBarcode("BC001");

        testLoan = new Loan();
        testLoan.setId(1L);
        testLoan.setMember(testMember);
        testLoan.setBookCopy(testBookCopy);
        testLoan.setLoanDate(LocalDateTime.now().minusDays(20));
        testLoan.setDueDate(LocalDateTime.now().minusDays(5));
        testLoan.setStatus(LoanStatus.ACTIVE);

        testFine = new Fine();
        testFine.setId(1L);
        testFine.setLoan(testLoan);
        testFine.setMember(testMember);
        testFine.setAmount(new BigDecimal("5.00"));
        testFine.setStatus(FineStatus.UNPAID);
        testFine.setFineDate(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should calculate overdue fines successfully")
    void testCalculateOverdueFines_Success() {
        // Given
        List<Loan> overdueLoans = Arrays.asList(testLoan);
        when(loanRepository.findByStatus(LoanStatus.ACTIVE)).thenReturn(overdueLoans);
        when(fineRepository.findByLoanId(testLoan.getId())).thenReturn(Optional.empty());
        when(fineRepository.save(any(Fine.class))).thenReturn(testFine);
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // When
        fineService.calculateOverdueFines();

        // Then
        verify(loanRepository, times(1)).findByStatus(LoanStatus.ACTIVE);
        verify(fineRepository, times(1)).save(any(Fine.class));
    }

    @Test
    @DisplayName("Should create new fine for overdue loan")
    void testCreateOrUpdateFine_CreateNew() {
        // Given
        when(fineRepository.findByLoanId(testLoan.getId())).thenReturn(Optional.empty());
        when(fineRepository.save(any(Fine.class))).thenReturn(testFine);
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // When
        fineService.createOrUpdateFine(testLoan);

        // Then
        verify(fineRepository, times(1)).save(any(Fine.class));
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("Should update existing fine for overdue loan")
    void testCreateOrUpdateFine_UpdateExisting() {
        // Given
        when(fineRepository.findByLoanId(testLoan.getId())).thenReturn(Optional.of(testFine));
        when(fineRepository.save(any(Fine.class))).thenReturn(testFine);
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // When
        fineService.createOrUpdateFine(testLoan);

        // Then
        verify(fineRepository, times(1)).save(any(Fine.class));
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("Should not create fine for loan not yet due")
    void testCreateOrUpdateFine_NotOverdue() {
        // Given
        testLoan.setDueDate(LocalDateTime.now().plusDays(5));

        // When
        fineService.createOrUpdateFine(testLoan);

        // Then
        verify(fineRepository, never()).save(any(Fine.class));
    }

    @Test
    @DisplayName("Should pay fine successfully")
    void testPayFine_Success() {
        // Given
        testMember.setBalance(new BigDecimal("5.00"));
        when(fineRepository.findById(1L)).thenReturn(Optional.of(testFine));
        when(fineRepository.save(any(Fine.class))).thenReturn(testFine);
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // When
        fineService.payFine(1L);

        // Then
        assertThat(testFine.getStatus()).isEqualTo(FineStatus.PAID);
        assertThat(testMember.getBalance().compareTo(BigDecimal.ZERO)).isZero();
        verify(fineRepository, times(1)).save(testFine);
        verify(memberRepository, times(1)).save(testMember);
    }

    @Test
    @DisplayName("Should throw exception when fine not found for payment")
    void testPayFine_NotFound() {
        // Given
        when(fineRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> fineService.payFine(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Fine not found");
    }

    @Test
    @DisplayName("Should throw exception when fine already paid")
    void testPayFine_AlreadyPaid() {
        // Given
        testFine.setStatus(FineStatus.PAID);
        when(fineRepository.findById(1L)).thenReturn(Optional.of(testFine));

        // When/Then
        assertThatThrownBy(() -> fineService.payFine(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    @DisplayName("Should get fines by member successfully")
    void testGetFinesByMember_Success() {
        // Given
        Fine fine2 = new Fine();
        fine2.setId(2L);
        fine2.setMember(testMember);
        fine2.setAmount(new BigDecimal("3.00"));
        fine2.setStatus(FineStatus.UNPAID);

        List<Fine> fines = Arrays.asList(testFine, fine2);
        when(fineRepository.findByMemberId(1L)).thenReturn(fines);

        // When
        List<Fine> result = fineService.getFinesByMember(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(testFine, fine2);
        verify(fineRepository, times(1)).findByMemberId(1L);
    }

    @Test
    @DisplayName("Should get all fines successfully")
    void testGetAllFines_Success() {
        // Given
        Fine fine2 = new Fine();
        fine2.setId(2L);

        List<Fine> fines = Arrays.asList(testFine, fine2);
        when(fineRepository.findAll()).thenReturn(fines);

        // When
        List<Fine> result = fineService.getAllFines();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(testFine, fine2);
        verify(fineRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should calculate correct fine amount for multiple overdue days")
    void testCreateOrUpdateFine_MultipleOverdueDays() {
        // Given - 5 days overdue
        testLoan.setDueDate(LocalDateTime.now().minusDays(5));
        when(fineRepository.findByLoanId(testLoan.getId())).thenReturn(Optional.empty());
        when(fineRepository.save(any(Fine.class))).thenAnswer(invocation -> {
            Fine savedFine = invocation.getArgument(0);
            assertThat(savedFine.getAmount()).isGreaterThan(BigDecimal.ZERO);
            return savedFine;
        });
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // When
        fineService.createOrUpdateFine(testLoan);

        // Then
        verify(fineRepository, times(1)).save(any(Fine.class));
    }

    @Test
    @DisplayName("Should handle returned loan with overdue fine")
    void testCreateOrUpdateFine_ReturnedLoan() {
        // Given
        testLoan.setReturnDate(LocalDateTime.now());
        testLoan.setStatus(LoanStatus.RETURNED);
        when(fineRepository.findByLoanId(testLoan.getId())).thenReturn(Optional.empty());
        when(fineRepository.save(any(Fine.class))).thenReturn(testFine);
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // When
        fineService.createOrUpdateFine(testLoan);

        // Then
        verify(fineRepository, times(1)).save(any(Fine.class));
    }

    @Test
    @DisplayName("Should not update paid fine")
    void testCreateOrUpdateFine_PaidFine() {
        // Given
        testFine.setStatus(FineStatus.PAID);
        when(fineRepository.findByLoanId(testLoan.getId())).thenReturn(Optional.of(testFine));

        // When
        fineService.createOrUpdateFine(testLoan);

        // Then
        verify(fineRepository, never()).save(any(Fine.class));
    }
}
