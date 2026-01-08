package com.library.management.repository;

import com.library.management.entity.Reservation;
import com.library.management.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByBookIdAndStatusOrderByCreatedAtAsc(Long bookId, ReservationStatus status);

    boolean existsByBookIdAndMemberIdAndStatus(Long bookId, Long memberId, ReservationStatus status);

    @Query("SELECT r FROM Reservation r WHERE r.book.id = :bookId AND r.status = 'PENDING' ORDER BY r.createdAt ASC LIMIT 1")
    Optional<Reservation> findFirstPendingReservation(@Param("bookId") Long bookId);

        Optional<Reservation> findFirstByBookIdAndMemberIdAndStatusOrderByCreatedAtAsc(Long bookId, Long memberId,
            ReservationStatus status);
}
