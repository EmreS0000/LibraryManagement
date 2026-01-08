package com.library.management.service.impl;

import com.library.management.entity.*;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.*;
import com.library.management.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final BookCopyRepository bookCopyRepository;
    private final com.library.management.service.FineService fineService;
    private final com.library.management.service.NotificationService notificationService;

    private static final int LOAN_PERIOD_DAYS = 14;

    @Override
    public Loan borrowBook(Loan loan) {
        Member member = memberRepository.findById(loan.getMember().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (member.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Member has outstanding fines. Please pay before borrowing.");
        }

        // Check membership limits
        int maxBooks = 5; // default
        int loanDays = LOAN_PERIOD_DAYS; // default

        if (member.getMembershipType() != null) {
            maxBooks = member.getMembershipType().getMaxBooks();
            loanDays = member.getMembershipType().getMaxLoanDays();
        }

        // Count active loans
        long activeLoanCount = loanRepository.findByMemberId(member.getId()).stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE)
                .count();

        if (activeLoanCount >= maxBooks) {
            throw new BusinessException(
                    "Member has reached maximum borrowing limit based on membership type: " + maxBooks);
        }

        BookCopy copy;
        if (loan.getBookCopy() != null && loan.getBookCopy().getBarcode() != null && !loan.getBookCopy().getBarcode().isEmpty()) {
            copy = bookCopyRepository.findByBarcode(loan.getBookCopy().getBarcode())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Book copy not found with barcode: " + loan.getBookCopy().getBarcode()));
        } else if (loan.getBookCopy() != null && loan.getBookCopy().getId() != null) {
            // If BookCopy ID is provided, fetch it from database
            copy = bookCopyRepository.findById(loan.getBookCopy().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Book copy not found with ID: " + loan.getBookCopy().getId()));
        } else {
            // Find valid copy: AVAILABLE or RESERVED (if for this user)
            // Implementation choice: Search for RESERVED for this user first, then
            // AVAILABLE
            // But simplify: just find first check if usable
            Long bookId = loan.getBookCopy() != null && loan.getBookCopy().getBook() != null ? 
                    loan.getBookCopy().getBook().getId() : null;
            if (bookId == null) {
                throw new BusinessException("Book ID or BookCopy ID is required");
            }
            copy = bookCopyRepository.findByBookId(bookId).stream()
                    .filter(c -> c.getStatus() == BookCopyStatus.AVAILABLE ||
                            (c.getStatus() == BookCopyStatus.RESERVED &&
                                    reservationRepository.existsByBookIdAndMemberIdAndStatus(c.getBook().getId(),
                                            member.getId(), ReservationStatus.READY_FOR_PICKUP)))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("No available copies for this book"));
        }

        if (copy.getStatus() == BookCopyStatus.RESERVED) {
            // Check if reserved for THIS user and is READY
            boolean isReservedForUser = reservationRepository.existsByBookIdAndMemberIdAndStatus(
                    copy.getBook().getId(), member.getId(), ReservationStatus.READY_FOR_PICKUP);

            if (!isReservedForUser) {
                throw new BusinessException("Book copy is reserved for another member");
            }

            Reservation readyRes = reservationRepository
                    .findFirstByBookIdAndMemberIdAndStatusOrderByCreatedAtAsc(copy.getBook().getId(),
                            member.getId(), ReservationStatus.READY_FOR_PICKUP)
                    .orElseThrow(() -> new BusinessException("Ready reservation not found for this member"));
            readyRes.setStatus(ReservationStatus.FULFILLED);
            reservationRepository.save(readyRes);
        } else if (copy.getStatus() != BookCopyStatus.AVAILABLE) {
            throw new BusinessException("Book copy is not available");
        }

        // Check if there are other pending reservations (and this wasn't a reserved
        // copy for this user)
        // If this copy was AVAILABLE, but there are pending reservations, strict queue
        // enforcement means
        // we shouldn't allow jumping the queue unless we are the first in queue.
        // However, if the system auto-reserves on return, AVAILABLE copies imply no
        // queue or leftovers.
        // But what if a copy was just added?
        // Strictly: If queue exists, AVAILABLE copy should be RESERVED for first in
        // queue.
        // For MVP: If AVAILABLE, allow borrow, UNLESS there is a queue (Queue check).

        if (copy.getStatus() == BookCopyStatus.AVAILABLE) {
            Optional<Reservation> pendingRes = reservationRepository
                    .findFirstPendingReservation(copy.getBook().getId());
            if (pendingRes.isPresent()) {
                // There is a queue. Is this user the first one?
                if (!pendingRes.get().getMember().getId().equals(member.getId())) {
                    throw new BusinessException("There is a reservation queue for this book. Please join the queue.");
                } else {
                    // Fulfill pending reservation (became direct borrow without waiting for return)
                    Reservation res = pendingRes.get();
                    res.setStatus(ReservationStatus.FULFILLED);
                    reservationRepository.save(res);
                }
            }
        }

        copy.setStatus(BookCopyStatus.LOANED);
        bookCopyRepository.save(copy);

        Loan newLoan = new Loan();
        newLoan.setBookCopy(copy);
        newLoan.setMember(member);
        newLoan.setLoanDate(LocalDateTime.now());
        newLoan.setDueDate(LocalDateTime.now().plusDays(loanDays));
        newLoan.setStatus(LoanStatus.ACTIVE);

        return loanRepository.save(newLoan);
    }

    @Override
    public Loan returnBook(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException("Book already returned");
        }

        BookCopy copy = loan.getBookCopy();

        // Check for pending reservation
        Optional<Reservation> pendingRes = reservationRepository.findFirstPendingReservation(copy.getBook().getId());
        if (pendingRes.isPresent()) {
            copy.setStatus(BookCopyStatus.RESERVED);
            Reservation reservation = pendingRes.get();
            reservation.setStatus(ReservationStatus.READY_FOR_PICKUP);
            reservation.setExpiryDate(LocalDateTime.now().plusDays(3)); // 3 days to pickup
            reservationRepository.save(reservation);
            notificationService.notifyReservationReady(reservation.getMember().getEmail(), copy.getBook().getTitle());
        } else {
            copy.setStatus(BookCopyStatus.AVAILABLE);
        }

        bookCopyRepository.save(copy);

        loan.setStatus(LoanStatus.RETURNED);
        LocalDateTime now = LocalDateTime.now();
        loan.setReturnDate(now);

        Loan savedLoan = loanRepository.save(loan);

        // Calculate/Finalize Fine
        fineService.createOrUpdateFine(savedLoan);

        return savedLoan;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Loan> getLoansByMember(Long memberId) {
        return loanRepository.findByMemberId(memberId);
    }

}
