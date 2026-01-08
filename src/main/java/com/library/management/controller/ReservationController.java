package com.library.management.controller;

import com.library.management.entity.Reservation;
import com.library.management.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Reservation Management APIs")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Place a reservation for a book")
    public ResponseEntity<Reservation> placeReservation(@RequestBody Reservation reservation) {
        if (reservation.getMember() == null || reservation.getMember().getId() == null ||
            reservation.getBook() == null || reservation.getBook().getId() == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(reservationService.placeReservation(reservation), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a reservation")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/book/{bookId}")
    @Operation(summary = "Get all reservations for a book")
    public ResponseEntity<List<Reservation>> getReservationsForBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(reservationService.getReservationsForBook(bookId));
    }

    @GetMapping("/queue-position/{bookId}/{memberId}")
    @Operation(summary = "Get queue position for a member")
    public ResponseEntity<Integer> getQueuePosition(@PathVariable Long bookId, @PathVariable Long memberId) {
        return ResponseEntity.ok(reservationService.getQueuePosition(bookId, memberId));
    }
}
