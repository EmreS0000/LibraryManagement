package com.library.management.service;

import com.library.management.entity.Reservation;
import java.util.List;

public interface ReservationService {
    Reservation placeReservation(Reservation reservation);

    void cancelReservation(Long reservationId);

    boolean isBookReservedByOther(Long bookId, Long memberId);

    List<Reservation> getReservationsForBook(Long bookId);

    void checkAndNotifyNextReservation(Long bookId);

    int getQueuePosition(Long bookId, Long memberId);
}
