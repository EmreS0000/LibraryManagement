package com.library.management.service.impl;

import com.library.management.entity.Book;
import com.library.management.entity.Member;
import com.library.management.entity.Reservation;
import com.library.management.entity.ReservationStatus;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.BookRepository;
import com.library.management.repository.MemberRepository;
import com.library.management.repository.ReservationRepository;
import com.library.management.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    @Override
    public Reservation placeReservation(Reservation reservation) {
        Book book = bookRepository.findById(reservation.getBook().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        if (book.getCopies().stream()
                .anyMatch(c -> c.getStatus() == com.library.management.entity.BookCopyStatus.AVAILABLE)) {
            throw new BusinessException("Book is available, no need to reserve. Please borrow it directly.");
        }

        if (reservationRepository.existsByBookIdAndMemberIdAndStatus(book.getId(), reservation.getMember().getId(),
                ReservationStatus.PENDING)) {
            throw new BusinessException("Member already has a pending reservation for this book");
        }

        Member member = memberRepository.findById(reservation.getMember().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        reservation.setBook(book);
        reservation.setMember(member);
        reservation.setStatus(ReservationStatus.PENDING);

        return reservationRepository.save(reservation);
    }

    @Override
    public void cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        reservationRepository.delete(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBookReservedByOther(Long bookId, Long memberId) {
        // If there are pending reservations, check if the first one belongs to this
        // member
        return reservationRepository.findFirstPendingReservation(bookId)
                .map(r -> !r.getMember().getId().equals(memberId))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> getReservationsForBook(Long bookId) {
        return reservationRepository.findByBookIdAndStatusOrderByCreatedAtAsc(bookId, ReservationStatus.PENDING);
    }

    @Override
    public void checkAndNotifyNextReservation(Long bookId) {
        // Logic moved to notification service or implicit in loan return
    }

    @Override
    @Transactional(readOnly = true)
    public int getQueuePosition(Long bookId, Long memberId) {
        List<Reservation> queue = reservationRepository.findByBookIdAndStatusOrderByCreatedAtAsc(bookId,
                ReservationStatus.PENDING);
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getMember().getId().equals(memberId)) {
                return i + 1; // 1-based index
            }
        }
        return 0; // Not in queue or not pending
    }
}
