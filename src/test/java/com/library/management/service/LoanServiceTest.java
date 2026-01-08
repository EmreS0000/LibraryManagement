package com.library.management.service;

import com.library.management.entity.*;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.*;
import com.library.management.service.impl.LoanServiceImpl;
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
@DisplayName("LoanService Unit Tests")
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private FineService fineService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LoanServiceImpl loanService;

    private Member testMember;
    private Book testBook;
    private BookCopy testBookCopy;
    private Loan testLoan;
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
        testMember.setBalance(BigDecimal.ZERO);
        testMember.setMembershipType(testMembershipType);

        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("Test Book");
        testBook.setIsbn("978-1234567890");

        testBookCopy = new BookCopy();
        testBookCopy.setId(1L);
        testBookCopy.setBook(testBook);
        testBookCopy.setBarcode("BC001");
        testBookCopy.setStatus(BookCopyStatus.AVAILABLE);

        testLoan = new Loan();
        testLoan.setId(1L);
        testLoan.setMember(testMember);
        testLoan.setBookCopy(testBookCopy);
        testLoan.setLoanDate(LocalDateTime.now());
        testLoan.setDueDate(LocalDateTime.now().plusDays(14));
        testLoan.setStatus(LoanStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should borrow book successfully with barcode")
    void testBorrowBook_Success_WithBarcode() {
        // Given
        Loan newLoan = new Loan();
        BookCopy copy = new BookCopy();
        copy.setBarcode("BC001");
        newLoan.setBookCopy(copy);
        newLoan.setMember(testMember);

        when(memberRepository.findById(testMember.getId())).thenReturn(Optional.of(testMember));
        when(bookCopyRepository.findByBarcode("BC001")).thenReturn(Optional.of(testBookCopy));
        when(loanRepository.findByMemberId(testMember.getId())).thenReturn(Arrays.asList());
        when(reservationRepository.findFirstPendingReservation(anyLong())).thenReturn(Optional.empty());
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

        // When
        Loan result = loanService.borrowBook(newLoan);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        verify(bookCopyRepository, times(1)).save(any(BookCopy.class));
        verify(loanRepository, times(1)).save(any(Loan.class));
    }

    @Test
    @DisplayName("Should throw exception when member not found")
    void testBorrowBook_MemberNotFound() {
        // Given
        Loan newLoan = new Loan();
        Member member = new Member();
        member.setId(99L);
        newLoan.setMember(member);

        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> loanService.borrowBook(newLoan))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Member not found");
    }

    @Test
    @DisplayName("Should throw exception when member has outstanding fines")
    void testBorrowBook_OutstandingFines() {
        // Given
        testMember.setBalance(new BigDecimal("10.00"));
        Loan newLoan = new Loan();
        newLoan.setMember(testMember);

        when(memberRepository.findById(testMember.getId())).thenReturn(Optional.of(testMember));

        // When/Then
        assertThatThrownBy(() -> loanService.borrowBook(newLoan))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("outstanding fines");
    }

    @Test
    @DisplayName("Should throw exception when member reached borrowing limit")
    void testBorrowBook_BorrowingLimitReached() {
        // Given
        Loan newLoan = new Loan();
        BookCopy copy = new BookCopy();
        copy.setBarcode("BC001");
        newLoan.setBookCopy(copy);
        newLoan.setMember(testMember);

        List<Loan> activeLoans = Arrays.asList(
                createActiveLoan(), createActiveLoan(), createActiveLoan(),
                createActiveLoan(), createActiveLoan()
        );

        when(memberRepository.findById(testMember.getId())).thenReturn(Optional.of(testMember));
        when(loanRepository.findByMemberId(testMember.getId())).thenReturn(activeLoans);

        // When/Then
        assertThatThrownBy(() -> loanService.borrowBook(newLoan))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("maximum borrowing limit");
    }

    @Test
    @DisplayName("Should throw exception when book copy not found")
    void testBorrowBook_BookCopyNotFound() {
        // Given
        Loan newLoan = new Loan();
        BookCopy copy = new BookCopy();
        copy.setBarcode("INVALID");
        newLoan.setBookCopy(copy);
        newLoan.setMember(testMember);

        when(memberRepository.findById(testMember.getId())).thenReturn(Optional.of(testMember));
        when(loanRepository.findByMemberId(testMember.getId())).thenReturn(Arrays.asList());
        when(bookCopyRepository.findByBarcode("INVALID")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> loanService.borrowBook(newLoan))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book copy not found");
    }

    @Test
    @DisplayName("Should return book successfully")
    void testReturnBook_Success() {
        // Given
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(reservationRepository.findFirstPendingReservation(anyLong())).thenReturn(Optional.empty());
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

        // When
        Loan result = loanService.returnBook(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getReturnDate()).isNotNull();
        verify(bookCopyRepository, times(1)).save(any(BookCopy.class));
        verify(loanRepository, times(1)).save(any(Loan.class));
    }

    @Test
    @DisplayName("Should throw exception when loan not found for return")
    void testReturnBook_LoanNotFound() {
        // Given
        when(loanRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> loanService.returnBook(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Loan not found");
    }

    @Test
    @DisplayName("Should throw exception when book already returned")
    void testReturnBook_AlreadyReturned() {
        // Given
        testLoan.setStatus(LoanStatus.RETURNED);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

        // When/Then
        assertThatThrownBy(() -> loanService.returnBook(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already returned");
    }

    @Test
    @DisplayName("Should get all loans successfully")
    void testGetAllLoans_Success() {
        // Given
        Loan loan2 = new Loan();
        loan2.setId(2L);
        loan2.setStatus(LoanStatus.ACTIVE);

        List<Loan> loans = Arrays.asList(testLoan, loan2);
        when(loanRepository.findAll()).thenReturn(loans);

        // When
        List<Loan> result = loanService.getAllLoans();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(testLoan, loan2);
        verify(loanRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should get loans by member successfully")
    void testGetLoansByMember_Success() {
        // Given
        List<Loan> memberLoans = Arrays.asList(testLoan);
        when(loanRepository.findByMemberId(1L)).thenReturn(memberLoans);

        // When
        List<Loan> result = loanService.getLoansByMember(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMember().getId()).isEqualTo(1L);
        verify(loanRepository, times(1)).findByMemberId(1L);
    }

    private Loan createActiveLoan() {
        Loan loan = new Loan();
        loan.setStatus(LoanStatus.ACTIVE);
        return loan;
    }
}
